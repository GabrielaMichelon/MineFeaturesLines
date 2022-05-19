package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.Visitable;

public abstract class NonConditionalNode extends ConditionNode implements Visitable {
    private final String condition;
    private final ConditionalNode parent;
    private final int lineInfo;

    public NonConditionalNode(int lineInfo, ConditionalNode parent) {
        this.condition = parent.getCondition();
        this.parent = parent;
        this.lineInfo = lineInfo;
    }

    public String getCondition() {
        return condition;
    }

    public int getLineInfo() {
        return lineInfo;
    }

    @Override
    public ConditionalNode getParent() {
        return this.parent;
    }
}
