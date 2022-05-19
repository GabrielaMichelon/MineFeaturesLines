package at.jku.isse.gitecco.core.preprocessor;

import at.jku.isse.gitecco.core.git.GitCommitList;
import at.jku.isse.gitecco.core.type.Feature;
import org.anarres.cpp.OnlyExpandMacrosInIfsController;
import org.anarres.cpp.PreprocessorAPI;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class PreprocessorHelper {


    /**
     * Generates a "clean version" of the given repo/folder.
     * This means: all defines, includes and other PP-Statements are kept in the code.
     * BUT all macros used in #if,#ifdef,#ifndef statements in defines are expanded.
     * @param src
     * @param target
     */
    public Long generateCleanVersion(File src, File target, List<String> dirFiles) {
        PreprocessorAPI pp = new PreprocessorAPI(new OnlyExpandMacrosInIfsController());
        pp.setInlineIncludes(false);
        pp.setKeepIncludes(true);
        pp.setKeepDefines(true);

        Long timeBefore, timeAfter, runtimePPCheckoutCleanVersion;
        timeBefore = System.currentTimeMillis();
        pp.preprocess(src, target, dirFiles,"");
        timeAfter = System.currentTimeMillis();
        runtimePPCheckoutCleanVersion = timeAfter - timeBefore;
        return runtimePPCheckoutCleanVersion;
    }

    /**
     * Generates a variant of a given repository (inPath) in the destination outPath/data
     * @param configuration the configuration for preprocessing
     * @param src input folder
     * @param target output folder
     */
    public void generateVariants(Map<Feature, Integer> configuration,File src, File target, List<String> dirFiles, String commitInformation) {

        //if(target.exists()) GitCommitList.recursiveDelete(target.toPath());
        if(!target.exists()) target.mkdir();

        PreprocessorAPI pp = new PreprocessorAPI();

        pp.setInlineIncludes(false);
        pp.setKeepIncludes(true);
        pp.setKeepDefines(true);

        for (Map.Entry<Feature, Integer> entry : configuration.entrySet()) {
            pp.addMacro(entry.getKey().getName(),entry.getValue().toString());
        }

        pp.preprocess(src, target, dirFiles, commitInformation);

    }

}
