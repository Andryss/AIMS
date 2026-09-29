package aims.puml2vp;

import com.vp.plugin.ApplicationManager;
import com.vp.plugin.DiagramManager;
import com.vp.plugin.VPPlugin;
import com.vp.plugin.VPPluginCommandLineSupport;
import com.vp.plugin.VPPluginInfo;
import com.vp.plugin.diagram.IClassDiagramUIModel;
import com.vp.plugin.diagram.IConnectorUIModel;
import com.vp.plugin.diagram.IDiagramElement;
import com.vp.plugin.diagram.IDiagramUIModel;
import com.vp.plugin.diagram.IShapeUIModel;
import com.vp.plugin.diagram.connector.IAssociationUIModel;
import com.vp.plugin.diagram.shape.IClassUIModel;
import com.vp.plugin.model.IAssociation;
import com.vp.plugin.model.IAssociationEnd;
import com.vp.plugin.model.IAttribute;
import com.vp.plugin.model.IClass;
import com.vp.plugin.model.IDependency;
import com.vp.plugin.model.IModelElement;
import com.vp.plugin.model.IOperation;
import com.vp.plugin.model.IPackage;
import com.vp.plugin.model.IParameter;
import com.vp.plugin.model.IProject;
import com.vp.plugin.model.IRealization;
import com.vp.plugin.model.factory.IModelElementFactory;

import java.awt.Point;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds class diagrams in the opened project from a tab-separated spec produced by puml2vp.py.
 *
 * <pre>
 * DIAGRAM  name  modelPackageName
 * PACKAGE  key  name  parentKey|-  x  y  w  h
 * CLASS    key  name  class|interface|enum  stereotypes(comma)  packageKey|-  x  y  w  h
 * ATTR     classKey  visibility  name  type
 * OP       classKey  visibility  name  params(name:type;...)  returnType
 * REL      assoc|directed|dependency|realization|aggregation|composition  fromKey  toKey  label  x,y;x,y;...
 * </pre>
 */
public class Puml2VpPlugin implements VPPlugin, VPPluginCommandLineSupport {

    @Override
    public void loaded(VPPluginInfo info) {
    }

    @Override
    public void unloaded() {
    }

    @Override
    public void invoke(String[] args) {
        try {
            for (String specPath : args) {
                build(new File(specPath));
            }
            boolean saved = ApplicationManager.instance().getProjectManager().saveProject();
            System.out.println("[puml2vp] project saved: " + saved);
        } catch (Throwable t) {
            System.out.println("[puml2vp] FAILED: " + t);
            t.printStackTrace(System.out);
        }
    }

