package at.jku.isse.gitecco.translation;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.git.GitCommit;
import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.featureid.identification.ID;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import at.jku.isse.gitecco.translation.constraintcomputation.util.ConstraintComputer;
import at.jku.isse.gitecco.translation.mining.ChangeCharacteristic;
import at.jku.isse.gitecco.translation.mining.GetAllConditionalStatementsVisitor;
import at.jku.isse.gitecco.translation.visitor.GetNodesForChangeVisitor;
import com.opencsv.CSVWriter;
import org.glassfish.grizzly.http.server.accesslog.FileAppender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class MiningFeaturesLines {
    //to execute from command line type:
    //gradle run -Pmyargs='C:\Users\gabil\Desktop\PHD\Willian\libssh-mirror','C:\Users\gabil\Desktop\PHD\Willian\results2','c8a621c6063f205e631d83a8ccd4befc60056e93'

    private final static int EVERY_NTH_COMMIT = 1;
    private final static ArrayList<Feature> featureList = new ArrayList<>();
    private final static ArrayList<String> featureNamesList = new ArrayList<String>();
    private static String REPO_PATH = "/Users/willianmendonca/Documents/database/sqlite";
    private static String FEATURES_PATH = "/Users/willianmendonca/Documents/database/result";

//    private static String REPO_PATH = "";//""C:\\Users\\gabil\\Desktop\\PHD\\Willian\\libssh-mirror";
//    private static String FEATURES_PATH = "";//"C:\\Users\\gabil\\Desktop\\PHD\\Willian\\results2";
    static String commit = "b8bbe3e2db8b0cd12a955de6c10a441301b60e55";
    static String fileStoreConfig = "configurations.csv";
    static List<String> changedFiles = new ArrayList<>();
    static List<String> changedFilesNext = new ArrayList<>();
    static GitCommit gcPrevious = null;
    static Boolean previous = true;
    static List<String> configurations = new ArrayList<>();
    static Map<Feature, Integer> featureVersions = new HashMap<>();
    static String feats;


    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            REPO_PATH = args[0];
            FEATURES_PATH = args[1];
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
            commit = args[2];
            System.out.println("\u001B[32m" + "Mining process started. It can take minutes or hours...");
            identification();
            System.out.println("\u001B[32m" + "Process finished!!");
        } else {
            identification();
        }
    }


    public static void identification() throws Exception {
        String repoPath;
        repoPath = REPO_PATH;

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        GitCommitList commitList = new GitCommitList(gitHelper);

        gitHelper.getOneCommit(commitList, commit);
        File resultspath = new File(FEATURES_PATH);
        if (!resultspath.exists())
            resultspath.mkdir();
        File file = new File(FEATURES_PATH, commit);
        if (!file.exists())
            file.mkdir();
        final File filesFeatureFolder = new File(file, "FilesFeature");
        if (!filesFeatureFolder.exists())
            filesFeatureFolder.mkdir();
        //feature identification
        long measureIdentifyFeatures = System.currentTimeMillis();
        identifyFeatures(commitList);
        System.out.println("Time identify features: " + ((System.currentTimeMillis() - measureIdentifyFeatures) / 1000) + " seconds");
        initVars();
        for (GitCommit commits : commitList) {
            long measureLinesFeatures = System.currentTimeMillis();
            featuresFiles(filesFeatureFolder, commits.getNumber(), commits.getTree(), featureNamesList);
            System.out.println("Time identify lines per file for all features and files: " + ((System.currentTimeMillis() - measureLinesFeatures) / 1000) + " seconds");
            configurations.clear();
            long measureChangedFeatures = System.currentTimeMillis();
            characteristicsChange2(gitHelper, commits, featureNamesList);
            System.out.println("Time identify changed features: " + ((System.currentTimeMillis() - measureChangedFeatures) / 1000) + " seconds");
            //dispose tree if it is not needed -> for memory saving reasons.
            commits.disposeTree();
        }


        System.out.println("finished analyzing repo");
    }

    public static void initVars() {
        changedFiles.clear();
        changedFilesNext.clear();
        gcPrevious = null;
        previous = true;
        configurations.clear();
        featureVersions.clear();
    }

    public static void characteristicsChange2(GitHelper gitHelper, GitCommit gc, ArrayList<String> featureNamesList) throws Exception {

        final File gitFolder = new File(gitHelper.getPath());

        GetNodesForChangeVisitor visitor = new GetNodesForChangeVisitor();
        ArrayList<ConditionalNode> changedNodes = new ArrayList<>();
        ArrayList<ConditionalNode> deletedNodes = new ArrayList<>();
        Map<Feature, ChangeCharacteristic> featureMap = new HashMap<>();

        if (gc.getNumber() == Long.valueOf(0)) {
            gcPrevious = new GitCommit(gc.getCommitName(), gc.getNumber(), gc.getDiffCommitName(), gc.getBranch(), gc.getRevCommit());
            gcPrevious.setTree(gc.getTree());
        } else if (previous || gcPrevious != null) {
            if (gc.getNumber() - 1 < 1) {
                gcPrevious = new GitCommit(gc.getCommitName(), gc.getNumber(), gc.getDiffCommitName(), gc.getBranch(), gc.getRevCommit());
                gcPrevious.setTree(gc.getTree());
                previous = false;
            } else {
                gcPrevious = new GitCommit(gc.getRevCommit().getParent(0).getName(), gc.getNumber() - 1, gc.getRevCommit().getParent(0).getParent(0).getName(), gc.getBranch(), gc.getRevCommit().getParent(0));
                GitCommitList gcl = new GitCommitList(gitHelper);
                gcl.addTreeParent(gcPrevious, gc.getCommitName());
            }
        }

        Map<Change, FileNode> changesDelete = new HashMap<>();
        //retrieve changed nodes
        for (FileNode child : gc.getTree().getChildren()) {
            if (child instanceof SourceFileNode) {

                Change[] changes = null;
                try {
                    changes = gitHelper.getFileDiffs(gc, child, false);
                } catch (Exception e) {
                    System.err.println("error while executing the file diff: " + child.getFilePath());
                    e.printStackTrace();
                }

                for (Change change : changes) {
                    if (change.getChangeType().equals("INSERT")) {
                        visitor.setChange(change);
                        child.accept(visitor, null);
                        changedNodes.addAll(visitor.getchangedNodes());
                    } else if (change.getChangeType().equals("DELETE")) {
                        changesDelete.put(change, child);
                    } else if (change.getChangeType().equals("CHANGE")) {
                        visitor.setChange(change);
                        child.accept(visitor, null);
                        changedNodes.addAll(visitor.getchangedNodes());
                    }

                }
            }
            if (gc.getNumber() == 0) {
                changedFiles.add(child.getFilePath());
                previous = false;
            } else {
                changedFilesNext.add(child.getFilePath());
            }
        }

        if (previous) {
            for (FileNode child : gcPrevious.getTree().getChildren()) {
                String start = child.getFilePath().replace("arent" + File.separator, "");
                changedFiles.add(start);
                previous = false;
            }
        }

        if (gc.getNumber() == 0 || previous) {
            previous = false;
        } else {
            //to retrieve changed nodes of deleted files
            if (gcPrevious != null) {
                for (String file : changedFiles) {
                    if (!changedFilesNext.contains(file)) {
                        FileNode child = gcPrevious.getTree().getChild(file);
                        if (child instanceof SourceFileNode) {
                            Change[] changes = null;
                            try {
                                changes = gitHelper.getFileDiffs(gc, child, true);
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
                    FileNode child = gcPrevious.getTree().getChild(childAux.getFilePath());
                    visitor.setChange(change);
                    child.accept(visitor, null);
                    deletedNodes.addAll(visitor.getchangedNodes());
                }
            }
            //next is changedFiles for the next commit
            changedFiles.removeAll(changedFiles);
            changedFiles.addAll(changedFilesNext);
            changedFilesNext.removeAll(changedFilesNext);
        }


        final ConstraintComputer constraintComputer = new ConstraintComputer(featureNamesList);
        Map<Feature, Integer> config;
        Set<Feature> changed;
        Set<Feature> alreadyComitted = new HashSet<>();
        Integer count = 0;

        //if there is no changed node then there must be a change in the binary files --> commit base.
        if ((changedNodes.size() == 0) && (deletedNodes.size() == 0) && (gc.getTree().getChildren().size() > 0))
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

        ArrayList<String> configsToCommit = new ArrayList<>();
        Map<Map<Feature, Integer>, String> configsToGenerateVariant = new HashMap<>();
        Feature base = new Feature("BASE");
        for (ConditionalNode changedNode : changedNodes) {
            //compute the config for the var gen
            config = constraintComputer.computeConfig(changedNode, gc.getTree());
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                changed = constraintComputer.computeChangedFeatures(changedNode, config);
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
                            if (changed.contains(configFeature.getKey())) {
                                version++;
                            }
                            if (version == 0 && !(changed.contains(configFeature.getKey()))) {
                                version = 1;
                            }
                            featureVersions.put(configFeature.getKey(), version);
                        }
                        if (!configFeature.getKey().toString().equals("BASE"))
                            eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                        else
                            eccoConfig += "," + configFeature.getKey().toString() + ".$$";
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (!configurations.contains(eccoConfig)) {
                        count++;
                        //configuration that will be used to generate the variant of this changed node
                        configsToGenerateVariant.put(config, eccoConfig);
                        configurations.add(eccoConfig);
                    }
                }
            }
        }

        if (deletedNodes.size() != 0) {
            for (ConditionalNode deletedNode : deletedNodes) {
                //compute the config for the var gen
                config = constraintComputer.computeConfig(deletedNode, gcPrevious.getTree());
                if (config != null && !config.isEmpty()) {
                    //compute the marked as changed features.
                    changed = constraintComputer.computeChangedFeatures(deletedNode, config);
                    if (!changed.contains(base))
                        config.remove(base);
                    //map with the name of the feature and version and a boolean to set true when it is already incremented in the analysed commit
                    String eccoConfig = "";
                    for (Map.Entry<Feature, Integer> configFeature : config.entrySet()) {
                        int version = 0;
                        if (featureNamesList.contains(configFeature.getKey().getName()) && configFeature.getValue() != 0) {
                            if (featureVersions.containsKey(configFeature.getKey())) {
                                version = featureVersions.get(configFeature.getKey());
                            }
                            if (!alreadyComitted.contains(configFeature.getKey())) {
                                alreadyComitted.add(configFeature.getKey());
                                if (changed.contains(configFeature.getKey())) {
                                    version++;
                                }
                                featureVersions.put(configFeature.getKey(), version);
                            }
                            if (!configFeature.getKey().toString().equals("BASE"))
                                eccoConfig += "," + configFeature.getKey().toString() + "." + version;
                            else
                                eccoConfig += "," + configFeature.getKey().toString() + ".$$";
                        }
                    }
                    if (!eccoConfig.contains("BASE")) {
                        eccoConfig += "," + "BASE.$$";
                    }
                    eccoConfig = eccoConfig.replaceFirst(",", "");

                    if (!configurations.contains(eccoConfig)) {
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

            FileAppender csvWriter = new FileAppender(new File(FEATURES_PATH, fileStoreConfig));

            for (String configs : configsToCommit) {
                List<List<String>> headerRows = Arrays.asList(
                        Arrays.asList(Long.toString(gc.getNumber()), gc.getCommitName(), configs)
                );
                for (List<String> rowData : headerRows) {
                    csvWriter.append(String.join(",", rowData));
                }
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        changedNodes.clear();
        deletedNodes.clear();
        featureMap.clear();

    }

    public static void identifyFeatures(GitCommitList commitList) throws IOException {
        final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
        int count = 0;
        for (GitCommit commit : commitList) {
            ID.evaluateFeatureMap(evaluation, ID.id(commit.getTree()), commit.getNumber());
            //dispose tree if it is not needed -> for memory saving reasons.
            if (count == commitList.size() - 1) {
                writeToCsv(evaluation, commit.getCommitName());
            }
            count++;
        }
        evaluation.clear();

    }

    private static void writeToCsv(List<TraceableFeature> features, String fileName) throws IOException {

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

        }

        writerTXT.println(feats.substring(0, feats.length() - 1) + "}");
        writerTXT.close();
        System.out.println(feats.substring(0, feats.length() - 1) + "}");
        // closing writer connection
        writer.close();
    }


    public static void featuresFiles(File filesFeatureFolder, Long commitNr, RootNode tree, ArrayList<String> featureNamesList) {

        Map<Feature, ArrayList<String>> filesfeatureMap = new HashMap<>();
        GetAllConditionalStatementsVisitor visitor = new GetAllConditionalStatementsVisitor();
        Set<ConditionalNode> conditionalNodes = new HashSet<>();
        Set<ConditionalNode> negatedConditionalNodes = new HashSet<>();
        final ConstraintComputer constraintComputer = new ConstraintComputer(featureNamesList);
        Feature baseFeature = new Feature("BASE");
        for (FileNode child : tree.getChildren()) {
            if (child instanceof SourceFileNode) {
                int from = ((SourceFileNode) child).getBaseNode().getLineFrom();
                int to = ((SourceFileNode) child).getBaseNode().getLineTo();
                ArrayList<Integer> lines = new ArrayList<>();
                lines.add(from);
                lines.add(to);
                Change changes = new Change(from, to, lines, -1, null);
                visitor.setChange(changes);
                child.accept(visitor, null);
                conditionalNodes.addAll(visitor.getConditionalNodes());
                negatedConditionalNodes.addAll(visitor.getNegatedConditionalNodes());
                String file = child.getFilePath();
                int last = 0;
                ArrayList<String> files = new ArrayList<>();
                int i = 0;
                TreeMap<Integer, Integer> sortedMap = new TreeMap<Integer, Integer>(visitor.getLinesConditionalNodes());
                //sortedMap.entrySet().forEach(System.out::println);
                if (visitor.getLinesConditionalNodes().size() != 0) {
                    for (Map.Entry<Integer, Integer> linesBase : sortedMap.entrySet()) {
                        if (i == 0 && from < linesBase.getKey()) {
                            if (visitor.getLinesConditionalNodes().size() > 1)
                                last = linesBase.getKey();
                            else
                                last = linesBase.getValue();
                            i++;
                            //Lines per file per feature
                            if (filesfeatureMap.get(baseFeature) == null)
                                files = new ArrayList<>();
                            else
                                files = filesfeatureMap.get(baseFeature);
                            files.add(file + ":" + last + "-" + from);
                            filesfeatureMap.put(baseFeature, files);
                        } else {
                            if (i == 1) {
                                //Lines per file per feature
                                if (filesfeatureMap.get(baseFeature) == null)
                                    files = new ArrayList<>();
                                else
                                    files = filesfeatureMap.get(baseFeature);
                                if (visitor.getLinesConditionalNodes().size() > 1)
                                    last = linesBase.getKey();
                                else
                                    last = linesBase.getValue();
                                from = linesBase.getValue();
                                i++;
                            } else {
                                if (from >= last && from <= linesBase.getValue()) {
                                    files.add(file + ":" + linesBase.getValue() + "-" + from);
                                    filesfeatureMap.put(baseFeature, files);
                                }
                                if (visitor.getLinesConditionalNodes().size() > 1)
                                    last = linesBase.getKey();
                                else
                                    last = linesBase.getValue();
                                from = linesBase.getValue();

                                i++;
                            }
                        }
                    }
                    if (visitor.getLinesConditionalNodes().size() > 1 && from > last && from <= ((SourceFileNode) child).getBaseNode().getLineTo()) {
                        files.add(file + ":" + ((SourceFileNode) child).getBaseNode().getLineTo() + "-" + from);
                        filesfeatureMap.put(baseFeature, files);
                    }
                } else {
                    //Lines per file per feature
                    if (filesfeatureMap.get(baseFeature) == null)
                        files = new ArrayList<>();
                    else
                        files = filesfeatureMap.get(baseFeature);
                    files.add(file + ":" + ((SourceFileNode) child).getBaseNode().getLineTo() + "-" + from);
                    filesfeatureMap.put(baseFeature, files);
                }
            }
        }
        visitor.reset();
        Set<Feature> changed = new HashSet<>();
        for (ConditionalNode cNode : conditionalNodes) {
            int counttrio_minimal = 0;
            if (cNode.getCondition().contains("!TRIO_MINIMAL")) {
                String[] countTrioMininal = cNode.getCondition().split("&&");
                for (String string : countTrioMininal) {
                    if (string.contains("TRIO_MINIMAL") && !string.contains("!"))
                        counttrio_minimal++;
                }

            }
            if (counttrio_minimal <= 0 && !cNode.getCondition().contains("]")) {
                Map<Feature, Integer> config;
                config = constraintComputer.computeConfig(cNode, tree);
                changed = new HashSet<>();
                if (config != null && !config.isEmpty()) {
                    changed = constraintComputer.computeChangedFeatures(cNode, config);
                    if (!changed.contains(baseFeature))
                        config.remove(baseFeature);


                    for (Map.Entry<Feature, Integer> featsConditionalStatement : config.entrySet()) {
                        if (featsConditionalStatement.getValue() != 0 && featureNamesList.contains(featsConditionalStatement.getKey().getName())) {
                            String file = cNode.getParent().getParent().getContainingFile().getFilePath();
                            //Lines per file per feature
                            ArrayList<String> files;
                            if (filesfeatureMap.get(featsConditionalStatement.getKey()) == null)
                                files = new ArrayList<>();
                            else
                                files = filesfeatureMap.get(featsConditionalStatement.getKey());
                            files.add(file + ":" + cNode.getLineTo() + "-" + cNode.getLineFrom());
                            filesfeatureMap.put(featsConditionalStatement.getKey(), files);
                        }
                    }
                }
            }
        }
        for (ConditionalNode cNode : negatedConditionalNodes) {
            Map<Feature, Integer> config;
            config = constraintComputer.computeConfig(cNode, tree);
            changed = new HashSet<>();
            if (config != null && !config.isEmpty()) {
                //compute the marked as changed features.
                changed = constraintComputer.computeChangedFeatures(cNode, config);
                if (!changed.contains(baseFeature)) {
                    config.remove(baseFeature);
                }

                for (Map.Entry<Feature, Integer> feat : config.entrySet()) {
                    if (feat.getValue() != 0 && featureNamesList.contains(feat.getKey().getName())) {
                        changed.add(feat.getKey());
                    }
                }
                for (Feature featsConditionalStatement : changed) {
                    String file = cNode.getParent().getParent().getContainingFile().getFilePath();
                    //Lines per file per feature
                    ArrayList<String> files = new ArrayList<>();
                    if (filesfeatureMap.get(featsConditionalStatement) == null)
                        files = new ArrayList<>();
                    else
                        files = filesfeatureMap.get(featsConditionalStatement);
                    files.add(file + ":" + cNode.getLineTo() + "-" + cNode.getLineFrom());
                    filesfeatureMap.put(featsConditionalStatement, files);
                }
            }
        }
        File featureCSV = new File(filesFeatureFolder, "feature.csv");
        for (Map.Entry<Feature, ArrayList<String>> feat : filesfeatureMap.entrySet()) {
//            File featureCSV = new File(filesFeatureFolder, feat.getKey().getName() + ".csv");
            if (!featureCSV.exists()) {
                try {
                    FileWriter csvWriter = new FileWriter(featureCSV);
                    List<List<String>> headerRows = Arrays.asList(
                            Arrays.asList("Commit Nr","TargetFile","FeatureName", "FetFrom", "FetTo")
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
                for (String filefeature : feat.getValue()) {
                    String file = filefeature.substring(0, filefeature.indexOf(":"));
                    String to = filefeature.substring(filefeature.indexOf(":") + 1, filefeature.lastIndexOf("-"));
                    String featurename = feat.getKey().getName();
                    String from = filefeature.substring(filefeature.lastIndexOf("-") + 1);
                    List<List<String>> contentRows = Arrays.asList(
                            Arrays.asList(Long.toString(commitNr), file,featurename, from, to));
                    for (List<String> rowData : contentRows) {
                        csvAppender.append(String.join(",", rowData));
                    }
                }
                csvAppender.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}