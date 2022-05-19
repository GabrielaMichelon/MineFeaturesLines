package at.jku.isse.gitecco.translation.mining;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

import java.util.*;

public class GetAllConditionalStatementsVisitor implements TreeVisitor {
    private Change change;
    private final Set<ConditionalNode> conditionalNodes;
    private final Set<ConditionalNode> negatedConditionalNodes;
    private final Map<Integer, Integer> linesConditionalNodes;

    public GetAllConditionalStatementsVisitor(Change change) {
        this.change = change;
        this.conditionalNodes = new HashSet<>();
        this.negatedConditionalNodes = new HashSet<>();
        this.linesConditionalNodes = new HashMap<>();
    }

    public GetAllConditionalStatementsVisitor() {
        this.conditionalNodes = new HashSet<>();
        this.negatedConditionalNodes =  new HashSet<>();
        this.linesConditionalNodes =  new HashMap<>();
    }

    public void reset() {
        this.change = null;
        this.conditionalNodes.clear();
        this.negatedConditionalNodes.clear();
        this.linesConditionalNodes.clear();
    }

    public void setChange(Change c) {
        this.change = c;
        this.conditionalNodes.clear();
        this.negatedConditionalNodes.clear();
        this.linesConditionalNodes.clear();
    }

    public Collection<ConditionalNode> getConditionalNodes() {
        return Collections.unmodifiableSet(this.conditionalNodes);
    }

    public Collection<ConditionalNode> getNegatedConditionalNodes(){
        return Collections.unmodifiableSet(this.negatedConditionalNodes);
    }

    public Map<Integer,Integer> getLinesConditionalNodes(){
        return this.linesConditionalNodes;
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
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.conditionalNodes.add(c);
            this.linesConditionalNodes.put(c.getLineFrom(),c.getLineTo());
            //this is necessary to mark newly added features as changed.
            if(!change.contains(c)) change = null;
            //change is set to null so that no further nodes will be interpreted as changed.
        }
    }

    @Override
    public void visit(IFDEFCondition c, String feature) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.conditionalNodes.add(c);
            this.linesConditionalNodes.put(c.getLineFrom(),c.getLineTo());
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(IFNDEFCondition c, String feature) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.negatedConditionalNodes.add(c);
            this.linesConditionalNodes.put(c.getLineFrom(),c.getLineTo());
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(ELSECondition c, String feature) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.negatedConditionalNodes.add(c);
            this.linesConditionalNodes.put(c.getLineFrom(),c.getLineTo());
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(ELIFCondition c, String feature) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.conditionalNodes.add(c);
            this.linesConditionalNodes.put(c.getLineFrom(),c.getLineTo());
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(Define d, String feature) {

    }

    @Override
    public void visit(Undef d, String feature) {

    }

    @Override
    public void visit(IncludeNode n, String feature) {

    }

    @Override
    public void visit(BaseNode n, String feature) {

    }
}
