package at.jku.isse.gitecco.core.git;

import java.io.IOException;

public interface GitCommitListener {
    void onCommit(GitCommit gc, GitCommitList gcl) throws IOException;
}
