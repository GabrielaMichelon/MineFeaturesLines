package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing a #include&lt;filename&gt; preprocessor statement
 */
public final class IncludeNode extends NonConditionalNode implements Visitable {
    private final String fileName;

    public IncludeNode(String fileName, int lineInfo, ConditionalNode parent) {
        super(lineInfo, parent);
        this.fileName = fileName;
    }

    /**
     * Retrieves the name of the file which is to be included into the source file.
     * @return
     */
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public void accept(TreeVisitor v, String feature) {
        v.visit(this,feature);
    }
}
