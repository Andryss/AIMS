#!/usr/bin/env python3
"""Import PlantUML class diagrams into a Visual Paradigm project.

Pipeline:
  1. extract ```plantuml blocks from a Markdown file and parse classes, packages, members, relations;
  2. render each block to SVG with PlantUML and read shape bounds / link paths (layout);
  3. write a tab-separated spec per diagram;
  4. build the VP plugin (vp-plugin/) and run it headless via VP's command-line Plugin runner,
     which creates the model elements and diagrams in the .vpp and saves it.

Visual Paradigm must be closed while the .vpp is being modified.
"""

import argparse
import datetime
import os
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parent
PLUGIN_SRC = TOOLS_DIR / "vp-plugin"
PLUGIN_ID = "aims.puml2vp"

VP_APP = Path("/Applications/Visual Paradigm.app/Contents/Resources")
VP_JAVA = VP_APP / "jre.bundle/Contents/Home/bin/java"
VP_LIB = VP_APP / "app/lib"
VP_ORMLIB = VP_APP / "app/ormlib"
VP_BIN = VP_APP / "app/bin"
VP_PLUGINS = Path.home() / "Library/Application Support/VisualParadigm/plugins"

SVG_NS = "{http://www.w3.org/2000/svg}"

VISIBILITY = {"+": "public", "-": "private", "#": "protected", "~": "package"}

DECL_RE = re.compile(
    r'^(?P<kind>abstract\s+class|class|interface|enum)\s+'
    r'(?:"(?P<label>[^"]+)"|(?P<name>[\w.]+))'
    r'(?:\s+as\s+(?P<alias>\w+))?'
    r'(?:\s*<<(?P<stereo>.+?)>>)?'
    r'\s*(?P<body>\{)?\s*$'
)
PACKAGE_RE = re.compile(r'^package\s+(?:"(?P<label>[^"]+)"|(?P<name>[\w.]+))(?:\s+as\s+(?P<alias>\w+))?\s*\{\s*$')
REL_RE = re.compile(r'^(?P<a>\w+)\s+(?P<op>[-.<>|o*\w\[\]]+)\s+(?P<b>\w+)\s*(?::\s*(?P<label>.*))?$')
TITLE_RE = re.compile(r'^title\s+(?P<title>.+)$')


# --------------------------------------------------------------------------- parsing

def extract_blocks(markdown: str) -> list[str]:
    return re.findall(r"```plantuml\n(.*?)```", markdown, re.S)


def join_continuations(text: str) -> str:
    # PlantUML member line continuation used in the docs: "...,%n()\<newline>    \t..."
    return re.sub(r"%n\(\)\\\n\s*(?:\\t)?", " ", text)


def split_params(text: str) -> list[str]:
    parts, depth, cur = [], 0, ""
    for ch in text:
        if ch in "<([":
            depth += 1
        elif ch in ">)]":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += ch
    if cur.strip():
        parts.append(cur)
    return [p.strip() for p in parts]


def parse_member(line: str) -> dict:
    vis = "Unspecified"
    if line[0] in VISIBILITY:
        vis, line = VISIBILITY[line[0]], line[1:].strip()
    if "(" in line:
        name, rest = line.split("(", 1)
        params, _, ret = rest.rpartition(")")
        ret = ret.strip().lstrip(":").strip()
        plist = []
        for p in split_params(params):
            pname, _, ptype = p.partition(":")
            plist.append((pname.strip(), ptype.strip()))
        return {"member": "op", "vis": vis, "name": name.strip(), "params": plist, "ret": ret}
    name, _, typ = line.partition(":")
    return {"member": "attr", "vis": vis, "name": name.strip(), "type": typ.strip()}