    private void build(File spec) throws Exception {
        IProject project = ApplicationManager.instance().getProjectManager().getProject();
        DiagramManager dm = ApplicationManager.instance().getDiagramManager();
        IModelElementFactory factory = IModelElementFactory.instance();

        IClassDiagramUIModel diagram = null;
        IPackage root = null;
        Map<String, IModelElement> models = new HashMap<>();
        Map<String, IShapeUIModel> shapes = new HashMap<>();
        Map<IShapeUIModel, int[]> bounds = new java.util.LinkedHashMap<>();
        List<IShapeUIModel> packageShapes = new ArrayList<>();

        boolean layoutFixed = false;
        for (String line : Files.readAllLines(spec.toPath(), StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t", -1);
            switch (f[0]) {
                case "DIAGRAM": {
                    removeExisting(project, f[1], f[2]);
                    root = factory.createPackage();
                    root.setName(f[2]);
                    diagram = (IClassDiagramUIModel) dm.createDiagram(DiagramManager.DIAGRAM_TYPE_CLASS_DIAGRAM);
                    diagram.setName(f[1]);
                    diagram.setConnectorStyle(IConnectorUIModel.CS_OBLIQUE);
                    diagram.setAutoFitShapesSize(false);
                    diagram.setShowAssociationNavigationArrows(
                            IAssociationUIModel.SHOW_NAVIGATION_ARROWS_HIDE_ARROWS_WITH_TWO_WAY_NAVIGABILITY);
                    System.out.println("[puml2vp] diagram: " + f[1]);
                    break;
                }
                case "PACKAGE": {
                    IPackage pkg = factory.createPackage();
                    pkg.setName(f[2]);
                    parentModel(models, f[3], root).addChild(pkg);
                    models.put(f[1], pkg);
                    IShapeUIModel shape = (IShapeUIModel) dm.createDiagramElement(diagram, pkg);
                    bounds.put(shape, place(shape, f, 4));
                    packageShapes.add(shape);
                    addToParentShape(shapes, f[3], shape);
                    shapes.put(f[1], shape);
                    break;
                }
                case "CLASS": {
                    IClass cls = factory.createClass();
                    cls.setName(f[2]);
                    if ("interface".equals(f[3])) {
                        cls.addStereotype("Interface");
                    } else if ("enum".equals(f[3])) {
                        cls.addStereotype("enumeration");
                    }
                    for (String st : f[4].split(",")) {
                        if (!st.isBlank()) {
                            cls.addStereotype(st.trim());
                        }
                    }
                    parentModel(models, f[5], root).addChild(cls);
                    models.put(f[1], cls);
                    IShapeUIModel shape = (IShapeUIModel) dm.createDiagramElement(diagram, cls);
                    // PlantUML draws «entity»/«control»/«boundary» as plain class boxes, not robustness icons
                    ((IClassUIModel) shape).setDisplayAsRobustnessAnalysisIcon(false);
                    shape.setPresentationOption(IShapeUIModel.PRESENTATION_OPTION_STANDARD);
                    bounds.put(shape, place(shape, f, 6));
                    addToParentShape(shapes, f[5], shape);
                    shapes.put(f[1], shape);
                    break;
                }
                case "ATTR": {
                    IClass cls = (IClass) models.get(f[1]);
                    IAttribute attr = factory.createAttribute();
                    attr.setName(f[3]);
                    attr.setVisibility(f[2]);
                    if (!f[4].isEmpty()) {
                        attr.setType(f[4]);
                    }
                    cls.addAttribute(attr);
                    break;
                }
                case "OP": {
                    IClass cls = (IClass) models.get(f[1]);
                    IOperation op = factory.createOperation();
                    op.setName(f[3]);
                    op.setVisibility(f[2]);
                    for (String p : f[4].split(";")) {
                        if (p.isBlank()) {
                            continue;
                        }
                        String[] nt = p.split(":", 2);
                        IParameter param = factory.createParameter();
                        param.setName(nt[0].trim());
                        if (nt.length > 1) {
                            param.setType(nt[1].trim());
                        }
                        op.addParameter(param);
                    }
                    if (!f[5].isEmpty()) {
                        op.setReturnType(f[5]);
                    }
                    cls.addOperation(op);
                    break;
                }
                case "REL": {
                    if (!layoutFixed) {
                        fixLayout(bounds, packageShapes);
                        layoutFixed = true;
                    }
                    relation(dm, factory, diagram, models, shapes, f);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown spec line: " + line);
            }
        }
        if (!layoutFixed) {
            fixLayout(bounds, packageShapes);
        }
    }

    /** Nesting shapes into packages moves/resizes them; restore PlantUML bounds and keep packages behind. */
    private static void fixLayout(Map<IShapeUIModel, int[]> bounds, List<IShapeUIModel> packageShapes) {
        for (Map.Entry<IShapeUIModel, int[]> e : bounds.entrySet()) {
            int[] b = e.getValue();
            e.getKey().setBounds(b[0], b[1], b[2], b[3]);
        }
        for (int i = packageShapes.size() - 1; i >= 0; i--) {
            packageShapes.get(i).sendToBack();
        }
    }

    private void relation(DiagramManager dm, IModelElementFactory factory, IDiagramUIModel diagram,
                          Map<String, IModelElement> models, Map<String, IShapeUIModel> shapes, String[] f) {
        String kind = f[1];
        IModelElement from = models.get(f[2]);
        IModelElement to = models.get(f[3]);
        IShapeUIModel fromShape = shapes.get(f[2]);
        IShapeUIModel toShape = shapes.get(f[3]);
        Point[] points = points(f[5]);
        IModelElement rel;

        switch (kind) {
            case "dependency": {
                IDependency dep = factory.createDependency();
                dep.setFrom(from);
                dep.setTo(to);
                rel = dep;
                break;
            }
            case "realization": {
                // VP: from = supplier (interface, gets the triangle), to = client (implementation)
                IRealization real = factory.createRealization();
                real.setFrom(to);
                real.setTo(from);
                rel = real;
                break;
            }
            default: {
                IAssociation assoc = factory.createAssociation();
                assoc.setFrom(from);
                assoc.setTo(to);
                IAssociationEnd fromEnd = (IAssociationEnd) assoc.getFromEnd();
                IAssociationEnd toEnd = (IAssociationEnd) assoc.getToEnd();
                if ("directed".equals(kind)) {
                    // VP treats "unspecified" as navigable, so a one-way arrow needs an explicit non-navigable end
                    fromEnd.setNavigable(IAssociationEnd.NAVIGABLE_NON_NAVIGABLE);
                    toEnd.setNavigable(IAssociationEnd.NAVIGABLE_NAVIGABLE);
                } else if ("aggregation".equals(kind)) {
                    fromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_SHARED);
                } else if ("composition".equals(kind)) {
                    fromEnd.setAggregationKind(IAssociationEnd.AGGREGATION_KIND_COMPOSITED);
                }
                rel = assoc;
            }
        }
        if (!f[4].isEmpty()) {
            rel.setName(f[4]);
        }
        IDiagramElement connector = "realization".equals(kind)
                ? dm.createConnector(diagram, rel, toShape, fromShape, reverse(points))
                : dm.createConnector(diagram, rel, fromShape, toShape, points);
        connector.resetCaption();
    }

    private static Point[] reverse(Point[] points) {
        Point[] result = new Point[points.length];
        for (int i = 0; i < points.length; i++) {
            result[i] = points[points.length - 1 - i];
        }
        return result;
    }

    private static Point[] points(String spec) {
        List<Point> result = new ArrayList<>();
        for (String xy : spec.split(";")) {
            if (xy.isBlank()) {
                continue;
            }
            String[] p = xy.split(",");
            result.add(new Point(Integer.parseInt(p[0]), Integer.parseInt(p[1])));
        }
        return result.toArray(new Point[0]);
    }

    private static int[] place(IShapeUIModel shape, String[] f, int offset) {
        int[] b = {Integer.parseInt(f[offset]), Integer.parseInt(f[offset + 1]),
                Integer.parseInt(f[offset + 2]), Integer.parseInt(f[offset + 3])};
        shape.setBounds(b[0], b[1], b[2], b[3]);
        shape.resetCaption();
        return b;
    }

    private static IModelElement parentModel(Map<String, IModelElement> models, String key, IPackage root) {
        return "-".equals(key) ? root : models.get(key);
    }

    private static void addToParentShape(Map<String, IShapeUIModel> shapes, String key, IShapeUIModel shape) {
        if (!"-".equals(key)) {
            shapes.get(key).addChild(shape);
        }
    }

    /** Makes re-runs idempotent: drops a previously imported diagram and its model package. */
    private static void removeExisting(IProject project, String diagramName, String packageName) {
        for (IDiagramUIModel d : project.toDiagramArray()) {
            if (diagramName.equals(d.getName())) {
                d.delete();
            }
        }
        for (IModelElement e : project.toModelElementArray(IModelElementFactory.MODEL_TYPE_PACKAGE)) {
            if (packageName.equals(e.getName())) {
                e.delete();
            }
        }
    }
}
