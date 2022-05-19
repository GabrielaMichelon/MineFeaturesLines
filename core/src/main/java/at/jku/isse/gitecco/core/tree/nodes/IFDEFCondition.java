package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an IFDEF Condition, which means the condition of this must
 * be defined for this to be evaluated as true.
 */
public final class IFDEFCondition extends ConditionalNode implements Visitable {
    private String condition;

    public IFDEFCondition(ConditionBlockNode parent, String condition) {
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
            while (conditionalNode.getLocalCondition() != null &&  !(conditionalNode.getLocalCondition().contains("BASE"))) {
                if(!(conditionalNode.getParent().getParent().getLocalCondition().contains("BASE"))){
                    expression += " && (" + conditionalNode.getLocalCondition() + ")";
                }
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
        //return getCondition();
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
