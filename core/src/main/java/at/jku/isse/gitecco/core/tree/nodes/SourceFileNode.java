package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.Collection;

/**
 * Class for representing a source file of a repository in a commit tree.
 * A Source File has a BASE which will be the BASE feature.
 */
public final class SourceFileNode extends FileNode implements Visitable {
    /**The Base feature of this file.*/
    private BaseNode base;

    public SourceFileNode(RootNode parent, String filePath) {
        super(parent, filePath);
        base = null;
    }

    /**
     * Sets the given ConditionBlockNode as the BASE feature of the source file.
     * This can only be performed once. If this is called a second time an IllegalAccessException
     * will be raised.
     * @param n
     * @throws IllegalAccessException
     */
    public void setBase(BaseNode n) throws IllegalAccessException {
        if(base == null) this.base = n;
        else throw new IllegalAccessException("Cannot set base twice.");
    }

    public BaseNode getBaseNode() {
        return this.base;
    }

    public Collection<IncludeNode> getIncludesBase(){
        Collection<IncludeNode> includes = this.base.getIncludeNodes();
        return includes;
    }

    @Override
    public void accept(TreeVisitor v, String feature) {
        if(base != null) base.accept(v,feature);
        v.visit(this,feature);
    }
}