def classify_relation(op: str):
    """Returns (kind, swap) where swap means the model 'from' is the right-hand side."""
    if "hidden" in op:
        return None, False
    body = re.sub(r"\[[^\]]*\]", "", op)  # drop [dotted], [#color] ...
    body = re.sub(r"(up|down|left|right|u|d|l|r)(?=[-.])", "", body)
    dotted = "." in body or "[dotted]" in op
    if body.endswith("|>"):
        return ("realization" if dotted else "generalization"), False
    if body.startswith("<|"):
        return ("realization" if dotted else "generalization"), True
    if body.startswith("o"):
        return "aggregation", False
    if body.endswith("o"):
        return "aggregation", True
    if body.startswith("*"):
        return "composition", False
    if body.endswith("*"):
        return "composition", True
    if body.endswith(">"):
        return ("dependency" if dotted else "directed"), False
    if body.startswith("<"):
        return ("dependency" if dotted else "directed"), True
    return ("dependency" if dotted else "assoc"), False


def parse_block(block: str) -> dict:
    lines = [l.strip() for l in join_continuations(block).splitlines()]
    diagram = {"id": None, "title": None, "packages": [], "classes": {}, "relations": []}
    stack = []  # entries: ("package", key) | ("class", key)
    for line in lines:
        if not line or line.startswith("'") or line.startswith("!") or line.startswith("skinparam"):
            continue
        if line.startswith("@startuml"):
            diagram["id"] = line.split(maxsplit=1)[1] if " " in line else "diagram"
            continue
        if line.startswith("@enduml") or line.endswith("direction"):
            continue
        if m := TITLE_RE.match(line):
            diagram["title"] = m["title"].strip()
            continue
        if line == "}":
            stack.pop()
            continue
        if stack and stack[-1][0] == "class":
            diagram["classes"][stack[-1][1]]["members"].append(parse_member(line))
            continue
        parent = next((k for t, k in reversed(stack) if t == "package"), None)
        if m := PACKAGE_RE.match(line):
            key = f"pkg{len(diagram['packages'])}"
            diagram["packages"].append({"key": key, "name": m["label"] or m["name"], "parent": parent})
            stack.append(("package", key))
            continue
        if m := DECL_RE.match(line):
            name = m["label"] or m["name"]
            alias = m["alias"] or m["name"] or name
            kind = "class" if "class" in m["kind"] else m["kind"]
            stereos = [s.strip() for s in (m["stereo"] or "").split(",") if s.strip()]
            diagram["classes"][alias] = {"key": alias, "name": name, "kind": kind, "stereotypes": stereos,
                                         "package": parent, "members": []}
            if m["body"]:
                stack.append(("class", alias))
            continue
        if m := REL_RE.match(line):
            kind, swap = classify_relation(m["op"])
            if kind is None:
                continue
            a, b = (m["b"], m["a"]) if swap else (m["a"], m["b"])
            diagram["relations"].append({"kind": kind, "from": a, "to": b, "label": (m["label"] or "").strip()})
            continue
        raise ValueError(f"Unsupported PlantUML line in {diagram['id']}: {line!r}")
    return diagram


# --------------------------------------------------------------------------- layout

def render_svgs(markdown_path: Path, plantuml_jar: Path, out_dir: Path) -> None:
    subprocess.run(["java", "-Djava.awt.headless=true", "-jar", str(plantuml_jar), "-tsvg", "-charset", "UTF-8",
                    "-o", str(out_dir), str(markdown_path)], check=True)


def numbers(text: str) -> list[float]:
    return [float(n) for n in re.findall(r"-?\d+(?:\.\d+)?", text)]


def read_layout(svg_path: Path) -> dict:
    root = ET.parse(svg_path).getroot()
    entities, clusters, links = {}, [], []
    for g in root.iter(f"{SVG_NS}g"):
        cls = g.get("class")
        if cls == "entity":
            rect = g.find(f"{SVG_NS}rect")
            alias = g.get("data-qualified-name").split(".")[-1]
            entities[g.get("id")] = {"alias": alias, "bounds": (float(rect.get("x")), float(rect.get("y")),
                                                                 float(rect.get("width")), float(rect.get("height")))}
        elif cls == "cluster":
            pts = path_vertices(g.find(f"{SVG_NS}path").get("d"))
            xs, ys = [p[0] for p in pts], [p[1] for p in pts]
            clusters.append((min(xs), min(ys), max(xs) - min(xs), max(ys) - min(ys)))
        elif cls == "link":
            path = g.find(f"{SVG_NS}path")
            links.append({"e1": g.get("data-entity-1"), "e2": g.get("data-entity-2"),
                          "points": path_waypoints(path.get("d"))})
    by_alias = {e["alias"]: e["bounds"] for e in entities.values()}
    alias_of = {eid: e["alias"] for eid, e in entities.items()}
    for link in links:
        link["a1"], link["a2"] = alias_of.get(link["e1"]), alias_of.get(link["e2"])
    return {"classes": by_alias, "clusters": clusters, "links": links}


