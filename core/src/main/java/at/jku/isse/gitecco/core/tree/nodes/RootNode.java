package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class for a root node of a commit tree.
 */
public final class RootNode extends Node implements Visitable {
    private final String path;
    private final ArrayList<FileNode> children;

    /**
     * Creates a new root node. The path is the path of the repository.
     * @param path
     */
    public RootNode(String path) {
        this.path = path;
        this.children = new ArrayList<>();
    }

    /**
     * Adds a new child to the root node.
     * These children can only be an instance of a FileNode.
     * @param n
     */
    public void addChild(FileNode n) {
        children.add(n);
    }

    /**
     * Retrieves a child node for a given path
     * @param path
     * @return
     */
    public FileNode getChild(String path) {
        for (FileNode child : children) {
            if(child.getFilePath().contains(path)) return child;
        }
        return null;
    }

    /**
     * Adds a new child to the root node.
     * These children can only be an instance of a FileNode.
     * @param n
     */
    public void addAllChildren(Collection<FileNode> n) {
        children.addAll(n);
    }

    @Override
    public void accept(TreeVisitor v, String feature) {
        for (FileNode child : children) {
            v.visit(this,feature);
            child.accept(v,feature);
        }
    }

    public ArrayList<FileNode> getChildren() {
        return children;
    }

    public String getPath() {
        return path;
    }

    /**
     * Return null --> no parent.
     * @return
     */
    @Override
    public Node getParent() {
        return null;
    }
}
