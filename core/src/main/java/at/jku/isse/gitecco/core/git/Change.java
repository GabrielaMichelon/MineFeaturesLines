package at.jku.isse.gitecco.core.git;

import at.jku.isse.gitecco.core.tree.nodes.ConditionalNode;

import java.util.ArrayList;

/**
 * Class to store a change.
 * A change is defined by two coordinates.<br>
 * <code>from</code> and <code>to</code>. <br>
 * These two int values do indicate the line number over which this change appears.
 */
public class Change {
    private final int from;
    private final int to;
    private final ArrayList<Integer> lines;
    private int position = -1;
    private final String changeType;


    /**
     * Creates a new change.
     *
     * @param from int value of the starting line number of the change.
     * @param cnt  int value which indicates how many lines are covered by this change.
     */
    public Change(int from, int cnt, ArrayList<Integer> lines, int position, String changeType) {
        this.from = from;
        this.to = cnt;
        this.lines = lines;
        this.position = position;
        this.changeType = changeType;
        //this.to = from+cnt;
    }

    /**
     * Checks if a given ConditionalNode n lays inside of this change..
     * Used to determine if a new Feature was added.
     *
     * @param n The ConditionalNode to be checked.
     * @return True if the Feature is in fact in this change, otherwise false.
     */
    public boolean contains(ConditionalNode n) {
        return from <= n.getLineFrom() && to >= n.getLineTo();
    }

    /**
     * Gets the start of the change.
     *
     * @return int value of the starting line number of the change.
     */
    public int getFrom() {
        return this.from;
    }

    /**
     * Gets the end of the change.
     *
     * @return int value of the ending line number of the change.
     */
    public int getTo() {
        return this.to;
    }

    /**
     * Gets number of lines of the change
     * @return
     */
    public ArrayList<Integer> getLines() {
        return this.lines;
    }

    /**
     * Gets number of lines of the change of the old version of the file
     * @return
     */
    public int getPosition() {
        return this.position;
    }

    /**
     * Gets the type of the change: DELETE, INSERT, CHANGE
     * @return
     */
    public String getChangeType() {
        return changeType;
    }

    @Override
    public String toString() {
        return ""+from+","+to;
    }

}
