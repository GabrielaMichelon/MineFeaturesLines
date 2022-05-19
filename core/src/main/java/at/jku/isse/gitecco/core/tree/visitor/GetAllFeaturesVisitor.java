package at.jku.isse.gitecco.core.tree.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GetAllFeaturesVisitor implements TreeVisitor {
    private final Set<Feature> features;

    public GetAllFeaturesVisitor() {
        this.features = new HashSet<>();
    }

    public Set<Feature> getAllFeatures() {
        return Collections.unmodifiableSet(features);
    }

    @Override
    public void visit(RootNode n,String feature) {

    }

    @Override
    public void visit(BinaryFileNode n,String feature) {

    }

    @Override
    public void visit(SourceFileNode n,String feature) {

    }

    @Override
    public void visit(ConditionBlockNode n,String feature) {

    }

    @Override
    public void visit(IFCondition c,String feature) {
        features.addAll(Feature.parseCondition(c.getCondition()));
    }

    @Override
    public void visit(IFDEFCondition c,String feature) {
        features.addAll(Feature.parseCondition(c.getCondition()));
    }

    @Override
    public void visit(IFNDEFCondition c,String feature) {
        features.addAll(Feature.parseCondition(c.getCondition()));
    }

    @Override
    public void visit(ELSECondition c,String feature) {
    }

    @Override
    public void visit(ELIFCondition c,String feature) {
        features.addAll(Feature.parseCondition(c.getLocalCondition()));
    }

    @Override
    public void visit(Define d,String feature) {

    }

    @Override
    public void visit(Undef d,String feature) {

    }

    @Override
    public void visit(IncludeNode n,String feature) {

    }

    @Override
    public void visit(BaseNode n,String feature) {
        features.addAll(Feature.parseCondition(n.getLocalCondition()));
    }
}
