package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.git.Change;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Abstract class for representing a conditional expression.
 * Will be used for If, ifndef, ifdef, else conditions.
 */
public abstract class ConditionalNode extends ConditionNode {
    private int lineFrom = -1;
    private int lineTo = -1;
    private ArrayList<Integer> lineNumberInserts;
    private ArrayList<Integer> lineNumberDeleted;
    private ArrayList<Deltas> lineNumberInsertsAndDeletes;
    private final List<ConditionBlockNode> children;
    private final ConditionBlockNode parent;
    private final List<NonConditionalNode> definesAndIncludes = new ArrayList<>();
    //private List<DefineNode> defineNodes = new ArrayList();
    //private final List<IncludeNode> includeNodes = new ArrayList<>();

    public ConditionalNode(ConditionBlockNode parent) {
        this.children = new ArrayList<ConditionBlockNode>();
        this.parent = parent;
        this.lineNumberInsertsAndDeletes = new ArrayList<Deltas>();
        this.lineNumberInserts = new ArrayList<>();
        this.lineNumberDeleted = new ArrayList<>();
    }


    public ArrayList<Integer> getLineNumberInserts() {
        return lineNumberInserts;
    }

    public void addLineNumberInserts(int lineNumberInserts) {
        this.lineNumberInserts.add(lineNumberInserts);
    }

    public ArrayList<Integer> getLineNumberDeleted() {
        return lineNumberDeleted;
    }

    public void addLineNumberDeleted(int lineNumberDeleted) {
        this.lineNumberDeleted.add(lineNumberDeleted);
    }

    /**
     * Sets the deltas containing the number of lines that were added and deleted in a block
     * @param lineNumberInsertsAndDeletes
     */
    public void setLineNumberInsertsAndDeletes(ArrayList<Deltas> lineNumberInsertsAndDeletes) {
        this.lineNumberInsertsAndDeletes = lineNumberInsertsAndDeletes;
    }

    public void addDeltas(Deltas deltas){
        this.lineNumberInsertsAndDeletes.add(deltas);
    }

    /**
     * Retrieves the deltas containing the number of lines that were added and deleted in a block
     * @return
     */
    public ArrayList<Deltas> getLineNumberInsertsAndDeletes() {
        return this.lineNumberInsertsAndDeletes;
    }

    /**
     * Adds a new include as IncludeNode to the ConditionalNode.
     * @param n the IncludeNode
     */
    public void addInclude(IncludeNode n) {
        this.definesAndIncludes.add(n);
    }

    /**
     * Returns a list of all the include nodes contained in this ConditionalNode
     * @return list of all includes in the conditional node.
     */
    public List<IncludeNode> getIncludeNodes() {
        return definesAndIncludes
                .stream()
                .filter(x -> x instanceof IncludeNode)
                .map(x -> (IncludeNode) x)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns all the define nodes parented by this node.
     * @return
     */
    public List<DefineNode> getDefineNodes() {
        return definesAndIncludes
                .stream()
                .filter(x -> x instanceof DefineNode)
                .map(x -> (DefineNode) x)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<NonConditionalNode> getDefinesAndIncludes () {
        return definesAndIncludes;
    }

    /**
     * Adds a new define to the node.
     * @param d
     */
    public void addDefineNode(DefineNode d) {
        this.definesAndIncludes.add(d);
    }

    public void deleteDefineNode(DefineNode d){
        this.definesAndIncludes.remove(d);
    }

    /**
     * Evaluates if this Node contains a given change.
     * @param c
     * @return
     */
    public boolean containsChange(Change c) {
        if(c == null) return false;
        if(lineFrom == -1 || lineTo == -1) {
            //throw new IllegalStateException("line values have not been set correctly");
            if(lineFrom != -1 && lineTo == -1) {
                return lineFrom <= c.getFrom() && lineTo >= c.getFrom();
            }
        }
        return lineFrom <= c.getFrom() && lineTo >= c.getTo();
    }

    /**
     * Adds a child to this node.
     * Children are of types ConditionBlockNode. Which then will have ConditionalNodes as children.
     * @param n
     * @return
     */
    public ConditionBlockNode addChild(ConditionBlockNode n) {
        this.children.add(n);
        return n;
    }


    /**
     * Sets the LineFrom info. This can only be performed once.
     * On second call this will throw an IllegalAccessException.
     * @param lineFrom
     * @throws IllegalAccessException
     */
    public void setLineFrom(int lineFrom) throws IllegalAccessException {
        if(this.lineFrom == -1) this.lineFrom = lineFrom;
        else throw new IllegalAccessException("Cannot set the line more than once");
    }

    /**
     * Sets the LineTo info. This can only be performed once.
     * On second call this will thro an IllegalAccessException.
     * @param lineTo
     * @throws IllegalAccessException
     */
    public void setLineTo(int lineTo) throws IllegalAccessException {
        if(this.lineTo == -1) this.lineTo = lineTo;
        else throw new IllegalAccessException("Cannot set the line more than once");
    }

    /**
     * Retrieves the LineForm info.
     * --> start of this condition.
     * @return
     */
    public int getLineFrom() {
        return lineFrom;
    }

    @Override
    public ConditionBlockNode getParent() {
        return this.parent;
    }

    /**
     * Retrieves the LineTo info.
     * --> end of this condition.
     * @return
     */
    public int getLineTo() {
        return lineTo;
    }

    /**
     * Returns a list of all the children this node has.
     * @return
     */
    public List<ConditionBlockNode> getChildren() {
        return Collections.unmodifiableList(children);
    }


    /**
     * Returns the condition of this node.
     * @return
     */
    public abstract String getCondition();


    public abstract void setConString(String newCondition);

    /**
     * Returns just the local (immediate) condition of this node.
     * @return
     */
    public abstract String getLocalCondition();

    /**
     * Retrieves the filenode that contains this ConditionalNode
     * @return
     */
    public SourceFileNode getContainingFile() {
        Node n = this;
        while(!(n instanceof BaseNode)) n = n.getParent();

        return ((BaseNode) n).getFileNode();
    }

    public List<String> getContainingFileLines() {

        Node n = this;
        while(!(n instanceof BaseNode)) n = n.getParent();

        return ((BaseNode) n).getFileNode().getFileContent();
    }

    public List<String> getContainingPreviousFileLines() {

        Node n = this;
        while(!(n instanceof BaseNode)) n = n.getParent();

        return ((BaseNode) n).getFileNode().getPreviousFileContent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionalNode that = (ConditionalNode) o;
        return lineFrom == that.lineFrom &&
                lineTo == that.lineTo &&
                Objects.equals(lineNumberInserts, that.lineNumberInserts) &&
                Objects.equals(lineNumberDeleted, that.lineNumberDeleted) &&
                Objects.equals(lineNumberInsertsAndDeletes, that.lineNumberInsertsAndDeletes) &&
                Objects.equals(children, that.children) &&
                Objects.equals(parent, that.parent) &&
                Objects.equals(definesAndIncludes, that.definesAndIncludes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineFrom, lineTo, lineNumberInserts, lineNumberDeleted, lineNumberInsertsAndDeletes, children, parent, definesAndIncludes);
    }
}