def path_vertices(d: str) -> list[tuple[float, float]]:
    """End points of absolute M/L/A/C commands (arc radii and flags are skipped)."""
    points = []
    for cmd, args in re.findall(r"([MLAC])([^MLACZ]*)", d):
        nums = numbers(args)
        step = {"M": 2, "L": 2, "A": 7, "C": 6}[cmd]
        for i in range(0, len(nums) - step + 1, step):
            points.append((nums[i + step - 2], nums[i + step - 1]))
    return points


def path_waypoints(d: str) -> list[tuple[float, float]]:
    """Start point plus the end point of every cubic segment (drops Bezier control points)."""
    points = []
    for cmd, args in re.findall(r"([MC])([^MC]*)", d):
        nums = numbers(args)
        pairs = list(zip(nums[0::2], nums[1::2]))
        points.extend(pairs if cmd == "M" else pairs[2::3])
    return points


# --------------------------------------------------------------------------- spec

def clean(text: str) -> str:
    return text.replace("\t", " ").replace("\n", " ")


def fmt_bounds(b) -> str:
    return "\t".join(str(round(v)) for v in b)


def build_spec(diagram: dict, layout: dict) -> str:
    title = diagram["title"] or diagram["id"]
    out = [f"DIAGRAM\t{clean(title)}\t{clean(title)}"]

    if len(layout["clusters"]) != len(diagram["packages"]):
        raise ValueError(f"{diagram['id']}: {len(diagram['packages'])} packages but "
                         f"{len(layout['clusters'])} clusters in SVG")
    for pkg, bounds in zip(diagram["packages"], layout["clusters"]):
        out.append(f"PACKAGE\t{pkg['key']}\t{clean(pkg['name'])}\t{pkg['parent'] or '-'}\t{fmt_bounds(bounds)}")

    for c in diagram["classes"].values():
        bounds = layout["classes"].get(c["key"])
        if bounds is None:
            raise ValueError(f"{diagram['id']}: class {c['key']} not found in SVG")
        out.append(f"CLASS\t{c['key']}\t{clean(c['name'])}\t{c['kind']}\t{','.join(c['stereotypes'])}\t"
                   f"{c['package'] or '-'}\t{fmt_bounds(bounds)}")
        for m in c["members"]:
            if m["member"] == "attr":
                out.append(f"ATTR\t{c['key']}\t{m['vis']}\t{clean(m['name'])}\t{clean(m['type'])}")
            else:
                params = ";".join(f"{n}:{t}" for n, t in m["params"])
                out.append(f"OP\t{c['key']}\t{m['vis']}\t{clean(m['name'])}\t{clean(params)}\t{clean(m['ret'])}")

    unused = list(layout["links"])
    for r in diagram["relations"]:
        link = next((l for l in unused if {l["a1"], l["a2"]} == {r["from"], r["to"]}), None)
        points = []
        if link is not None:
            unused.remove(link)
            points = link["points"] if link["a1"] == r["from"] else list(reversed(link["points"]))
        pts = ";".join(f"{round(x)},{round(y)}" for x, y in points)
        out.append(f"REL\t{r['kind']}\t{r['from']}\t{r['to']}\t{clean(r['label'])}\t{pts}")
    return "\n".join(out) + "\n"


# --------------------------------------------------------------------------- VP

def vp_classpath() -> str:
    jars = sorted(VP_LIB.glob("*.jar")) + sorted(VP_ORMLIB.glob("*.jar"))
    return ":".join(["."] + [str(j) for j in jars])


