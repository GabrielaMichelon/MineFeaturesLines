package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

/**
 * Class for representing a BinaryFile.
 * A Binary File does not contain any Features. It will be a leaf in the commit tree.
 */
public final class BinaryFileNode extends FileNode {

    public BinaryFileNode(RootNode parent, String filePath) {
        super(parent, filePath);
    }

    @Override
    public void accept(TreeVisitor v,String feature) {
        v.visit(this,feature);
    }
}
