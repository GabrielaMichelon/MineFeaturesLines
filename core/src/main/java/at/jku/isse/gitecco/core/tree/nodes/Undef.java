package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.Objects;

/**
 * Class to represent a #undef preprocessor statement
 */
public final class Undef extends DefineNode implements Visitable {

    public Undef(String name, int lineInfo, ConditionalNode parent) {
        super(name, lineInfo, parent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Undef undef = (Undef) o;
        return Objects.equals(getMacroName(), undef.getMacroName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMacroName());
    }

    @Override
    public String toString() {
        return "#undef " + getMacroName();
    }

    @Override
    public void accept(TreeVisitor v,String feature) {
        v.visit(this,feature);
    }
}
