package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an IFCondition.
 * Name speaks for itself.
 */
public final class IFCondition extends ConditionalNode implements Visitable {
    private String condition;

    public IFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public String getCondition() {
        //getting the parent blocks
        String expression = "(" + this.condition + ")";
        if (!getLocalCondition().contains("BASE")) {
            ConditionalNode changedNodeParent = getParent().getIfBlock().getParent().getParent();
            ConditionalNode conditionalNode = changedNodeParent;
            while (conditionalNode.getLocalCondition() != null && !(conditionalNode.getLocalCondition().contains("BASE"))) {
                expression += " && (" + conditionalNode.getLocalCondition() + ")";
                conditionalNode = conditionalNode.getParent().getParent();
            }
            expression += " && (" + conditionalNode.getLocalCondition() + ")";
            return expression;
        }
        return expression;
    }

    @Override
    public void setConString(String newCondition) {
        this.condition = newCondition;
    }

    @Override
    public String getLocalCondition() {
        return this.condition;
    }

    @Override
    public void accept(TreeVisitor v, String feature) {
        for (ConditionBlockNode child : getChildren()) {
            child.accept(v,feature);
        }
        for (NonConditionalNode node : getDefinesAndIncludes()) {
            node.accept(v,feature);
        }
        v.visit(this,feature);
    }

}
