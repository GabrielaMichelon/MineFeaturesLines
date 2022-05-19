package at.jku.isse.gitecco.core.git;

import at.jku.isse.gitecco.core.tree.nodes.BinaryFileNode;
import at.jku.isse.gitecco.core.tree.nodes.FileNode;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

import java.io.*;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.Files.copy;

/**
 * HelperClass for working with JGit.
 */
public class GitHelper {

    private static Git git;
    private final String pathUrl;
    private static String pathUrlAux;
    private final List<String> dirFiles;
    private Long runtimeGitCommit;

    public Long getRuntimeGitCommit() {
        return runtimeGitCommit;
    }

    public void setRuntimeGitCommit(Long runtimeGitCommit) {
        this.runtimeGitCommit = runtimeGitCommit;
    }

    //when having problems with repo --> following line checks out the latest commit
    //git checkout $(git log --branches -1 --pretty=format:"%H")

    /**
     * Creates a new instance of GitHelper and clones the specified
     * repo form the url String to the path String.
     *
     * @param url  The URL of the git repository to be cloned
     * @param path The path string, where the repository should be cloned to.
     * @throws Exception
     */
    public GitHelper(String url, String path, List<String> dirFiles) throws Exception {
        git = cloneRepo(url, path);
        pathUrl = path;
        pathUrlAux = path;
        this.dirFiles = dirFiles;
    }

    public GitHelper() {
        git = null;
        pathUrl = null;
        pathUrlAux = null;
        this.dirFiles = null;
    }

    public void gitCommitAndCheckout(String srcpathFiles, String destpathFiles, String commitName, String commitMessage) throws IOException, GitAPIException {
        Path sourcepath = Paths.get(srcpathFiles);
        Path destpath = Paths.get(destpathFiles);
        Files.setAttribute(destpath, "dos:readonly", false);
        File dest = new File(String.valueOf(destpath));
        Boolean initializeGit = true;
        if (destpath.toFile().exists()) {
            for (File path : dest.listFiles()) {
                if (path.isDirectory() && path.getName().equals(".git")) {
                    initializeGit = false;
                } else {
                    GitCommitList.recursiveDelete(path.toPath());
                }
            }
        } else {
            dest.mkdir();
            initializeGit = false;
        }

        if (initializeGit) {
            Git git = Git.init().setDirectory(dest).call();
        }

        //checkout of the specif commit of the original git project to copy the files that we need to simulate the GIT commit with the variant
        File srcPath = new File(srcpathFiles);
        Ref git = Git.open(srcPath)
                .checkout()
                .setName(commitName)
                .call();

        //end checkout of the specif commit of the original git project to copy the files that we need to simulate the GIT commit with the variant
        Path pathGit = Paths.get(sourcepath + File.separator + ".git");

        Files.walk(sourcepath)
                .forEach(source -> {
                    try {
                        if (!source.equals(pathGit))
                            copy(source, destpath.resolve(sourcepath.relativize(source)));
                    } catch (IOException e) {

                    }
                });

        File localPath = new File(destpathFiles);

        // Create the git repository with init


        // run the add-call

        Git gitOpen = Git.open(dest);
        gitOpen.add().addFilepattern(localPath.toString()).call();
        //git commit
        Long timeBefore = System.currentTimeMillis();
        gitOpen.commit().setMessage(commitMessage).call();
        Long timeAfter = System.currentTimeMillis();
        setRuntimeGitCommit(timeAfter - timeBefore);
        gitOpen.close();
        //end git commit


    }

    /**
     * Creates a new instance of GitHelper by opening an existing repository.
     * at the given path.
     * Note that the repository needs to be existing already.
     *
     * @param path The path String to the existing repository.
     * @throws IOException
     */
    public GitHelper(String path, List<String> dirFiles) throws IOException {
        git = openRepo(path);
        pathUrl = path;
        pathUrlAux = path;
        this.dirFiles = dirFiles;
    }

    public List<String> getDirFiles() {
        return dirFiles;
    }

    /**
     * Gets the path in which the repository is stored
     *
     * @return
     */
    public String getPath() {
        return this.pathUrl;
    }


