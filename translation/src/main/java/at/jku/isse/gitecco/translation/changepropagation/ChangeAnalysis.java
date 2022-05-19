package at.jku.isse.gitecco.translation.changepropagation;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.mining.ChangeCharacteristic;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ChangeAnalysis {
    private static boolean EVERYCOMMIT = false;
    private static int EVERY_NTH_COMMIT = 1;
    private static ArrayList<Feature> featureList = new ArrayList<>();
    private static ArrayList<String> featureNamesList = new ArrayList<String>();
    //private static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\New research\\ChangePropagation\\runningexample";
    private static String FEATURES_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation";
    private static String FEATURES_TXT = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\runningexample.txt";//features-release-0-3-1.txt";
    static String fileReportFeature = "features_report_each_project_commit.csv";
    static String fileStoreConfig = "configurations.csv";
    static List<String> changedFiles = new ArrayList<>();
    static List<String> changedFilesNext = new ArrayList<>();
    static List<String> changedFilesPropagate = new ArrayList<>();
    static GitCommit gcPrevious = null;
    static Boolean previous = true;
    static List<String> configurations = new ArrayList<>();
    static Map<Feature, Integer> featureVersions = new HashMap<>();
    static String feats;
    static String analyze = "0";
    static String release = "";//"release-0-3-1";
    //private static String firstcommit = "a29e19a4557aa53f123767a5ae0284c01c79390d";//"918a912cd56dcac81feea2c52348cdc24b1468cf";
    //private static String secondcommit = "1d42a8d2bfa46c4f0874cdae2e9d8757e33b5da6";//"101bf21d414afab092caafcdb83cf035b0d8966b";
    //private static String featpropagatename = "featA";
    //private static Feature featpropagate = new Feature(featpropagatename);
    private static String mergeResult = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\merge";
    private static String backup = "C:\\Users\\gabil\\Desktop\\PHD\\ChangeAnalysis_Propagation\\ChangePropagation\\backup";


    public static Map<Map<String, List<String>>, Changes> identification(String featuresRepo, String REPO_PATH, String firstcommit, String secondcommit, String featpropagatename) throws Exception {
        if (REPO_PATH.contains("//")) {
            REPO_PATH.replaceAll("//", File.separator);
        }
        if (REPO_PATH.contains("\\\\")) {
            REPO_PATH.replaceAll("\\\\", File.separator);
        }
        if (FEATURES_PATH.contains("//")) {
            FEATURES_PATH.replaceAll("//", File.separator);
        }
        if (FEATURES_PATH.contains("\\\\")) {
            FEATURES_PATH.replaceAll("\\\\", File.separator);
        }
        if (featuresRepo.contains("//")) {
            featuresRepo.replaceAll("//", File.separator);
        }
        if (featuresRepo.contains("\\\\")) {
            featuresRepo.replaceAll("\\\\", File.separator);
        }
        FEATURES_TXT = featuresRepo;
        String[] featspropagate = featpropagatename.replaceAll(" ", "").split(",");
        ArrayList<Feature> featurespropagate = new ArrayList<>();
        for (String feature : featspropagate) {
            Feature featpropagate = new Feature(feature);
            featurespropagate.add(featpropagate);
        }
        // Feature featpropagate = new Feature(featpropagatename);
        Map<Map<String, List<String>>, Changes> results = new HashMap<>();

        long measure = System.currentTimeMillis();
        String repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);


        if (EVERYCOMMIT) {
            //gitHelper.getAllCommits(commitList);
        } else {
            /* Map<Long, String> mapTags = gitHelper.getCommitNumberTag();
           LinkedHashMap<Long, String> orderedMap = mapTags.entrySet() //
                    .stream() //
                    .sorted(Map.Entry.comparingByKey()) //
                    .collect(Collectors.toMap(Map.Entry::getKey, //
                            Map.Entry::getValue, //
                            (key, content) -> content, //
                            LinkedHashMap::new)); //
            */
            //if (mapTags.size() == 0) {
            gitHelper.getTwoCommits(commitList, firstcommit, secondcommit);
            File file = new File(FEATURES_PATH, "runningex");
            if (!file.exists())
                file.mkdir();
            String folderRelease = file.getAbsolutePath();
            final File changeFolder = new File(file, "ChangeCharacteristic");
            final File folder = new File(file, "FeatureCharacteristic");
            final File idFeatsfolder = new File(file, "IdentifiedFeatures");
            // if the directory does not exist, create it
            if (!changeFolder.exists())
                changeFolder.mkdir();
            if (!folder.exists())
                folder.mkdir();
            if (!idFeatsfolder.exists())
                idFeatsfolder.mkdir();
            //feature identification
            final File featuresTXT = new File(FEATURES_TXT);
            if (featuresTXT.exists())
                addFeatures();
            else
                identifyFeatures(commitList, "runningex", idFeatsfolder);
            //
            initVars(folderRelease);
            //for (GitCommit commits : commitList) {
            //ComputeRQMetrics.characteristicsFeature(folder, commits.getNumber(), commits.getTree(), featureNamesList);
            configurations.clear();
            characteristicsChange(gitHelper, changeFolder, commitList, featureNamesList, featurespropagate, results);
            //dispose tree if it is not needed -> for memory saving reasons.
            //commits.disposeTree();
            //}
            //RQ.2 How many times one feature changed along a number of Git commits?
            File filetxt = new File(folder.getParent(), "TimesEachFeatureChanged.txt");
            PrintWriter writerTXT = new PrintWriter(filetxt.getAbsolutePath(), "UTF-8");
            for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
                if (featureRevision.getValue() > 1)
                    writerTXT.println(featureRevision.getKey() + " Changed " + (featureRevision.getValue() - 1) + " times.");
            }
            writerTXT.close();
        }

        System.out.println("finished analyzing repo");
        return results;
    }

    public static void initVars(String folderRelease) {
        changedFiles.clear();
        changedFilesNext.clear();
        gcPrevious = null;
        previous = true;
        configurations.clear();
        featureVersions.clear();

        //csv to report new features and features changed per git commit of the project
        //RQ.2 How many features changed per Git commit?
        try {
            FileWriter csvWriter = new FileWriter(folderRelease + File.separator + fileReportFeature);
            List<List<String>> headerRows = Arrays.asList(
                    Arrays.asList("CommitNumber", "NewFeatures", "ChangedFeatures")
            );
            for (List<String> rowData : headerRows) {
                csvWriter.append(String.join(",", rowData));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        // end csv to report new features and features changed
    }

    public static void addFeatures() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(FEATURES_TXT));
            String line = "";
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] cols = line.split(",");
                if (!featureNamesList.contains(cols[0].substring(1).replace("\"", "")))
                    featureNamesList.add(cols[0].substring(1).replace("\"", ""));
                for (int i = 1; i < cols.length - 1; i++) {
                    if (!featureNamesList.contains(cols[i].replace("\"", "")))
                        featureNamesList.add(cols[i].replace("\"", ""));
                }
                String lastfeature = cols[cols.length - 1].replace("\"", "");
                if (!featureNamesList.contains(lastfeature.replace("}", "")))
                    featureNamesList.add(lastfeature.replace("}", ""));
                if (!featureNamesList.contains("BASE"))
                    featureNamesList.add("BASE");
            }
        } catch (IOException ex) {
            System.out.println((char) 27 + "[31m" + "Error reading the features text file!\nVerify if you wrote in the correct format -> {\"WITH_SERVER\",\"HAVE_LIBZ\",...}");
            System.exit(0);
        }
    }

    public static void identifyFeatures(GitCommitList commitList, String release, File idFeatFolder) throws IOException {
        final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
        String csvFile = release.substring(release.lastIndexOf("/") + 1);
        int count = 0;
        for (GitCommit commit : commitList) {
            System.out.println("for git identifyFeatures" + commit.getCommitName());
            at.jku.isse.gitecco.featureid.identification.ID.evaluateFeatureMap(evaluation, at.jku.isse.gitecco.featureid.identification.ID.id(commit.getTree()), commit.getNumber());
            //dispose tree if it is not needed -> for memory saving reasons.
            if (count == commitList.size() - 1) {
                //commits.disposeTree();
                writeToCsv(evaluation, csvFile, idFeatFolder);
                getFeatureDeletedTimes(idFeatFolder);
            } //else
            //commits.disposeTree();
            count++;
        }
        evaluation.clear();
    }

    private static void writeToCsv(List<TraceableFeature> features, String fileName, File idFeatsFolder) throws IOException {

        System.out.println("writing to CSV");
        FileWriter outputfile = null;
        File csvFile = new File(FEATURES_PATH, "features-" + fileName + ".csv");
        //second parameter is boolean for appending --> never append
        outputfile = new FileWriter(csvFile, false);
        feats = "{";

        // create CSVWriter object file writer object as parameter
        //deprecated but no other way available --> it still works anyways
        @SuppressWarnings("deprecation") CSVWriter writer = new CSVWriter(outputfile, ',', CSVWriter.NO_QUOTE_CHARACTER);

        //adding header to csv
        writer.writeNext(new String[]{"Label/FeatureName", "#total", "#external", "#internal", "#transient"});

        PrintWriter writerTXT = new PrintWriter(FEATURES_PATH + File.separator + "features-" + fileName + ".txt", "UTF-8");

        //write each feature/label with: Name, totalOcc, InternalOcc, externalOcc, transientOcc.
        for (TraceableFeature feature : features) {
            if (feature.getExternalOcc() == feature.getTotalOcc() || feature.getExternalOcc() == feature.getCommitList().size()) {
                feats += "\"" + feature.getName() + "\",";
                Feature feat = new Feature(feature.getName());
                featureList.add(feat);
                featureNamesList.add(feature.getName());
            }
            writer.writeNext(
                    new String[]{
                            feature.getName(),
                            feature.getTotalOcc().toString(),
                            feature.getExternalOcc().toString(),
                            feature.getInternalOcc().toString(),
                            feature.getTransientOcc().toString()
                    });
            FileWriter commitList = null;

            final File featureFile = new File(idFeatsFolder, feature.getName() + ".csv");
            commitList = new FileWriter(featureFile, false);
            CSVWriter writerFeature = new CSVWriter(commitList, ',', CSVWriter.NO_QUOTE_CHARACTER);
            writerFeature.writeNext(new String[]{"commitNumber", "Present"});
            for (Map.Entry<Long, Boolean> commit : feature.getCommitList().entrySet()) {
                writerFeature.writeNext(new String[]{String.valueOf(commit.getKey()), String.valueOf(commit.getValue())});
            }
            writerFeature.close();

        }

        writerTXT.println(feats.substring(0, feats.length() - 1) + "}");
        writerTXT.close();
        System.out.println(feats.substring(0, feats.length() - 1) + "}");
        // closing writer connection
        writer.close();
    }


    public static void getFeatureDeletedTimes(File featureFolder) throws IOException {
        File[] lista = featureFolder.listFiles();
        int deletedTimes = 0;
        Map<Integer, Integer> deletePerGitCommit = new HashMap<>();
        if (lista.length > 0) {
            for (File file : lista) {
                if (featureNamesList.contains(file.getName().substring(0, file.getName().indexOf(".csv")))) {
                    Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
                    CSVReader csvReader = new CSVReaderBuilder(reader).build();
                    List<String[]> lines = csvReader.readAll();
                    int count = 0;
                    for (int i = 1; i < lines.size(); i++) {
                        String[] line = lines.get(i);
                        String[] split = line[0].split(",");
                        if (i == 1) {
                            int first = Integer.valueOf(split[0]);
                            count = first;
                        } else {
                            int commitNumber = Integer.valueOf(split[0]);
                            if (commitNumber != (count + 1)) {
                                deletedTimes++;
                                Integer alreadyExist = deletePerGitCommit.computeIfPresent(commitNumber, (k, v) -> v + 1);
                                if (alreadyExist == null)
                                    deletePerGitCommit.put(commitNumber, 1);
                            }
                            count = commitNumber;
                        }

                    }
                    //RQ.2 How many times one feature were deleted along a number of Git commits?
                    File featureCSV = new File(featureFolder.getParent(), "deleteFeatures.csv");
                    if (!featureCSV.exists()) {
                        try {
                            FileWriter csvWriter = new FileWriter(featureCSV);
                            List<List<String>> headerRows = Arrays.asList(
                                    Arrays.asList("Feature", "Deleted Times")
                            );
                            for (List<String> rowData : headerRows) {
                                csvWriter.append(String.join(",", rowData));
                                csvWriter.append("\n");
                            }
                            csvWriter.flush();
                            csvWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        FileAppender csvAppender = new FileAppender(featureCSV);
                        List<List<String>> contentRows = Arrays.asList(
                                Arrays.asList(file.getName().substring(0, file.getName().indexOf(".csv")), String.valueOf(deletedTimes)));
                        for (List<String> rowData : contentRows) {
                            csvAppender.append(String.join(",", rowData));
                        }
                        csvAppender.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //System.out.println("Feature: " + file.getName() + " deleted times: " + deletedTimes);
                    deletedTimes = 0;
                }
            }

            File featureCSV = new File(featureFolder.getParent(), "delFeatsGitCommit.csv");
            if (!featureCSV.exists()) {
                try {
                    FileWriter csvWriter = new FileWriter(featureCSV);
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList("Commit Nr.", "Nr. Feat Del")
                    );
                    for (List<String> rowData : headerRows) {
                        csvWriter.append(String.join(",", rowData));
                        csvWriter.append("\n");
                    }
                    csvWriter.flush();
                    csvWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //RQ.2 How many features were deleted per Git commit?
            for (Map.Entry<Integer, Integer> commit : deletePerGitCommit.entrySet()) {
                try {
                    FileAppender csvAppender = new FileAppender(featureCSV);
                    List<List<String>> contentRows = Arrays.asList(
                            Arrays.asList(String.valueOf(commit.getKey()), String.valueOf(commit.getValue())));
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                    csvAppender.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("Commit Number: " + commit.getKey() + " features deleted: " + commit.getValue());
            }
        }
    }

    //RQ1
    public static void characteristicsChange(GitHelper gitHelper, File changeFolder, GitCommitList gcs, ArrayList<String> featureNamesList, ArrayList<Feature> featurespropagate, Map<Map<String, List<String>>, Changes> results) throws Exception {

        final File gitFolder = new File(gitHelper.getPath());
        final File eccoFolder = new File(gitFolder.getParent(), "ecco");

        Integer countFeaturesChanged = 0; //COUNT PER GIT COMMIT
        Integer newFeatures = 0; //COUNT PER GIT COMMIT

        GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
        ArrayList<ConditionalNode> changedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> newNodes = new ArrayList<>();
        ArrayList<ConditionalNode> deletedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> propagateChangedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> propagateDeletedNodes = new ArrayList<>();
        Map<Feature, ChangeCharacteristic> featureMap = new HashMap<>();
        GitCommit gc1 = gcs.get(0);
        GitCommit gc2 = gcs.get(1);

        int nrChunksInsert, nrChunksRemove;

        gcPrevious = new GitCommit(gc2.getCommitName(), gc2.getNumber(), gc1.getCommitName(), gc2.getBranch(), gc2.getRevCommit());
        gcPrevious.setTree(gc2.getTree());
        gc2 = gcPrevious;

        GitCommitList gcl = new GitCommitList(gitHelper);
        gcl.addTreeParent(gcPrevious, gc2.getCommitName());

        Map<Change, FileNode> changesDelete = new HashMap<>();
        Map<Change, FileNode> changesInsertAndDelete = new HashMap<>();


        for (FileNode nod : gc1.getTree().getChildren()) {
            if (nod instanceof SourceFileNode) {
                changedFiles.add(nod.getFilePath());
            }
        }

        for (FileNode nod : gc2.getTree().getChildren()) {
            if (nod instanceof SourceFileNode) {
                changedFilesNext.add(nod.getFilePath());
            }
        }

        //changed nodes of common and new files
        for (String file : changedFilesNext) {
            if (changedFiles.contains(file)) {//changed files common to both commit 1 and 2
                FileNode child = gc1.getTree().getChild(file);
                FileNode childCommit2 = gc2.getTree().getChild(file);
                if (child instanceof SourceFileNode) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc2, child,false, false);
                    } catch (Exception e) {
                        System.err.println("error while executing the file diff: " + child.getFilePath());
                        e.printStackTrace();
                    }
                    childCommit2.setPreviousFileContent(child.getPreviousFileContent());
                    childCommit2.setFileContent(child.getFileContent());
                    for (Change change : changes) {
                        visitor.setChange(change);
                        if(change.getChangeType().equals("CHANGE"))
                            child.accept(visitor, null);
                        else
                            childCommit2.accept(visitor,null);
                        changedNodes.addAll(visitor.getchangedNodes());
                    }
                }
            } else { //new files from commit2 not in commit1
                FileNode child = gc2.getTree().getChild(file);
                if (child instanceof SourceFileNode) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc2, child,false, true);
                    } catch (Exception e) {
                        System.err.println("error while executing the file diff: " + child.getFilePath());
                        e.printStackTrace();
                    }

                    for (Change change : changes) {
                        visitor.setChange(change);
                        child.accept(visitor, null);
                        newNodes.addAll(visitor.getchangedNodes());
                    }
                }
            }
        }

        //Deleted nodes in commit2 of files only in commit1
        for (String file : changedFiles) {
            if (!changedFilesNext.contains(file)) {
                FileNode child = gc1.getTree().getChild(file);
                if (child instanceof SourceFileNode) {
                    Change[] changes = null;
                    try {
                        changes = gitHelper.getFileDiffs(gc2, child, true, false);
                    } catch (Exception e) {
                        System.err.println("error while executing the file diff: " + child.getFilePath());
                        e.printStackTrace();
                    }

                    for (Change change : changes) {
                        visitor.setChange(change);
                        child.accept(visitor, null);
                        deletedNodes.addAll(visitor.getchangedNodes());
                    }
                }
            }
        }

        for (Map.Entry<Change, FileNode> changeInsert : changesDelete.entrySet()) {
            Change change = changeInsert.getKey();
            FileNode childAux = changeInsert.getValue();
            FileNode child = childAux;//gc1.getTree().getChild(childAux.getFilePath());
            visitor.setChange(change);
            child.accept(visitor, null);
            deletedNodes.addAll(visitor.getchangedNodes());
        }


        final ConstraintComputer constraintComputer = new ConstraintComputer(featureNamesList);
        final PreprocessorHelper pph = new PreprocessorHelper();
        Map<Feature, Integer> config;
        Set<Feature> changed;
        Set<Feature> alreadyComitted = new HashSet<>();
        Integer count = 0;

        //if there is no changed node then there must be a change in the binary files --> commit base.
        if ((changedNodes.size() == 0) && (deletedNodes.size() == 0) && (newNodes.size() == 0) && (gc2.getTree().getChildren().size() > 0))
            changedNodes.add(new BaseNode(null, 0));

        Boolean baseChanged = false;
        for (ConditionalNode nods : changedNodes) {
            if (nods instanceof BaseNode) {
                baseChanged = true;
            }
        }
        if (!baseChanged) {
            for (ConditionalNode nods : deletedNodes) {
                if (nods instanceof BaseNode) {
                    baseChanged = true;
                }
            }
        }

        ArrayList<ConditionalNode> changedNodesAux = new ArrayList<>();
        for (ConditionalNode changedNode : changedNodes) {
            if (!changedNodesAux.contains(changedNode))
                changedNodesAux.add(changedNode);
        }

        ArrayList<ConditionalNode> newNodesAux = new ArrayList<>();
        for (ConditionalNode changedNode : newNodes) {
            if (!newNodesAux.contains(changedNode))
                newNodesAux.add(changedNode);
        }

        ArrayList<ConditionalNode> deletedNodesAux = new ArrayList<>();
        for (ConditionalNode deletedNode : deletedNodes) {
            if (!deletedNodesAux.contains(deletedNode))
                deletedNodesAux.add(deletedNode);
        }

        ArrayList<String> configsToCommit = new ArrayList<>();
        Map<Map<Feature, Integer>, String> configsToGenerateVariant = new HashMap<>();
        Feature base = new Feature("BASE");
        //changedNodes = changedNodes.stream().filter(x -> x.getLocalCondition().equals("__AVR_ATmega644P__ || __AVR_ATmega644__")).collect(Collectors.toSet());
        for (ConditionalNode changedNode : changedNodesAux) {
            //compute the config for the var gen
            config = constraintComputer.computeConfig(changedNode, gc2.getTree());
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                changed = constraintComputer.computeChangedFeatures(changedNode, config);
                for (Feature feature : featurespropagate) {
                    if (changed.contains(feature) && !propagateChangedNodes.contains(changedNode)) {
                        //if (changed.contains(featpropagate)) {
                        propagateChangedNodes.add(changedNode);
                        System.out.println("CHANGED NODE CONTAINS FEATURE: " + feature.getName());
                        ArrayList<Integer> linesInsert = new ArrayList<>();
                        ArrayList<Integer> linesRemoved = new ArrayList<>();
                        ArrayList<String> featureInteraction = new ArrayList<>();
                        ArrayList<String> featureMightBeAffected = new ArrayList<>();
                        if (!changedFiles.contains(changedNode.getContainingFile().getFilePath())) {
                            System.out.println("NEW FILE: " + changedNode.getContainingFile().getFilePath());
                            //linesInsert.addAll(changedNode.getLineNumberInserts());
                            //System.out.println("Line numbers insert: " + changedNode.getLineNumberInserts());
                            if (!changedFilesPropagate.contains(changedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(changedNode.getContainingFile().getFilePath());
                        } else {
                            if (!changedFilesPropagate.contains(changedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(changedNode.getContainingFile().getFilePath());
                            System.out.println("CHANGED FILE: " + changedNode.getContainingFile().getFilePath());
                            //linesInsert.addAll(changedNode.getLineNumberInserts());
                            //linesRemoved.addAll(changedNode.getLineNumberDeleted());
                            //System.out.println("Line numbers insert: " + changedNode.getLineNumberInserts() + " Line numbers removed: " + changedNode.getLineNumberDeleted());
                        }

                        System.out.println("Feature interactions: ");
                        for (Feature fea : changed) {
                            if (!fea.equals(feature)) {
                                featureInteraction.add(fea.getName());
                                System.out.println(fea.getName());
                            }
                        }


                        System.out.println("Feature might be affected: ");
                        for (Map.Entry<Feature, Integer> fea : config.entrySet()) {
                            if (!changed.contains(fea.getKey())) {
                                featureMightBeAffected.add(fea.getKey().getName());
                                System.out.println(fea.getKey().getName());
                            }
                        }

                        List<String> linesFile = changedNode.getContainingFileLines();

                        Boolean mapExist = false;
                        Changes mapAux = new Changes();

                        for (Map<String, List<String>> map : results.keySet()) {
                            if (map.get(changedNode.getContainingFile().getFilePath()) != null) {
                                mapExist = true;
                                mapAux = results.get(map);
                            }
                        }

                        //new file
                        if (!changedFiles.contains(changedNode.getContainingFile().getFilePath())) {
                            Map<String, List<String>> mapFile = new HashMap<>();
                            for (Deltas delta : changedNode.getLineNumberInsertsAndDeletes()) {
                                ArrayList<Integer> linesInsertArray = new ArrayList<>();
                                linesInsertArray.add(delta.getLineFromAdd());
                                linesInsertArray.add(delta.getLineToAdd());
                                linesInsertArray.add(delta.getTotalLinesAdd());
                                AddedFile addedFile = new AddedFile(linesInsertArray, featureInteraction, featureMightBeAffected, feature);
                                mapAux.addAddedFiles(addedFile);
                            }
                            mapFile.put(changedNode.getContainingFile().getFilePath(), linesFile);
                            results.put(mapFile, mapAux);

                        } else { //changed file
                            for (Deltas delta : changedNode.getLineNumberInsertsAndDeletes()) {
                                ArrayList<Integer> linesInsertArray = new ArrayList<>();
                                ArrayList<Integer> linesRemovedArray = new ArrayList<>();
                                linesInsertArray.add(delta.getLineFromAdd());
                                linesInsertArray.add(delta.getLineToAdd());
                                linesInsertArray.add(delta.getTotalLinesAdd());
                                if (delta.getLineFromRemov() != -1) {
                                    linesRemovedArray.add(delta.getLineFromRemov());
                                    linesRemovedArray.add(delta.getLineToRemov());
                                    linesRemovedArray.add(delta.getTotalLinesRemov());
                                }
                                int position = delta.getPosLinesAdd();
                                if (position == -1)
                                    position = delta.getPosLinesRemov();
                                ChangedFile changedFile = new ChangedFile(linesInsertArray, linesRemovedArray, position, featureInteraction, featureMightBeAffected, changedNode.getContainingFileLines(), changedNode.getContainingPreviousFileLines(), feature);
                                changedFile.setPreviousLines(changedNode.getContainingPreviousFileLines());
                                mapAux.addChangedFiles(changedFile);
                            }
                            Map<String, List<String>> mapFile = new HashMap<>();
                            mapFile.put(changedNode.getContainingFile().getFilePath(), linesFile);
                            results.put(mapFile, mapAux);
                        }

                    }
                }
                //int tanglingDegree = 0;
                //for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                //    if (featureNamesList.contains(feat.getKey().getName()))
                //        tanglingDegree++;
                //}
                if (!changed.contains(base))
                    config.remove(base);
                //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                String eccoConfig = "";
                String file = "";
                if (changedNode.getContainingFile() != null)
                    file = changedNode.getContainingFile().getFilePath();
                for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                    int version = 0;
                    if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                        if (featureVersions.containsKey(configFeature.getKey())) {
                            version = featureVersions.get(configFeature.getKey());
                        }
                        if (!alreadyComitted.contains(configFeature.getKey())) {
                            alreadyComitted.add(configFeature.getKey());
                            //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                            if (changed.contains(configFeature.getKey())) { //|| gcl.size() < 2) {
                                version++;
                                if (version == 1)
                                    newFeatures++;
                                else
                                    countFeaturesChanged++;
                            }
                            if (version == 0 && !(changed.contains(configFeature.getKey()))) {
                                newFeatures++;
                                version = 1;
                            }
                            featureVersions.put(configFeature.getKey(), version);
                        }
                        if (!configFeature.getKey().toString().equals("BASE"))
                            eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                        else
                            eccoConfig += "," + configFeature.getKey().toString() + ".$$";


                       /* ChangeCharacteristic changeCharacteristic;
                        if (featureMap.get(configFeature.getKey()) == null) {
                            changeCharacteristic = new ChangeCharacteristic();
                            featureMap.put(configFeature.getKey(), new ChangeCharacteristic());
                        } else {
                            changeCharacteristic = featureMap.get(configFeature.getKey());
                        }
                        int aux = 0;*/
                        /*if (changedNode.getLineNumberInserts().size() > 0) {
                            for (int i = 2; i < changedNode.getLineNumberInserts().size(); i += 3) {
                                aux += changedNode.getLineNumberInserts().get(i);
                            }
                            changeCharacteristic.setLinesOfCodeAdded(changeCharacteristic.getLinesOfCodeAdded() + aux);
                            changedNode.getLineNumberInserts().removeAll(changedNode.getLineNumberInserts());
                        }

                        changeCharacteristic.addTanglingDegree(tanglingDegree);
                        if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                            changeCharacteristic.addScatteringDegreeFiles(file);
                        }
                        if (!(changedNode instanceof BaseNode))
                            changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                        ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                        featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                        featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
                    }*/
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (configurations.contains(eccoConfig)) {
                        //System.out.println("Config already used to generate a variant: " + eccoConfig);
                        //don't need to generate variant and commit it again at the same commit of the project git repository
                    } else {
                        count++;
                        //configuration that will be used to generate the variant of this changed node
                        configsToGenerateVariant.put(config, eccoConfig);

                        configurations.add(eccoConfig);
                        //folder where the variant is stored
                        File variantsrc = new File(eccoFolder, eccoConfig);
                        String outputCSV = variantsrc.getParentFile().getParentFile().getAbsolutePath();
                        final Path variant_dir = Paths.get(String.valueOf(variantsrc));

                    }
                }
            }

        }

        //---------

        for (ConditionalNode changedNode : newNodesAux) {
            //compute the config for the var gen
            config = constraintComputer.computeConfig(changedNode, gc2.getTree());
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                changed = constraintComputer.computeChangedFeatures(changedNode, config);
                for (Feature feature : featurespropagate) {
                    if (changed.contains(feature) && !propagateChangedNodes.contains(changedNode)) {
                        propagateChangedNodes.add(changedNode);
                        System.out.println("CHANGED NODE CONTAINS FEATURE: " + feature.getName());
                        ArrayList<Integer> linesInsert = new ArrayList<>();
                        ArrayList<Integer> linesRemoved = new ArrayList<>();
                        ArrayList<String> featureInteraction = new ArrayList<>();
                        ArrayList<String> featureMightBeAffected = new ArrayList<>();
                        if (!changedFiles.contains(changedNode.getContainingFile().getFilePath())) {
                            System.out.println("NEW FILE: " + changedNode.getContainingFile().getFilePath());
                            if (!changedFilesPropagate.contains(changedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(changedNode.getContainingFile().getFilePath());
                        } else {
                            if (!changedFilesPropagate.contains(changedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(changedNode.getContainingFile().getFilePath());
                            System.out.println("CHANGED FILE: " + changedNode.getContainingFile().getFilePath());
                        }

                        System.out.println("Feature interactions: ");
                        for (Feature fea : changed) {
                            if (!fea.equals(feature)) {
                                featureInteraction.add(fea.getName());
                                System.out.println(fea.getName());
                            }
                        }


                        System.out.println("Feature might be affected: ");
                        for (Map.Entry<Feature, Integer> fea : config.entrySet()) {
                            if (!changed.contains(fea.getKey())) {
                                featureMightBeAffected.add(fea.getKey().getName());
                                System.out.println(fea.getKey().getName());
                            }
                        }

                        List<String> linesFile = changedNode.getContainingFileLines();

                        Boolean mapExist = false;
                        Changes mapAux = new Changes();

                        for (Map<String, List<String>> map : results.keySet()) {
                            if (map.get(changedNode.getContainingFile().getFilePath()) != null) {
                                mapExist = true;
                                mapAux = results.get(map);
                            }
                        }

                        //new file
                        Map<String, List<String>> mapFile = new HashMap<>();
                        for (Deltas delta : changedNode.getLineNumberInsertsAndDeletes()) {
                            ArrayList<Integer> linesInsertArray = new ArrayList<>();
                            linesInsertArray.add(delta.getLineFromAdd());
                            linesInsertArray.add(delta.getLineToAdd());
                            linesInsertArray.add(delta.getTotalLinesAdd());
                            AddedFile addedFile = new AddedFile(linesInsertArray, featureInteraction, featureMightBeAffected, feature);
                            mapAux.addAddedFiles(addedFile);
                        }
                        mapFile.put(changedNode.getContainingFile().getFilePath(), linesFile);
                        results.put(mapFile, mapAux);
                    }

                }

                if (!changed.contains(base))
                    config.remove(base);
                //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                String eccoConfig = "";
                String file = "";
                if (changedNode.getContainingFile() != null)
                    file = changedNode.getContainingFile().getFilePath();
                for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                    int version = 0;
                    if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                        if (featureVersions.containsKey(configFeature.getKey())) {
                            version = featureVersions.get(configFeature.getKey());
                        }
                        if (!alreadyComitted.contains(configFeature.getKey())) {
                            alreadyComitted.add(configFeature.getKey());
                            if (changed.contains(configFeature.getKey())) { //|| gcl.size() < 2) {
                                version++;
                                if (version == 1)
                                    newFeatures++;
                                else
                                    countFeaturesChanged++;
                            }
                            if (version == 0 && !(changed.contains(configFeature.getKey()))) {
                                newFeatures++;
                                version = 1;
                            }
                            featureVersions.put(configFeature.getKey(), version);
                        }
                        if (!configFeature.getKey().toString().equals("BASE"))
                            eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                        else
                            eccoConfig += "," + configFeature.getKey().toString() + ".$$";

                       /* ChangeCharacteristic changeCharacteristic;
                        if (featureMap.get(configFeature.getKey()) == null) {
                            changeCharacteristic = new ChangeCharacteristic();
                            featureMap.put(configFeature.getKey(), new ChangeCharacteristic());
                        } else {
                            changeCharacteristic = featureMap.get(configFeature.getKey());
                        }
                        int aux = 0;*/
                        /*if (changedNode.getLineNumberInserts().size() > 0) {
                            for (int i = 2; i < changedNode.getLineNumberInserts().size(); i += 3) {
                                aux += changedNode.getLineNumberInserts().get(i);
                            }
                            changeCharacteristic.setLinesOfCodeAdded(changeCharacteristic.getLinesOfCodeAdded() + aux);
                            changedNode.getLineNumberInserts().removeAll(changedNode.getLineNumberInserts());
                        }

                        changeCharacteristic.addTanglingDegree(tanglingDegree);
                        if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                            changeCharacteristic.addScatteringDegreeFiles(file);
                        }
                        if (!(changedNode instanceof BaseNode))
                            changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                        ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                        featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                        featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
                    }*/
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (configurations.contains(eccoConfig)) {
                        //System.out.println("Config already used to generate a variant: " + eccoConfig);
                        //don't need to generate variant and commit it again at the same commit of the project git repository
                    } else {
                        count++;
                        //configuration that will be used to generate the variant of this changed node
                        configsToGenerateVariant.put(config, eccoConfig);

                        configurations.add(eccoConfig);
                        //folder where the variant is stored
                        File variantsrc = new File(eccoFolder, eccoConfig);
                        String outputCSV = variantsrc.getParentFile().getParentFile().getAbsolutePath();
                        final Path variant_dir = Paths.get(String.valueOf(variantsrc));

                    }
                }
            }

        }

        //----------
        if (deletedNodesAux.size() != 0) {
            String file = "";
            for (ConditionalNode deletedNode : deletedNodesAux) {
                file = deletedNode.getContainingFile().getFilePath();
                //compute the config for the var gen
                config = constraintComputer.computeConfig(deletedNode, gcPrevious.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(deletedNode, config);
                    for (Feature feature : featurespropagate) {
                        if (changed.contains(feature)) {
                            //if (changed.contains(featpropagate)) {
                            propagateDeletedNodes.add(deletedNode);
                            System.out.println("DELETED NODE CONTAINS FEATURE: " + feature.getName());
                            ArrayList<String> featureInteraction = new ArrayList<>();
                            ArrayList<String> featureMightBeAffected = new ArrayList<>();
                            ArrayList<Integer> linesRemoved = new ArrayList<>();
                            if (!changedFilesPropagate.contains(deletedNode.getContainingFile().getFilePath()))
                                changedFilesPropagate.add(deletedNode.getContainingFile().getFilePath());
                            if (!changedFiles.contains(deletedNode.getContainingFile().getFilePath())) {
                                System.out.println("NEW FILE: " + deletedNode.getContainingFile().getFilePath());
                            } else if (!changedFilesNext.contains(deletedNode.getContainingFile().getFilePath())) {
                                System.out.println("DELETED FILE: " + deletedNode.getContainingFile().getFilePath());
                            } else {
                                System.out.println("CHANGED FILE: " + deletedNode.getContainingFile().getFilePath());
                            }

                            System.out.println("FILE: " + deletedNode.getContainingFile().getFilePath());
                            //System.out.println("lines removed " + deletedNode.getLineNumberDeleted());
                            System.out.println("Feature interactions: ");
                            //linesRemoved.addAll(deletedNode.getLineNumberDeleted());
                            for (Feature fea : changed) {
                                if (!fea.equals(feature)) {
                                    featureInteraction.add(fea.getName());
                                    System.out.println(fea.getName());
                                }
                            }

                            System.out.println("Feature might be affected: ");
                            for (Map.Entry<Feature, Integer> fea : config.entrySet()) {
                                if (!changed.contains(fea.getKey())) {
                                    featureMightBeAffected.add(fea.getKey().getName());
                                    System.out.println(fea.getKey().getName());
                                }
                            }
                            List<String> linesFile = deletedNode.getContainingFileLines();
                            if (deletedNode.getContainingFile().getPreviousFileContent().size() == 0 && deletedNode.getContainingFile().getFileContent().size() == 0) {
                                for (ConditionalNode changedNode : changedNodesAux) {
                                    if (changedNode.getContainingFile().getFilePath().equals(deletedNode.getContainingFile().getFilePath()))
                                        linesFile = changedNode.getContainingFile().getPreviousFileContent();
                                }
                            } else {
                                linesFile = deletedNode.getContainingFileLines();//deletedNode.getContainingPreviousFileLines();
                            }


                            Boolean mapExist = false;
                            Changes mapAux = new Changes();

                            for (Map<String, List<String>> map : results.keySet()) {
                                if (map.get(deletedNode.getContainingFile().getFilePath()) != null) {
                                    mapExist = true;
                                    mapAux = results.get(map);
                                }
                            }

                            //new file
                            if (!changedFiles.contains(deletedNode.getContainingFile().getFilePath())) {
                                for (Deltas delta : deletedNode.getLineNumberInsertsAndDeletes()) {
                                    ArrayList<Integer> linesRemoveArray = new ArrayList<>();
                                    linesRemoveArray.add(delta.getLineFromRemov());
                                    linesRemoveArray.add(delta.getLineToRemov());
                                    linesRemoveArray.add(delta.getTotalLinesRemov());
                                    AddedFile addedFile = new AddedFile(linesRemoveArray, featureInteraction, featureMightBeAffected, feature);
                                    mapAux.addAddedFiles(addedFile);
                                }
                                Map<String, List<String>> mapFile = new HashMap<>();
                                mapFile.put(deletedNode.getContainingFile().getFilePath(), linesFile);
                                results.put(mapFile, mapAux);
                            } else if (!changedFilesNext.contains(deletedNode.getContainingFile().getFilePath())) { //deleted file
                                for (Deltas delta : deletedNode.getLineNumberInsertsAndDeletes()) {
                                    ArrayList<Integer> linesRemoveArray = new ArrayList<>();
                                    linesRemoveArray.add(delta.getLineFromRemov());
                                    linesRemoveArray.add(delta.getLineToRemov());
                                    linesRemoveArray.add(delta.getTotalLinesRemov());
                                    DeletedFile deletedFile = new DeletedFile(linesRemoveArray, featureInteraction, featureMightBeAffected, feature);
                                    mapAux.addDeletedFiles(deletedFile);
                                }
                                Map<String, List<String>> mapFile = new HashMap<>();
                                mapFile.put(deletedNode.getContainingFile().getFilePath(), linesFile);
                                results.put(mapFile, mapAux);
                            } else {//changed file
                                for (Deltas delta : deletedNode.getLineNumberInsertsAndDeletes()) {
                                    ArrayList<Integer> linesInsertArray = new ArrayList<>();
                                    ArrayList<Integer> linesRemoveArray = new ArrayList<>();
                                    linesInsertArray.add(delta.getLineFromAdd());
                                    linesInsertArray.add(delta.getLineToAdd());
                                    linesInsertArray.add(delta.getTotalLinesAdd());
                                    int position = delta.getPosLinesAdd();
                                    if (position == -1)
                                        position = delta.getPosLinesRemov();
                                    linesRemoveArray.add(delta.getLineFromRemov());
                                    linesRemoveArray.add(delta.getLineToRemov());
                                    linesRemoveArray.add(delta.getTotalLinesRemov());
                                    ChangedFile changedFile = new ChangedFile(linesInsertArray, linesRemoveArray, position, featureInteraction, featureMightBeAffected, deletedNode.getContainingFileLines(), deletedNode.getContainingPreviousFileLines(), feature);
                                    mapAux.addChangedFiles(changedFile);
                                }
                                Map<String, List<String>> mapFile = new HashMap<>();
                                mapFile.put(deletedNode.getContainingFile().getFilePath(), linesFile);
                                results.put(mapFile, mapAux);
                            }


                        }
                    }
                    int tanglingDegree = 0;
                    for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                        if (featureNamesList.contains(feat.getKey().getName()))
                            tanglingDegree++;
                    }
                    if (!changed.contains(base))
                        config.remove(base);
                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit*/
                    String eccoConfig = "";
                    for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                        int version = 0;
                        if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                            if (featureVersions.containsKey(configFeature.getKey())) {
                                version = featureVersions.get(configFeature.getKey());
                            }
                            if (!alreadyComitted.contains(configFeature.getKey())) {
                                alreadyComitted.add(configFeature.getKey());
                                //for marlin: first commit is empty, thus we need the < 2. otherwise <1 should be enough.
                                if (changed.contains(configFeature.getKey())) { //gcl.size() < 2 ||
                                    version++;
                                    if (version == 1)
                                        newFeatures++;
                                    else
                                        countFeaturesChanged++;
                                }
                                featureVersions.put(configFeature.getKey(), version);
                            }
                            if (!configFeature.getKey().toString().equals("BASE"))
                                eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                            else
                                eccoConfig += "," + configFeature.getKey().toString() + ".$$";

                            /*
                            //RQ1. deleted lines
                            ChangeCharacteristic changeCharacteristic = featureMap.get(configFeature.getKey());
                            if (changeCharacteristic == null)
                                changeCharacteristic = new ChangeCharacteristic();
                            int aux = 0;
                            if (deletedNode.getLineNumberDeleted().size() > 0) {
                                for (int i = 2; i < deletedNode.getLineNumberDeleted().size(); i += 3) {
                                    aux += deletedNode.getLineNumberDeleted().get(i);
                                }
                                changeCharacteristic.setLinesOfCodeRemoved(changeCharacteristic.getLinesOfCodeRemoved() + aux);
                                deletedNode.getLineNumberDeleted().removeAll(deletedNode.getLineNumberDeleted());
                            }
                            changeCharacteristic.addTanglingDegree(tanglingDegree);
                            if (!changeCharacteristic.getScatteringDegreeFiles().contains(file)) {
                                changeCharacteristic.addScatteringDegreeFiles(file);
                            }
                            if (!(deletedNode instanceof BaseNode))
                                changeCharacteristic.setScatteringDegreeIfs(changeCharacteristic.getScatteringDegreeIfs() + 1);
                            ChangeCharacteristic finalChangeCharacteristic = changeCharacteristic;
                            featureMap.computeIfAbsent(configFeature.getKey(), v -> finalChangeCharacteristic);
                            featureMap.computeIfPresent(configFeature.getKey(), (k, v) -> finalChangeCharacteristic);
                        }*/
                        }
                        if (!eccoConfig.contains("BASE")) {
                            eccoConfig += "," + "BASE.$$";
                        }
                        eccoConfig = eccoConfig.replaceFirst(",", "");

                        if (configurations.contains(eccoConfig)) {
                            //System.out.println("Config already used to generate a variant: " + eccoConfig);
                            //don't need to generate variant and commit it again at the same commit of the project git repository
                        } else {
                            count++;
                            //configuration that will be used to generate the variant of this changed node
                            configsToGenerateVariant.put(config, eccoConfig);

                            configurations.add(eccoConfig);


                        }
                    }
                }
            }

            String baseVersion = "";
            for (Map.Entry<Feature, Integer> configFeature : featureVersions.entrySet()) {
                if (configFeature.getKey().equals(base))
                    baseVersion = configFeature.getValue().toString();
            }

            //generate the variant for this config
            for (Map.Entry<Map<Feature, Integer>, String> variant : configsToGenerateVariant.entrySet()) {
                String eccoConfig = variant.getValue().replace("$$", baseVersion);
                //config that will be used to commit the variant generated with this changed node in ecco
                configsToCommit.add(eccoConfig);
            }


            //appending to the config csv
            try {

                FileAppender csvWriter = new FileAppender(new File(changeFolder.getParent(), fileStoreConfig));

                for (String configs : configsToCommit) {
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList(Long.toString(gc2.getNumber()), gc2.getCommitName(), configs)
                    );
                    for (List<String> rowData : headerRows) {
                        csvWriter.append(String.join(",", rowData));
                    }
                }
                csvWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            //append results to the feature report csv
            try {
                FileAppender csvAppender = new FileAppender(new File(changeFolder.getParent(), fileReportFeature));
                List<List<String>> contentRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc2.getNumber()), newFeatures.toString(), countFeaturesChanged.toString())
                );
                for (List<String> rowData : contentRows) {
                    csvAppender.append(String.join(",", rowData));
                }
                csvAppender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //end append results to the feature report csv

            for (Map.Entry<Feature, Integer> featureRevision : featureVersions.entrySet()) {
                System.out.println(featureRevision.getKey() + "." + featureRevision.getValue());
            }


            for (Map.Entry<Feature, ChangeCharacteristic> changes : featureMap.entrySet()) {
                ChangeCharacteristic changeCharacteristic = changes.getValue();
                Collections.sort(changeCharacteristic.getTanglingDegree());
                File featureCSV = new File(changeFolder, changes.getKey().getName() + ".csv");
                if (!featureCSV.exists()) {
                    try {
                        FileWriter csvWriter = new FileWriter(featureCSV);
                        List<List<String>> headerRows = Arrays.asList(
                                Arrays.asList("Commit Nr", "LOC A", "LOC R", "SD IF", "SD File", "TD IF")
                        );
                        for (List<String> rowData : headerRows) {
                            csvWriter.append(String.join(",", rowData));
                            csvWriter.append("\n");
                        }
                        csvWriter.flush();
                        csvWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    FileAppender csvAppender = new FileAppender(featureCSV);
                    int tangling = 0;
                    if (changeCharacteristic.getTanglingDegree().size() > 0)
                        tangling = changeCharacteristic.getTanglingDegree().get(changeCharacteristic.getTanglingDegree().size() - 1);
                    List<List<String>> contentRows = Arrays.asList(
                            Arrays.asList(Long.toString(gc2.getNumber()), String.valueOf(changeCharacteristic.getLinesOfCodeAdded()), String.valueOf(changeCharacteristic.getLinesOfCodeRemoved()), String.valueOf(changeCharacteristic.getScatteringDegreeIfs()), String.valueOf(changeCharacteristic.getScatteringDegreeFiles().size()),
                                    String.valueOf(tangling)));
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                    csvAppender.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            changedNodes.clear();
            newNodes.clear();
            deletedNodes.clear();
            deletedNodesAux.clear();
            changedNodesAux.clear();
            featureMap.clear();

        }
    }



   /* public static void addChange(Changes changes, ArrayList<String> featureInteraction, ArrayList<String> featureMightBeAffected, ArrayList<Integer> linesRemArray, String type, Map<Map<String, List<String>>, Changes> results, ConditionalNode changedNode) {
        if (type.equals("add")) {
            AddedFile addedFile = new AddedFile(linesAddArray, featureInteraction, featureMightBeAffected);

            if (!changes.getAddedFiles().contains(addedFile)) {
                changes.addAddedFiles(addedFile);
                Map<String, List<String>> mapFile = new HashMap<>();
                for (Map<String, List<String>> mapresults : results.keySet()) {
                    if (mapresults.keySet().contains(changedNode.getContainingFile().getFilePath())) {
                        mapFile = mapresults;
                    }
                }
                results.computeIfPresent(mapFile, (k, v) -> changes);
            }
        } else if (type.equals("change")) {
            ChangedFile changedFile = new ChangedFile(linesAddArray, linesRemArray, changedNode.getLineNumberOldPosition(), featureInteraction, featureMightBeAffected, changedNode.getContainingFileLines(), changedNode.getContainingPreviousFileLines());
            if (!changes.getChangedFiles().contains(changedFile)) {
                changes.addChangedFiles(changedFile);
                Map<String, List<String>> mapFile = new HashMap<>();
                for (Map<String, List<String>> mapresults : results.keySet()) {
                    if (mapresults.keySet().contains(changedNode.getContainingFile().getFilePath())) {
                        mapFile = mapresults;
                    }
                }
                results.computeIfPresent(mapFile, (k, v) -> changes);
            }
        }

    }*/


}
