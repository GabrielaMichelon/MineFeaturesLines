package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

public final class ELIFCondition extends ConditionalNode {

    private String condition;

    public ELIFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public String getCondition() {
        StringBuilder ret = new StringBuilder();
        ret.append("!(" + getParent().getIfBlock().getLocalCondition()+")");
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            if(this.equals(elseIfBlock)) {
                break;
            }
            ret.append(" && !(" + elseIfBlock.getLocalCondition()+")");
        }
        ret.append("&& ("+this.condition+")");

        if (!getLocalCondition().contains("BASE")) {
            ConditionalNode changedNodeParent = getParent().getIfBlock().getParent().getParent();
            ConditionalNode conditionalNode = changedNodeParent;
            while (conditionalNode.getLocalCondition() != null && !(conditionalNode.getLocalCondition().contains("BASE"))) {
                ret.append(" && (" + conditionalNode.getLocalCondition() + ")");
                conditionalNode = conditionalNode.getParent().getParent();
            }
            ret.append(" && (" + conditionalNode.getLocalCondition() + ")");
        }

        return ret.toString();
    }

    @Override
    public void setConString(String newCondition) {
        this.condition = newCondition;
    }

    @Override
    public String getLocalCondition() {
        if(this.condition.equals("!"))
            this.condition = "!BED_LIMIT_SWITCHING";
        return this.condition;
    }


    @Override
    public void accept(TreeVisitor v,String feature) {
        for (ConditionBlockNode child : getChildren()) {
            child.accept(v, feature);
        }
        for (NonConditionalNode node : getDefinesAndIncludes()) {
            node.accept(v, feature);
        }
        v.visit(this, feature);
    }
}
