package at.jku.isse.gitecco.core.tree.nodes;


import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an ELSECondition.
 * The name is slightly misleading since this has no condition on its own.
 * This feature/cond. depends on the corresponding IF/IFDEF/IFNDEF Condition.
 * It should represent an ELSE clause of an IF ELSE PPStatement.
 */
public final class ELSECondition extends ConditionalNode implements Visitable {

    public ELSECondition(ConditionBlockNode parent) {
        super(parent);
    }

    @Override
    public String getCondition() {
        StringBuilder ret = new StringBuilder();
        ret.append("!(" + getParent().getIfBlock().getLocalCondition() +")");
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            ret.append(" && !(" + elseIfBlock.getLocalCondition() + ")");
        }
        ret.append(" && ("+getParent().getParent().getCondition()+")");
        return ret.toString();
    }

    @Override
    public void setConString(String newCondition) {

    }

    @Override
    public String getLocalCondition() {
        return getCondition();
        //return "";
    }

    @Override
    public void accept(TreeVisitor v, String feature) {
        for (ConditionBlockNode child : getChildren()) {
            child.accept(v, feature);
        }
        for (NonConditionalNode node : getDefinesAndIncludes()) {
            node.accept(v, feature);
        }
        v.visit(this, feature);
    }
}
