package at.jku.isse.gitecco.featureid.featuretree.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetAllDefinesVisitor implements TreeVisitor {
    private final List<DefineNode> defines;
    private final RootNode tree;

    public GetAllDefinesVisitor(RootNode tree) {
        defines = new ArrayList<>();
        this.tree = tree;
    }

    /**
     * Returns all the found defines and undefs.
     * @return all the found defines and undefs.
     */
    public List<DefineNode> getDefines() {
        return Collections.unmodifiableList(defines);
    }

    /**
     * Resets the visitor for a new round of traversing.
     */
    public void reset() {
        this.defines.clear();
    }

    @Override
    public void visit(RootNode n,String feature) {

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
        if(feature!=null) {
            if (d.getCondition().equals(feature))
                defines.add(d);
        }else {
            defines.add(d);
        }
    }

    @Override
    public void visit(Undef d, String feature) {
        if(feature!=null) {
            if (d.getCondition().equals(feature))
                defines.add(d);
        }else {
            defines.add(d);
        }
    }

    @Override
    public void visit(IncludeNode n, String feature) {
        if(feature==null) {
            FileNode file = tree.getChild(n.getFileName());
            if (file != null) {
                file.accept(this, feature);
            }
        }
    }

    @Override
    public void visit(BaseNode n, String feature) {

    }
}