    /**
     * Gets the Diff between two Commits that are not neighbors specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param sfn       the source file node which should be examined
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffsTwoCommits(GitCommit newCommit, FileNode sfn, Boolean deletedFile) throws Exception {
        if (sfn instanceof BinaryFileNode) throw new IllegalArgumentException("cannot diff a binary file node");
        return getFileDiffsTwoCommitsMethod(newCommit, sfn, deletedFile);
    }


    /**
     * Gets the Diff between two Commits that are not neighbors specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param filePath  The FilePath for which the Diff should be applied.
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffsTwoCommitsMethod(GitCommit newCommit, FileNode filePath, Boolean deletedFile) throws Exception {

        List<Change> changes = new ArrayList<Change>();

        if (deletedFile) {
            if (newCommit.getNumber() != Long.valueOf(0))
                git.stashCreate().call();//setRef("HEAD").call();
            //git.stashDrop().setAll(true).call();
            git.checkout().setName(newCommit.getDiffCommitName()).call();
            String filePathRenamed = "";
            if (filePath.getFilePath().contains("arent"))
                filePathRenamed = filePath.getFilePath().replace("arent" + File.separator, "");
            else
                filePathRenamed = filePath.getFilePath();
            String newPath = pathUrl + File.separator + filePathRenamed;
            ArrayList<Integer> lines = new ArrayList<>();
            lines.add(0);
            lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
            if (filePath.getFileContent().size() == 0) {
                List<String> linesFile = Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1);
                filePath.setFileContent(linesFile);
            }
            changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, 1, "DELETE"));
            git.stashCreate().call();//setRef("HEAD").call();
            //git.stashDrop().setAll(true).call();
            git.checkout().setName(newCommit.getCommitName()).call();
        } else {
            //prepare for file path filter.
            //String filterPath = filePath.substring(pathUrl.length()+1).replace("\\", "/");
            try {
                git.stashCreate().call();//setRef("HEAD").call();
                //git.stashDrop().setAll(true).call();
                git.checkout().setName(newCommit.getCommitName()).call();
            } catch (CheckoutConflictException ex) {
                System.out.println("aaaaaaaException checkout ----- " + ex);
            }
            //System.out.println("Checked out: "+newCommit.getCommitName());
            List<DiffEntry> diff;
            diff = git.diff().
                    setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                    setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                    //setPathFilter(PathFilter.create(filePath)).
                            call();
            //to filter on Suffix use the following instead
            //setPathFilter(PathSuffixFilter.create(".cpp"))

            for (DiffEntry entry : diff) {
                String newPath = entry.getNewPath().replace("/", "\\");
                String oldPath = entry.getOldPath().replace("/", "\\");
                if (filePath.getFilePath().equals(newPath)) {
                    //System.out.println("file diff: " + newPath);
                    if (entry.getChangeType().toString().equals("ADD")) {
                        newPath = pathUrl + File.separator + newPath;
                        ArrayList<Integer> lines = new ArrayList<>();
                        lines.add(0);
                        lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
                        if (filePath.getFileContent().size() == 0) {
                            List<String> linesFile = Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1);
                            filePath.setFileContent(linesFile);
                        }
                        changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, 1, "INSERT"));
                    } else if (entry.getChangeType().toString().equals("MODIFY")) {
                        List<String> actual = new ArrayList<>();
                        List<String> old = new ArrayList<>();
                        File filenew = new File(pathUrl + File.separator + newPath);
                        if (filePath.getFileContent().size() == 0) {
                            List<String> linesFile = Files.readAllLines(Paths.get(filenew.getAbsolutePath()), StandardCharsets.ISO_8859_1);
                            filePath.setFileContent(linesFile);
                            git.stashCreate().call();//setRef("HEAD").call();
                            //git.stashDrop().setAll(true).call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                            if (filePath.getPreviousFileContent().size() == 0) {
                                linesFile = Files.readAllLines(Paths.get(filenew.getAbsolutePath()), StandardCharsets.ISO_8859_1);
                                filePath.setPreviousFileContent(linesFile);
                            }
                            git.stashCreate().call();//setRef("HEAD").call();
                            //git.stashDrop().setAll(true).call();
                            //git.reset().setMode(ResetCommand.ResetType.HARD).call();
                            git.checkout().setName(newCommit.getCommitName()).call();
                        }
                        try {
                            actual = Files.readAllLines(filenew.toPath());
                        } catch (MalformedInputException e) {
                            //StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                actual.add(sCurrentLine);
                            }
                            br.close();
                        }
                        try {
                            git.stashCreate().call();//setRef("HEAD").call();
                            //git.stashDrop().setAll(true).call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                        } catch (CheckoutConflictException ex) {
                            System.out.println("Exception checkout ----- " + ex);
                        }
                        File fileold = new File(pathUrl + File.separator + oldPath);
                        try {
                            old = Files.readAllLines(fileold.toPath());
                        } catch (MalformedInputException e) {
                            StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(fileold.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                old.add(sCurrentLine);
                            }
                            br.close();
                        }
                        git.stashCreate().call();//setRef("HEAD").call();
                        //git.stashDrop().setAll(true).call();
                        git.checkout().setName(newCommit.getCommitName()).call();
                        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                        Patch<String> patch = null;
                        patch = DiffUtils.diff(old, actual);
                        for (Delta delta : patch.getDeltas()) {
                            String changeType = null;
                            Integer first = 0, last = 0;
                            if (delta.getType().toString().equals("DELETE")) {
                                changeType = delta.getType().toString();
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size() - 1;
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last);
                                int position = delta.getOriginal().getPosition() + 1;
                                changes.add(new Change(first, last, lines, position, changeType));
                            } else if (delta.getType().toString().equals("INSERT")) {
                                changeType = delta.getType().toString();
                                first = delta.getRevised().getPosition();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                int position = delta.getOriginal().getPosition() + 1;
                                changes.add(new Change(first, last, lines, position, changeType));
                            } else if (delta.getType().toString().equals("CHANGE")) {
                                first = delta.getRevised().getPosition();
                                ArrayList<Integer> lines = new ArrayList<>();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                lines.add(first);
                                lines.add(last - 1);
                                //changes.add(new Change(first, last, lines, "CHANGE"));
                                int position = delta.getOriginal().getPosition() + 1;
                                //changes.add(new Change(first, last, lines, previousLines, "INSERT"));
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size();
                                //lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, position, "CHANGE"));
                            }
                        }

                    }
                }

            }
        }
        return changes.toArray(new Change[changes.size()]);
    }


    /**
     * Gets the Diff between two Commits specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param sfn       the source file node which should be examined
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffs(GitCommit newCommit, FileNode sfn, Boolean deletedFile) throws Exception {
        if (sfn instanceof BinaryFileNode) throw new IllegalArgumentException("cannot diff a binary file node");
        return getFileDiffsMethod(newCommit, sfn, deletedFile);
    }


    /**
     * Gets the Diff between two Commits specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param sfn       the source file node which should be examined
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffs(GitCommit newCommit, FileNode sfn, Boolean deletedFile, Boolean newFile) throws Exception {
        if (sfn instanceof BinaryFileNode) throw new IllegalArgumentException("cannot diff a binary file node");
        return getFileDiffsMethod(newCommit, sfn, deletedFile, newFile);
    }


    /**
     * Gets the Diff between two Commits specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param filePath  The FilePath for which the Diff should be applied.
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffsMethod(GitCommit newCommit, FileNode filePath, Boolean deletedFile) throws Exception {

        List<Change> changes = new ArrayList<Change>();

        if (deletedFile) {
            if (newCommit.getNumber() != Long.valueOf(0))
                git.stashCreate().call();//setRef("HEAD").call();
            //git.stashDrop().setAll(true).call();
            git.checkout().setName(newCommit.getDiffCommitName()).call();
            String filepathRenamed = "";
            if (filePath.getFilePath().contains("arent"))
                filepathRenamed = filePath.getFilePath().replace("arent" + File.separator, "");
            else
                filepathRenamed = filePath.getFilePath();
            String newPath = pathUrl + File.separator + filepathRenamed;
            if (filePath.getFileContent().size() == 0) {
                List<String> linesFile = Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1);
                filePath.setFileContent(linesFile);
            }
            ArrayList<Integer> lines = new ArrayList<>();
            lines.add(0);
            lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
            changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, 1, "DELETE"));
            git.stashCreate().call();//setRef("HEAD").call();
            //git.stashDrop().setAll(true).call();
            git.checkout().setName(newCommit.getCommitName()).call();
        } else {
            //prepare for file path filter.
            //String filterPath = filePath.substring(pathUrl.length()+1).replace("\\", "/");
            try {
                git.stashCreate().call();//setRef("HEAD").call();
                //git.stashDrop().setAll(true).call();
                git.checkout().setName(newCommit.getCommitName()).call();
            } catch (CheckoutConflictException ex) {
                System.out.println("aaaaaaaException checkout ----- " + ex);
            }
            //System.out.println("Checked out: "+newCommit.getCommitName());
            List<DiffEntry> diff;
            diff = git.diff().
                    setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                    setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                    //setPathFilter(PathFilter.create(filePath)).
                            call();
            //to filter on Suffix use the following instead
            //setPathFilter(PathSuffixFilter.create(".cpp"))


            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            DiffParser fileDiffParser = new DiffParser();


            for (DiffEntry entry : diff) {
                String newPath = entry.getNewPath().replace("/", "\\");
                String oldPath = entry.getOldPath().replace("/", "\\");
                if (filePath.getFilePath().equals(newPath)) {
                    //System.out.println("file diff: " + newPath);
                    if (filePath.getFileContent().size() == 0) {
                        List<String> linesFile = Files.readAllLines(Paths.get(pathUrl + File.separator + newPath), StandardCharsets.ISO_8859_1);
                        filePath.setFileContent(linesFile);
                    }
                    if (entry.getChangeType().toString().equals("ADD")) {
                        newPath = pathUrl + File.separator + newPath;
                        ArrayList<Integer> lines = new ArrayList<>();
                        lines.add(0);
                        lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
                        changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, 1, "INSERT"));
                    } else if (entry.getChangeType().toString().equals("MODIFY")) {
                        List<String> actual = new ArrayList<>();
                        List<String> old = new ArrayList<>();
                        File filenew = new File(pathUrl + File.separator + newPath);
                        try {
                            actual = Files.readAllLines(filenew.toPath());
                        } catch (MalformedInputException e) {
                            StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                actual.add(sCurrentLine);
                            }
                            br.close();
                        }
                        try {
                            git.stashCreate().call();//setRef("HEAD").call();
                            //git.stashDrop().setAll(true).call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                        } catch (CheckoutConflictException ex) {
                            System.out.println("Exception checkout ----- " + ex);
                        }
                        File fileold = new File(pathUrl + File.separator + oldPath);
                        try {
                            old = Files.readAllLines(fileold.toPath());
                        } catch (MalformedInputException e) {
                            StringBuilder contentBuilder = new StringBuilder();
                            BufferedReader br = new BufferedReader(new FileReader(fileold.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                old.add(sCurrentLine);
                            }
                            br.close();
                        }
                        git.stashCreate().call();//setRef("HEAD").call();
                        //git.stashDrop().setAll(true).call();
                        git.checkout().setName(newCommit.getCommitName()).call();
                        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                        Patch<String> patch = null;
                        patch = DiffUtils.diff(old, actual);
                        for (Delta delta : patch.getDeltas()) {
                            String changeType = null;
                            Integer first = 0, last = 0;
                            if (delta.getType().toString().equals("DELETE")) {
                                changeType = delta.getType().toString();
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size() - 1;
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last);
                                changes.add(new Change(first, last, lines, delta.getOriginal().getPosition() + 1, changeType));
                            } else if (delta.getType().toString().equals("INSERT")) {
                                changeType = delta.getType().toString();
                                first = delta.getRevised().getPosition();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, delta.getOriginal().getPosition() + 1, changeType));
                            } else if (delta.getType().toString().equals("CHANGE")) {
                                first = delta.getRevised().getPosition();
                                ArrayList<Integer> lines = new ArrayList<>();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                lines.add(first);
                                lines.add(last - 1);
                                //changes.add(new Change(first, last, lines, "CHANGE"));
                                int position = delta.getOriginal().getPosition() + 1;
                                //changes.add(new Change(first, last, lines, previousLines, "INSERT"));
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size();
                                //lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, position, "CHANGE"));
                                //changes.add(new Change(first, last, lines, "DELETE"));
                            }
                        }

                    }
                }

            }
        }
        return changes.toArray(new Change[changes.size()]);
    }

    /**
     * Gets the Diff between two Commits specified by their commit names.
     * The Diff is stored as a <code>Change</code>.
     * All the changes will be returned as an Array.
     *
     * @param newCommit The commit which should be diffed --> also contains the parent to diff with
     * @param filePath  The FilePath for which the Diff should be applied.
     * @return An Array of Changes which contains all the changes between the commits.
     * @throws Exception
     */
    public Change[] getFileDiffsMethod(GitCommit newCommit, FileNode filePath, Boolean deletedFile, Boolean newFile) throws Exception {

        List<Change> changes = new ArrayList<Change>();

        if (deletedFile) {
            if (newCommit.getNumber() != Long.valueOf(0))
                git.stashCreate().call();//setRef("HEAD").call();
            //git.stashDrop().setAll(true).call();
            git.checkout().setName(newCommit.getDiffCommitName()).call();
            String filepathRenamed = "";
            if (filePath.getFilePath().contains("arent"))
                filepathRenamed = filePath.getFilePath().replace("arent" + File.separator, "");
            else
                filepathRenamed = filePath.getFilePath();
            String newPath = pathUrl + File.separator + filepathRenamed;
            if (filePath.getFileContent().size() == 0) {
                List<String> linesFile = Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1);
                filePath.setFileContent(linesFile);
            }
            ArrayList<Integer> lines = new ArrayList<>();
            lines.add(0);
            lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
            changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, 1, "DELETE"));
            git.stashCreate().call();//setRef("HEAD").call();
            //git.stashDrop().setAll(true).call();
            git.checkout().setName(newCommit.getCommitName()).call();
        } else if (newFile) {
            try {
                git.stashCreate().call();//setRef("HEAD").call();
                //git.stashDrop().setAll(true).call();
                git.checkout().setName(newCommit.getCommitName()).call();
            } catch (CheckoutConflictException ex) {
                System.out.println("Exception checkout Git ----- " + ex);
            }
            List<DiffEntry> diff;
            diff = git.diff().
                    setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                    setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                    call();

            for (DiffEntry entry : diff) {
                String newPath = entry.getNewPath().replace("/", "\\");
                if (filePath.getFilePath().equals(newPath)) {
                    if (filePath.getFileContent().size() == 0) {
                        List<String> linesFile = Files.readAllLines(Paths.get(pathUrl + File.separator + newPath), StandardCharsets.ISO_8859_1);
                        filePath.setFileContent(linesFile);
                    }
                    if (entry.getChangeType().toString().equals("ADD")) {
                        newPath = pathUrl + File.separator + newPath;
                        ArrayList<Integer> lines = new ArrayList<>();
                        lines.add(0);
                        lines.add(Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size() - 1);
                        changes.add(new Change(0, Files.readAllLines(Paths.get(newPath), StandardCharsets.ISO_8859_1).size(), lines, 1, "INSERT"));
                    }
                }

            }
        } else { //changedFile
            try {
                git.stashCreate().call();//setRef("HEAD").call();
                //git.stashDrop().setAll(true).call();
                git.checkout().setName(newCommit.getCommitName()).call();
            } catch (CheckoutConflictException ex) {
                System.out.println("Exception checkout Git ----- " + ex);
            }
            List<DiffEntry> diff;
            diff = git.diff().
                    setOldTree(prepareTreeParser(git.getRepository(), newCommit.getDiffCommitName())).
                    setNewTree(prepareTreeParser(git.getRepository(), newCommit.getCommitName())).
                    call();

            for (DiffEntry entry : diff) {
                String newPath = entry.getNewPath().replace("/", "\\");
                String oldPath = entry.getOldPath().replace("/", "\\");
                if (filePath.getFilePath().equals(oldPath)) {
                    if (filePath.getFileContent().size() == 0) {
                        List<String> linesFile = Files.readAllLines(Paths.get(pathUrl + File.separator + oldPath), StandardCharsets.ISO_8859_1);
                        filePath.setFileContent(linesFile);
                    }
                    if (entry.getChangeType().toString().equals("MODIFY")) {
                        List<String> actual = new ArrayList<>();
                        List<String> old = new ArrayList<>();
                        File filenew = new File(pathUrl + File.separator + oldPath);
                        try {
                            actual = Files.readAllLines(filenew.toPath());
                        } catch (MalformedInputException e) {
                            BufferedReader br = new BufferedReader(new FileReader(filenew.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                actual.add(sCurrentLine);
                            }
                            br.close();
                        }
                        try {
                            git.stashCreate().call();//setRef("HEAD").call();
                            //git.stashDrop().setAll(true).call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                        } catch (CheckoutConflictException ex) {
                            System.out.println("Exception checkout Git ----- " + ex);
                        }
                        File fileold = new File(pathUrl + File.separator + newPath);
                        try {
                            old = Files.readAllLines(fileold.toPath());
                        } catch (MalformedInputException e) {
                            BufferedReader br = new BufferedReader(new FileReader(fileold.getAbsoluteFile()));
                            String sCurrentLine;
                            while ((sCurrentLine = br.readLine()) != null) {
                                old.add(sCurrentLine);
                            }
                            br.close();
                        }

                        if (filePath.getPreviousFileContent().size() == 0) {
                            git.stashCreate().call();//setRef("HEAD").call();
                            //git.stashDrop().setAll(true).call();
                            git.checkout().setName(newCommit.getDiffCommitName()).call();
                            List<String> linesFile = Files.readAllLines(Paths.get(filenew.getAbsolutePath()), StandardCharsets.ISO_8859_1);
                            filePath.setPreviousFileContent(linesFile);
                        }
                        git.stashCreate().call();//setRef("HEAD").call();
                        //git.stashDrop().setAll(true).call();
                        git.checkout().setName(newCommit.getCommitName()).call();
                        // Compute diff. Get the Patch object. Patch is the container for computed deltas.
                        Patch<String> patch = null;
                        patch = DiffUtils.diff(old, actual);
                        for (Delta delta : patch.getDeltas()) {
                            String changeType = null;
                            Integer first = 0, last = 0;
                            if (delta.getType().toString().equals("DELETE")) {
                                changeType = delta.getType().toString();
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size() - 1;
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last);
                                changes.add(new Change(first, last, lines, delta.getOriginal().getPosition() + 1, changeType));
                            } else if (delta.getType().toString().equals("INSERT")) {
                                changeType = delta.getType().toString();
                                first = delta.getRevised().getPosition();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                ArrayList<Integer> lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, delta.getOriginal().getPosition() + 1, changeType));
                            } else if (delta.getType().toString().equals("CHANGE")) {
                                first = delta.getRevised().getPosition();
                                ArrayList<Integer> lines = new ArrayList<>();
                                last = delta.getRevised().getPosition() + delta.getRevised().getLines().size();
                                lines.add(first);
                                lines.add(last - 1);
                                //changes.add(new Change(first, last, lines, "CHANGE"));
                                int position = delta.getOriginal().getPosition() + 1;
                                //changes.add(new Change(first, last, lines, previousLines, "INSERT"));
                                first = delta.getOriginal().getPosition();
                                last = delta.getOriginal().getPosition() + delta.getOriginal().getLines().size();
                                //lines = new ArrayList<>();
                                lines.add(first);
                                lines.add(last - 1);
                                changes.add(new Change(first, last, lines, position, "CHANGE"));
                                //changes.add(new Change(first, last, lines, "DELETE"));
                            }
                        }

                    }
                }
            }
        }

        //**************
        return changes.toArray(new Change[changes.size()]);
    }


    private Git openRepo(String dirPath) throws IOException {
        File dir = new File(dirPath);
        Git git = Git.open(dir);

        return git;
    }

    /**
     * Checks out a commit by the given name.
     * Does this by using the runtime execution since JGit is buggy
     * when it comes to checkouts and cleans, etc.
     *
     * @param name The name of the commit, which should be checked out.
     */
    public void checkOutCommit(String name) {
        /*System.out.println("Checking out commit: "+name
                +"\n at "+pathUrl);*/
        try {
            git.clean().setForce(true).call();
            //git.reset().setMode(ResetCommand.ResetType.HARD).call();
            try {
                git.stashCreate()
                        .call();
                //git.stashDrop().setAll(true).call();
                //git.stashCreate().call();//setRef("HEAD").call();
                //git.reset().setMode(ResetCommand.ResetType.HARD).call();
                git.checkout().setName(name).call();
            } catch (CheckoutConflictException e) {
                System.out.println(" EXCECPTION CHECKOUT. " + e);
            }

        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }


    public Git cloneRepo(String url, String dirPath) {
        File dir = new File(dirPath);
        //System.out.println("Cloning from " + url + " to " + dir);

        Git git = null;
        try {
            git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(dir)
                    .call();
        } catch (GitAPIException e) {
            System.err.println("problems checking out the given repo to the given destination path");
            e.printStackTrace();
        }

        //System.out.println("Having repository: " + git.getRepository().getDirectory() + "\n");

        return git;
    }


    /**
     * Method to retrieve all commits form a repository and put it to a GitCommitList.
     *
     * @param commits the GitCommitList to which the commits a re saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getAllCommits(GitCommitList commits) throws Exception {
        final boolean FASTMODE = false;
        final Repository repository = git.getRepository();
        final Collection<Ref> allRefs = repository.getRefDatabase().getRefs();

        RevWalk revWalk = new RevWalk(repository);
        revWalk.sort(RevSort.TOPO, true);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);

        //revWalk.markStart(revWalk.parseCommit(((List<Ref>) allRefs).get(0).getObjectId()));
        for (Ref ref : allRefs) revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));

        long number = 0;
        for (RevCommit rc : revWalk) {
            String branch = FASTMODE ? null : getBranchOfCommit(rc.getName());
            String parent;

            try {
                parent = rc.getParent(0).getName();
            } catch (ArrayIndexOutOfBoundsException e) {
                parent = "NULLCOMMIT";
            }
            commits.add(new GitCommit(rc.getName(), number, parent, branch, rc));
            number++;
        }

        return commits;
    }


    /**
     * Method to retrieve the diff of two commits from a repository and put it to a GitCommitList.
     * starts with a specific commit and ends with another specific commit hash
     *
     * @param commits the GitCommitList to which the commits are saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getTwoCommits(GitCommitList commits, String firstcommit, String secondcommit) throws
            Exception {
        final Repository repository = git.getRepository();
        List<Ref> tags = new ArrayList<Ref>(repository.getTags().values());
        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.setRetainBody(false);
        ObjectId id = repository.resolve(firstcommit);
        RevCommit rc = revWalk.parseCommit(id);
        String parent;

        //System.out.println(rc.getName());
        String branch = getBranchOfCommit(rc.getName());

        try {
            parent = rc.getParent(0).getName();
        } catch (ArrayIndexOutOfBoundsException e) {
            parent = "NULLCOMMIT";
        }
        commits.add(new GitCommit(rc.getName(), 1, parent, branch, rc));

        id = repository.resolve(secondcommit);
        rc = revWalk.parseCommit(id);
        try {
            parent = rc.getParent(0).getName();
        } catch (ArrayIndexOutOfBoundsException e) {
            parent = "NULLCOMMIT";
        }
        commits.add(new GitCommit(rc.getName(), 2, parent, branch, rc));

        return commits;
    }


    /**
     * Method to retrieve the diff of two commits from a repository and put it to a GitCommitList.
     * starts with a specific commit and ends with another specific commit hash
     *
     * @param commits the GitCommitList to which the commits are saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getOneCommit(GitCommitList commits, String firstcommit) throws
            Exception {
        final Repository repository = git.getRepository();
        List<Ref> tags = new ArrayList<Ref>(repository.getTags().values());
        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.setRetainBody(false);
        ObjectId id = repository.resolve(firstcommit);
        RevCommit rc = revWalk.parseCommit(id);
        String parent;

        //System.out.println(rc.getName());
        String branch = getBranchOfCommit(rc.getName());

        try {
            parent = rc.getParent(0).getName();
        } catch (ArrayIndexOutOfBoundsException e) {
            parent = "NULLCOMMIT";
        }
        commits.add(new GitCommit(rc.getName(), 1, parent, branch, rc));

        return commits;
    }


    /**
     * Method to retrieve every nth commit from a repository and put it to a GitCommitList.
     * starts with a certain commit number and ends with a certain commit number
     *
     * @param commits the GitCommitList to which the commits are saved to.
     * @return The GitCommitList which was passed to the method.
     * @throws GitAPIException
     * @throws IOException
     */
    public GitCommitList getEveryNthCommit2(GitCommitList commits, String release, String firstDiff,
                                            int startcommit, int endcommit, int n) throws Exception {
        final Repository repository = git.getRepository();
        List<Ref> tags = new ArrayList<Ref>(repository.getTags().values());
        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.setRetainBody(false);

        for (Ref ref : tags) {
            if (ref.getName().equals(release)) {
                revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                break;
            }
        }
        long number = 0;
        String parent = firstDiff;
        boolean enter = false;
        if (startcommit != 0)
            startcommit -= 1;
        for (RevCommit rc : revWalk) {
            if (number > endcommit) break;

            if (number >= startcommit) {
                String branch = getBranchOfCommit(rc.getName());

                if (parent == null || enter) {
                    try {
                        parent = rc.getParent(0).getName();
                        enter = true;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        parent = "NULLCOMMIT";
                        enter = true;
                    }
                } else {
                    enter = true;
                }

                commits.add(new GitCommit(rc.getName(), number, parent, branch, rc));
            }

            number += number < startcommit ? 1 : n;
        }

        return commits;
    }


    /**
     * Method to retrieve every tags and each commit number where each release is finished
     *
     * @return The a map with commit number of each release/tag of the repository.
     * @throws GitAPIException
     * @throws IOException
     */
    public Map<Long, String> getCommitNumberTag() throws Exception {
        final Repository repository = git.getRepository();
        List<Ref> tags = new ArrayList<Ref>(repository.getTags().values());
        RevWalk revWalk = new RevWalk(repository);
        revWalk.setRetainBody(false);
        revWalk.sort(RevSort.REVERSE, true);
        revWalk.setRevFilter(RevFilter.NO_MERGES);
        revWalk.sort(RevSort.COMMIT_TIME_DESC);
        revWalk.setRetainBody(false);

        Map<Long, String> tagsCommits = new HashMap<>();
        ArrayList<Long> tagsNumberCommits = new ArrayList<>();

        for (Ref ref : tags) {
            revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
            long number = 0;
            for (RevCommit rc : revWalk) {
                number++;
            }
            tagsCommits.put(number, ref.getName());
            tagsNumberCommits.add(number);
            revWalk.dispose();
        }

        Collections.sort(tagsNumberCommits);
        for (Long nr : tagsNumberCommits) {
            System.out.println("Commit number: " + nr + " tag: " + tagsCommits.get(nr));

        }
        tags.clear();
        tagsNumberCommits.clear();

        return tagsCommits;

    }


    /**
     * Retrieves the first branch of a commit.
     * Actually not very useful. --> disabled through the fastmode in getAllCommits.
     *
     * @param commit
     * @return
     * @throws MissingObjectException
     * @throws GitAPIException
     */
    private String getBranchOfCommit(String commit) throws MissingObjectException, GitAPIException {
        Map<ObjectId, String> map = git
                .nameRev()
                .addPrefix("refs/heads")
                .add(ObjectId.fromString(commit))
                .call();

        //TODO: probably better solution: one commit may have multiple branches
        return map.isEmpty() ? "" : map.get(ObjectId.fromString(commit)).split("~")[0];
    }


    private static AbstractTreeIterator prepareTreeParser(Repository repository, String commitName) throws
            IOException {
        if (commitName == null || commitName.equals("NULLCOMMIT")) return new EmptyTreeIterator();

        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(repository.resolve(commitName));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();

            return treeParser;
        }
    }

    public static void copyFolder(File source, String gitDirAux, File
            destination, ArrayList<String> filesRemoved, Map<String, Map<Integer, FileChange>> filesChanged) {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }

            String files[] = source.list();

            for (String file : files) {
                File srcFile = new File(source, file);
                if (!srcFile.getAbsolutePath().contains(".git")) {
                    File destFile = new File(destination, file);
                    copyFolder(srcFile, gitDirAux, destFile, filesRemoved, filesChanged);
                }
            }
        } else {
            String sourceFileDir = source.getAbsolutePath().substring(source.getAbsolutePath().lastIndexOf(gitDirAux) + gitDirAux.length() + 1);
            if (!filesRemoved.contains(sourceFileDir) && !filesChanged.keySet().contains(sourceFileDir)) {
                try {
                    FileUtils.copyFile(source, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void copyNewFiles(File source, String gitDirAux, File
            destination, ArrayList<String> filesAdded, Map<String, Map<Integer, FileChange>> filesChanged) {
        if (source.isDirectory()) {
            String files[] = source.list();

            for (String file : files) {
                File srcFile = new File(source, file);
                if (!srcFile.getAbsolutePath().contains(".git")) {
                    File destFile = new File(destination, file);
                    copyNewFiles(srcFile, gitDirAux, destFile, filesAdded, filesChanged);
                }
            }
        } else {
            String sourceFileDir = source.getAbsolutePath().substring(source.getAbsolutePath().lastIndexOf(gitDirAux) + gitDirAux.length() + 1);
            if (filesAdded.contains(sourceFileDir) && !filesChanged.keySet().contains(sourceFileDir)) {
                try {
                    FileUtils.copyFile(source, destination);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}