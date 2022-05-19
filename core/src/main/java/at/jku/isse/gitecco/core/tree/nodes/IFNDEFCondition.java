package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an IFNDEF Condition.
 * Which means the condition of this Node must not be defined to be evaluated as true.
 */
public final class IFNDEFCondition extends ConditionalNode implements Visitable {
    private String condition;

    public IFNDEFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = "!(" + condition + ")";
    }

    @Override
    public String getCondition() {
        //getting the parent blocks
        String expression = "(" + this.condition + ")";
        if (!getLocalCondition().contains("BASE")) {
            ConditionalNode changedNodeParent = getParent().getIfBlock().getParent().getParent();
            ConditionalNode conditionalNode = changedNodeParent;
            if(conditionalNode.getLocalCondition().contains("BASE")){
                //System.out.println(conditionalNode.getLocalCondition());
                    return conditionalNode.getLocalCondition();
            }

            while (conditionalNode.getLocalCondition() != null &&  !(conditionalNode.getLocalCondition().contains("BASE")) ) {
                if(!(conditionalNode.getParent().getParent().getLocalCondition().contains("BASE"))){
                    expression += " && (" + conditionalNode.getLocalCondition() + ")";
                }
                conditionalNode = conditionalNode.getParent().getParent();
            }

            expression += " && (" + conditionalNode.getLocalCondition() + ")";
            //System.out.println(expression);
            return expression;
        }else{
            //System.out.println(getParent().getIfBlock().getParent().getParent().getCondition());
            return  getParent().getIfBlock().getParent().getParent().getCondition();
        }
        //return expression;
            //return this.condition;
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
