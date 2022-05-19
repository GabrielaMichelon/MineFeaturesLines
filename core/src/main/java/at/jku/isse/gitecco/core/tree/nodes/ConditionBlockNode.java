package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for representing a block of conditions.
 * Consisting of: if condition, elseif conditions, else condition.
 * All of the conditionals may also be IFDEF IFNDEF.
 */
public final class ConditionBlockNode extends ConditionNode implements Visitable {
    private final List<ELIFCondition> elseIfBlocks;
    private ConditionalNode ifBlock;
    private ELSECondition elseBlock;
    private final ConditionalNode parent;

    public ConditionBlockNode(ConditionalNode parent) {
        this.parent = parent;
        this.elseIfBlocks = new ArrayList<>();
    }

    /**
     * Constructor without parameter for declaring the BASE block.
     */
    public ConditionBlockNode() {
        this.parent = null;
        this.elseIfBlocks = new ArrayList<>();
    }

    /**
     * Set the IF-Block of this Block.
     * The IF Block is a ConditionalNode which then can be IF, IFDEF, IFNDEF
     * @param n
     * @return
     */
    public ConditionalNode setIfBlock(ConditionalNode n) {
        this.ifBlock = n;
        return n;
    }

    /**
     * sets the Else if block (from #ELIF)
     * may be a blokc of multiple elseifs.
     * @param n
     * @return
     */
    public ELIFCondition addElseIfBlock(ELIFCondition n) {
        this.elseIfBlocks.add(n);
        return n;
    }

    /**
     * Sets the Else block. Else blocks hav eno condition.
     * @param n
     * @return
     */
    public ELSECondition setElseBlock(ELSECondition n) {
        this.elseBlock = n;
        return n;
    }

    /**
     * Retrieves the ifBlock.
     * @return
     */
    public ConditionalNode getIfBlock() {
        return this.ifBlock;
    }

    /**
     * Retrieves the ElseBlock
     * @return
     */
    public ELSECondition getElseBlock() {
        return this.elseBlock;
    }

    /**
     * Return the elseIfBlocks as a unmodifiable List.
     * @return
     */
    public List<ELIFCondition> getElseIfBlocks() {
        return Collections.unmodifiableList(elseIfBlocks);
    }

    @Override
    public void accept(TreeVisitor v,String feature) {
        if(elseBlock != null) elseBlock.accept(v,feature);
        for (ELIFCondition elseIfBlock : elseIfBlocks) {
            elseIfBlock.accept(v,feature);
        }
        if(ifBlock != null) ifBlock.accept(v,feature);
        v.visit(this,feature);
    }

    @Override
    public ConditionalNode getParent() {
        return this.parent;
    }
}
