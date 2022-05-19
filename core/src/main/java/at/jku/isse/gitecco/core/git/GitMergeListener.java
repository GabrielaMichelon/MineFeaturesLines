package at.jku.isse.gitecco.core.git;

public interface GitMergeListener {
    void onMerge(GitCommit gc, GitCommitList gcl);
}