def install_plugin(javac: str) -> None:
    target = VP_PLUGINS / PLUGIN_ID
    if target.exists():
        shutil.rmtree(target)
    (target / "classes").mkdir(parents=True)
    sources = [str(p) for p in (PLUGIN_SRC / "src").rglob("*.java")]
    subprocess.run([javac, "--release", "11", "-encoding", "UTF-8", "-cp", str(VP_LIB / "openapi.jar"),
                    "-d", str(target / "classes"), *sources], check=True)
    shutil.copy(PLUGIN_SRC / "plugin.xml", target / "plugin.xml")


def vp_running() -> bool:
    return subprocess.run(["pgrep", "-f", "Visual Paradigm.app"], capture_output=True).returncode == 0


def run_plugin(vpp: Path, specs: list[Path]) -> None:
    args = " ".join(f"'{s}'" for s in specs)
    cmd = [str(VP_JAVA), "-Xmx2g", "-Djava.awt.headless=true", "-cp", vp_classpath(), "com.vp.cmd.Plugin",
           "-project", str(vpp), "-pluginid", PLUGIN_ID, "-pluginargs", args]
    proc = subprocess.run(cmd, cwd=VP_BIN, capture_output=True, text=True)
    output = proc.stdout + proc.stderr
    print("\n".join(l for l in output.splitlines() if "puml2vp" in l or "Plugin" in l or "Exception" in l))
    if proc.returncode != 0 or "[puml2vp] FAILED" in output or "project saved: true" not in output:
        print(output, file=sys.stderr)
        raise SystemExit("VP plugin run failed")


def export_images(vpp: Path, out_dir: Path) -> None:
    cmd = [str(VP_JAVA), "-Djava.awt.headless=true", "-cp", vp_classpath(), "com.vp.cmd.ExportDiagramImage",
           "-project", str(vpp), "-out", str(out_dir), "-diagram", "*", "-type", "png_with_background"]
    subprocess.run(cmd, cwd=VP_BIN, capture_output=True, check=True)
    print(f"diagram images: {out_dir}")


def find_javac() -> str:
    try:
        home = subprocess.run(["/usr/libexec/java_home", "-v", "11+"], capture_output=True, text=True,
                              check=True).stdout.strip()
        return str(Path(home) / "bin/javac")
    except (subprocess.CalledProcessError, FileNotFoundError):
        return "javac"


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("markdown", type=Path, help="Markdown file with ```plantuml class diagrams")
    ap.add_argument("--vpp", type=Path, required=True, help="Visual Paradigm project to update")
    ap.add_argument("--plantuml", type=Path, default=Path("/tmp/plantuml.jar"))
    ap.add_argument("--work-dir", type=Path, default=None, help="where SVGs and specs are written")
    ap.add_argument("--dry-run", action="store_true", help="only parse and write specs")
    ap.add_argument("--export-images", type=Path, default=None, help="export all VP diagrams as PNG afterwards")
    args = ap.parse_args()

    work = args.work_dir or Path(tempfile.mkdtemp(prefix="puml2vp-"))
    work.mkdir(parents=True, exist_ok=True)
    diagrams = [parse_block(b) for b in extract_blocks(args.markdown.read_text(encoding="utf-8"))]
    render_svgs(args.markdown, args.plantuml, work)

    specs = []
    for d in diagrams:
        layout = read_layout(work / f"{d['id']}.svg")
        spec = work / f"{d['id']}.tsv"
        spec.write_text(build_spec(d, layout), encoding="utf-8")
        specs.append(spec)
        print(f"{d['id']}: {len(d['packages'])} packages, {len(d['classes'])} classifiers, "
              f"{len(d['relations'])} relations -> {spec}")

    if args.dry_run:
        return
    if vp_running():
        raise SystemExit("Close Visual Paradigm before importing (the project file is locked while open).")

    backup = args.vpp.with_name(f"{args.vpp.name}.before-import-{datetime.datetime.now():%Y%m%d-%H%M%S}")
    shutil.copy2(args.vpp, backup)
    print(f"backup: {backup}")

    install_plugin(find_javac())
    run_plugin(args.vpp.resolve(), specs)
    if args.export_images:
        export_images(args.vpp.resolve(), args.export_images.resolve())


if __name__ == "__main__":
    main()
