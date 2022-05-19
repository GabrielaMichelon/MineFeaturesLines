package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.Visitable;

import java.util.Objects;

/**
 * SuperClass for all define nodes (define, undef)
 */
public abstract class DefineNode extends NonConditionalNode implements Comparable<DefineNode>, Visitable {
    private final String macroName;

    public DefineNode(String name, int lineInfo, ConditionalNode parent) {
        super(lineInfo, parent);
        this.macroName = name;
    }

    public String getMacroName() {
        return macroName;
    }

    /**
     * Determines if the given node is identical to this one.
     * ATTENTION: This does also compare the line info of the given node.
     * use equals to check only for macro name.
     *
     * @param n
     * @return
     */
    public boolean isIdentical(DefineNode n) {
        return this.equals(n) && this.getLineInfo() == n.getLineInfo();
    }


    @Override
    public int compareTo(DefineNode o) {
        if (Integer.compare(getLineInfo(), o.getLineInfo()) == 0) {
            return getMacroName().compareTo(o.getMacroName());
        }
        return Integer.compare(getLineInfo(), o.getLineInfo());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefineNode that = (DefineNode) o;
        return getLineInfo() == that.getLineInfo() &&
                macroName.equals(that.macroName) &&
                Objects.equals(getParent(), that.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(macroName, getLineInfo());
    }
}
