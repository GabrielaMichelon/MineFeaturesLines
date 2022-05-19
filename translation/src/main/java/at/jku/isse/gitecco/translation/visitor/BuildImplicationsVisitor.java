package at.jku.isse.gitecco.translation.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;

import java.util.*;

public class BuildImplicationsVisitor implements TreeVisitor {

    private final Map<Feature, Queue<FeatureImplication>> implMap;
    private final String precondition;
    private final RootNode root;
    private final Integer line;
    private ArrayList<String> includeNodes;


    public BuildImplicationsVisitor(Map<Feature, Queue<FeatureImplication>> implMap, RootNode root, int line) {
        this.implMap = implMap;
        this.precondition = null;
        this.root = root;
        this.line = line;
        this.includeNodes = new ArrayList<>();
    }


    public BuildImplicationsVisitor(Map<Feature, Queue<FeatureImplication>> implMap, RootNode tree, IncludeNode includeNode, ArrayList<String> includeNodes) {
        this.implMap = implMap;
        this.precondition = includeNode.getCondition();
        this.root = tree;
        this.line = null;
        this.includeNodes = includeNodes;
    }

    public ArrayList<String> getIncludeNodes() {
        return includeNodes;
    }

    public void addIncludeNodes(String includeNode) {
        this.includeNodes.add(includeNode);
    }

    public Map<Feature, Queue<FeatureImplication>> getImplMap() {
        return implMap;
    }

    @Override
    public void visit(RootNode n, String feature) {

    }

    @Override
    public void visit(BinaryFileNode n, String feature) {

    }

    @Override
    public void visit(SourceFileNode n, String feature) {

    }

    @Override
    public void visit(ConditionBlockNode n, String feature) {

    }

    @Override
    public void visit(IFCondition c, String feature) {

    }

    @Override
    public void visit(IFDEFCondition c, String feature) {

    }

    @Override
    public void visit(IFNDEFCondition c, String feature) {

    }

    @Override
    public void visit(ELSECondition c, String feature) {

    }

    @Override
    public void visit(ELIFCondition c, String feature) {

    }

    @Override
    public void visit(Define d, String feature) {
        if( line != null && d.getLineInfo() > line) return;

        if((d.getParent() instanceof IFNDEFCondition) && (d.getLineInfo()-1 == d.getParent().getLineFrom())) {
            if(d.getParent().getLocalCondition().contains(d.getMacroName())) return;
        }

        String cond = precondition == null ? d.getCondition() : "(" + precondition + ") && (" + d.getCondition() + ")";
        FeatureImplication impl = new FeatureImplication(cond, d.getMacroName() + " == " + d.getMacroExpansion());

        Feature feat = new Feature(d.getMacroName());

        if(implMap.containsKey(feat)) {
            implMap.get(feat).add(impl);
        } else {
            implMap.put(feat, new LinkedList<FeatureImplication>());
        }
    }

    @Override
    public void visit(Undef d, String feature) {
        if( line != null && d.getLineInfo() > line) return;

        String cond = precondition == null ? d.getCondition() : "(" + precondition + ") && (" + d.getCondition() + ")";
        FeatureImplication impl = new FeatureImplication(cond, d.getMacroName() + " == 0");

        Feature feat = new Feature(d.getMacroName());

        if(implMap.containsKey(feat)) {
            implMap.get(feat).add(impl);
        } else {
            implMap.put(feat, new LinkedList<FeatureImplication>());
        }
    }

    @Override
    public void visit(IncludeNode n, String feature) {
        if(this.includeNodes.contains(n.getFileName())) return;
        if(line != null && n.getLineInfo()>line){
            this.includeNodes.add(n.getFileName());
            return;
        }

        this.includeNodes.add(n.getFileName());
        BuildImplicationsVisitor v = new BuildImplicationsVisitor(implMap, root, n, this.getIncludeNodes());
        FileNode sfn = root.getChild(n.getFileName());
        if(sfn != null) sfn.accept(v,null);
        //else System.out.println(n.getFileName() + " cannot be found in the repository");
    }

    @Override
    public void visit(BaseNode n, String feature) {

    }
}
