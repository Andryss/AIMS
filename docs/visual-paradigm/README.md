# Visual Paradigm model

`AIMS.vpp` is the Visual Paradigm (18.1) project of the system. Class diagrams written in PlantUML
(`docs/classes/*.md`) are imported into it by `tools/puml2vp.py`, so the VP diagrams have the same
elements, members, relations and layout as the PlantUML render.

## How it works

1. `puml2vp.py` parses the ```` ```plantuml ```` blocks (classes, interfaces, enums, packages,
   stereotypes, attributes, operations, relations; `-[hidden]` links are skipped).
2. PlantUML renders each block to SVG; shape bounds and link paths are taken from it as layout.
3. A tab-separated spec per diagram is written (format documented in `Puml2VpPlugin.java`).
4. The VP plugin in `tools/vp-plugin` is compiled, installed to
   `~/Library/Application Support/VisualParadigm/plugins/aims.puml2vp` and run headless through VP's
   command-line plugin runner; it creates the model and diagrams and saves the project.

Each diagram is stored in its own model package named after the PlantUML `title`. Re-running the
import replaces the diagrams and packages with the same names.

## Import

Close Visual Paradigm first (the project is locked while open). Requires a JDK 11+ for `javac`.

```bash
curl -L https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar -o /tmp/plantuml.jar

python3 docs/visual-paradigm/tools/puml2vp.py docs/classes/UC1.md \
  --vpp docs/visual-paradigm/AIMS.vpp \
  --export-images /tmp/aims-vp-render
```

A backup `AIMS.vpp.before-import-<timestamp>` is created next to the project before every import.
`--dry-run` only parses and writes the specs (see `--work-dir`).

## Differences from PlantUML

- Directed associations (`-->`) have an explicitly non-navigable source end, which VP draws with `×`.
- Shapes use VP's default style (fill color, fonts); PlantUML title is used as the diagram name.
