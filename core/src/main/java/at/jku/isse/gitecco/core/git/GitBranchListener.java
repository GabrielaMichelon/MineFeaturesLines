package at.jku.isse.gitecco.core.git;

public interface GitBranchListener {
    void onBranch(GitCommit gc, GitCommitList gcl);
}
