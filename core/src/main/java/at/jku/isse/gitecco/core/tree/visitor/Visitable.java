package at.jku.isse.gitecco.core.tree.visitor;

public interface Visitable {
    void accept(TreeVisitor v, String feature);
}
