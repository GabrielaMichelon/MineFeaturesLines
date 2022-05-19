package at.jku.isse.gitecco.featureid;


import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.featureid.identification.ID;
import at.jku.isse.gitecco.featureid.type.TraceableFeature;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class App extends Thread{

    //private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\test-featureid";
    //private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\TestMarlin\\Marlin\\Marlin\\Marlin";
    private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\Sqlite - Copy\\sqlite";
    //private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\sqlite";
    //private final static String REPO_PATH = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\libssh-mirror\\libssh-mirror";
    // "C:\\obermanndavid\\git-to-ecco\\test_repo5"
    // "C:\\obermanndavid\\git-ecco-test\\test_featureid\\betaflight"
    // "C:\\obermanndavid\\git-ecco-test\\test_featureid\\Marlin"
    //private final static String CSV_PATH = "C:\\Users\\gabil\\Desktop\\results\\results.csv";
    private final static String CSV_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\feature_identification\\results_sqlite3.13.0.csv";
    private final static String FEATURES_PATH = "C:\\Users\\gabil\\Desktop\\PHD\\Mining\\systems\\feature_identification\\sqlite-versions\\3.13.0\\";
    private final static boolean DISPOSE = true;
    private final static boolean DEBUG = true;
    private final static int MAX_COMMITS = 20000;
    private final static boolean EVERYCOMMIT = false;
    private final static int STARTCOMMIT = 15063;
    private final static int ENDCOMMIT = 15067;
    private final static int EVERY_NTH_COMMIT = 1;
    private final static boolean MAX_COMMITS_ENA = true;
    private final static boolean PARALLEL = false;
    private  static String feats = "{";

    public static void main(String... args) throws Exception {
        long measure = System.currentTimeMillis();
        if(!DEBUG && args.length < 2) {
            System.err.println("Two few arguments\n" +
                    "correct usage: arg1: repo path, arg2: path for csv file, arg3: dispose tree(y/n)");
            System.exit(-1);
        }

        String repoPath;
        String csvPath;
        boolean dispose;

        if(DEBUG) {
            repoPath = REPO_PATH;
            csvPath = CSV_PATH;
            dispose = DISPOSE;
        } else {
            repoPath = args[0];
            csvPath = args[1];
            dispose = args[2].equals("y");
        }

        final GitHelper gitHelper = new GitHelper(repoPath, null);
        final GitCommitList commitList = new GitCommitList(gitHelper);

        final List<TraceableFeature> evaluation = Collections.synchronizedList(new ArrayList<>());
        final List<Future<?>> tasks = new ArrayList<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(30);


        commitList.addGitCommitListener(
                (gc, gcl) -> {
                    if(gcl.size() > MAX_COMMITS && MAX_COMMITS_ENA) {
                        writeToCsv(evaluation, csvPath);
                        System.out.println((System.currentTimeMillis()-measure)/1000 + " seconds");
                        executorService.shutdownNow();
                        System.exit(0);
                    }

                    if(PARALLEL) {
                        tasks.add(
                                executorService.submit(() -> {
                                    ID.evaluateFeatureMap(evaluation, ID.id(gc.getTree()),gc.getNumber());
                                    //dispose tree if it is not needed -> for memory saving reasons.
                                    if (dispose) gc.disposeTree();
                                })
                        );
                    } else {
                        ID.evaluateFeatureMap(evaluation, ID.id(gc.getTree()), gc.getNumber());
                        //dispose tree if it is not needed -> for memory saving reasons.
                        if (dispose) gc.disposeTree();
                    }
                }
        );


        if(EVERYCOMMIT) {
            gitHelper.getAllCommits(commitList);
        } else {
            //gitHelper.getEveryNthCommit(commitList, null,STARTCOMMIT, ENDCOMMIT, EVERY_NTH_COMMIT);
            //gitHelper.getEveryNthCommit2(commitList, null,"f044b7153a46d7b2f3de4730c042c780a400b748", "55bcaf6829131233488f57035bc8c2dc6bbdaed1",EVERY_NTH_COMMIT);
        }

        while(PARALLEL && !isDone(tasks)) sleep(100);
        executorService.shutdownNow();

        //print to CSV:
        writeToCsv(evaluation, csvPath);

        System.out.println("finished analyzing repo");
    }

    /**
     * Helper method to check if all tasks are done.
     * @param tasks
     * @return
     */
    private static boolean isDone(List<Future<?>> tasks) {
        for (Future task : tasks)
            if(!task.isDone()) return false;

        return true;
    }


    private static void writeToCsv(List<TraceableFeature> features, String fileName) {
        final File csvFile = new File(fileName);
        System.out.println("writing to CSV");
        FileWriter outputfile = null;

        try {
            //second parameter is boolean for appending --> never append
            outputfile = new FileWriter(csvFile, false);
        } catch (IOException ioe) {
            System.out.println("Error while handling the csv file output!");
        }

        // create CSVWriter object file writer object as parameter
        //deprecated but no other way available --> it still works anyways
        @SuppressWarnings("deprecation")CSVWriter writer = new CSVWriter(outputfile, ';', CSVWriter.NO_QUOTE_CHARACTER);

        //adding header to csv
        writer.writeNext(new String[]{"Label/FeatureName","#total", "#external", "#internal", "#transient"});

        //write each feature/label with: Name, totalOcc, InternalOcc, externalOcc, transientOcc.

        for (TraceableFeature feature : features) {
            if(feature.getExternalOcc()==feature.getTotalOcc() || feature.getExternalOcc() == feature.getCommitList().size())
                feats+="\""+feature.getName()+"\",";
            writer.writeNext(
                    new String[]{
                            feature.getName(),
                            feature.getTotalOcc().toString(),
                            feature.getExternalOcc().toString(),
                            feature.getInternalOcc().toString(),
                            feature.getTransientOcc().toString()
            });
            FileWriter commitList = null;

            try {
                //second parameter is boolean for appending --> never append
                final File featureFile = new File(FEATURES_PATH+feature.getName()+".csv");
                commitList  = new FileWriter(featureFile, false);
                CSVWriter writerFeature = new CSVWriter(commitList, ';', CSVWriter.NO_QUOTE_CHARACTER);
                writerFeature.writeNext(new String[]{"commitNumber","Present"});
                for (Map.Entry<Long, Boolean> commit: feature.getCommitList().entrySet()) {
                    writerFeature.writeNext(new String[]{String.valueOf(commit.getKey()), String.valueOf(commit.getValue())});
                }
                writerFeature.close();
            } catch (IOException ioe) {
                System.out.println("Error while handling the csv file output!");
            }

        }

        System.out.println(feats+"}");

        // closing writer connection
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
