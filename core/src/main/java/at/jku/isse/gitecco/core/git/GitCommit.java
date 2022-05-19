package at.jku.isse.gitecco.core.git;

import at.jku.isse.gitecco.core.tree.nodes.RootNode;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Class for handling commits and be able to
 * distinguish between normal commits, branch points and merges.
 */
public class GitCommit {
    private RootNode tree;
    private final String commitName;
    private final String diffCommit;
    private final String branch;
    private final RevCommit revCommit;
    private final long number;
    private String commitMessage="";

    /**
     * Creates a new GitCommit
     * @param commitName
     * @param number
     * @param diffCommit
     * @param branch
     * @param rc
     * @param commitMessage
     */
    public GitCommit(String commitName, long number,String diffCommit, String branch, RevCommit rc, String commitMessage) {
        this.commitName = commitName;
        this.branch = branch;
        this.diffCommit = diffCommit;
        this.revCommit = rc;
        this.number = number;
        this.commitMessage = commitMessage;
    }

    /**
     * Creates a new GitCommit
     * @param commitName
     * @param number
     * @param diffCommit
     * @param branch
     * @param rc
     */
    public GitCommit(String commitName, long number,String diffCommit, String branch, RevCommit rc) {
        this.commitName = commitName;
        this.branch = branch;
        this.diffCommit = diffCommit;
        this.revCommit = rc;
        this.number = number;
    }

    public long getNumber() {
        return number;
    }

    /**
     * Dispose the tree and invoke garbage collection.
     * For memory saving purposes.
     */
    public void disposeTree() {
        this.tree = null;
    }

    /**
     * Gets the tree of a commit
     * @return
     */
    public RootNode getTree(){
        return tree;
    }

    /**
     * sets the tree once, a second call is not effective
     * @param n
     */
    public void setTree(RootNode n) {
        if(tree == null) tree = n;
    }

    /**
     * Gets the branch of the commit
     *
     * @return
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Gets the SHA1 ID aka the name of the commit to diff with
     *
     * @return the commit name / SHA1 ID
     */
    public String getDiffCommitName() {
        return diffCommit;
    }

    /**
     * Gets the SHA1 ID aka the name of the commit.
     *
     * @return the commit name / SHA1 ID
     */
    public String getCommitName() {
        return commitName;
    }

    public RevCommit getRevCommit() {
        return revCommit;
    }

    public String getCommitMessage() {
        return commitMessage;
    }
}
