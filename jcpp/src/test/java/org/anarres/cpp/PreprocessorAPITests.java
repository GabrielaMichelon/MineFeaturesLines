package org.anarres.cpp;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class PreprocessorAPITests {

    private static final String SRC = "src\\test\\resources\\my";
//    private static final String SRC = "src\\test\\resources\\my\\MinTest.c";

    private static final String TARGET = "processed";

//    private static final String cfile = "C:\\Users\\sfischer\\Desktop\\JKU\\systems\\glibc";
//    private static final String TARGET = "C:\\Users\\sfischer\\Desktop\\JKU\\systems\\processed\\glibc";

    private static final Stream<String> SYSTEM_INCLUDE_PATHS = Stream.of(
            "C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include",
            "C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include/ssp"
    );

    @Test
    public void testMain() throws Exception {
        Main.main(new String[]{SRC, "-I", "C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include", "-I", "C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include/ssp"});
    }

    @Test
    public void testSimplePreProcess() {
        PreprocessorAPI pp = new PreprocessorAPI(new OnlyExpandMacrosInIfsController());

        //add locations for includes
        SYSTEM_INCLUDE_PATHS.forEach(pp::addSystemIncludePath);

        File src = new File(SRC);
        File target = new File(TARGET);

        pp.preprocess(src, target);
    }

    /**
     * Generates a "clean version" of the given repo/folder.
     * This means: all defines, includes and other PP-Statements are kept in the code.
     * BUT all macros used in #if,#ifdef,#ifndef statements in defines are expanded.
     */
    @Test
    public void generateCleanVersion() {
        File src = new File(SRC);
        File target = new File(TARGET);

        PreprocessorAPI pp = new PreprocessorAPI(new OnlyExpandMacrosInIfsController());
        //add locations for includes
        SYSTEM_INCLUDE_PATHS.forEach(pp::addSystemIncludePath);

        pp.setInlineIncludes(false);
        pp.setKeepIncludes(true);
        pp.setKeepDefines(true);

        pp.preprocess(src, target);
    }

    @Test
    public void testOnlyExpandMacrosInIfs() {
        PreprocessorAPI pp = new PreprocessorAPI(new OnlyExpandMacrosInIfsController());

        //add locations for includes
        SYSTEM_INCLUDE_PATHS.forEach(pp::addSystemIncludePath);

        //insert code from header files into output?
        pp.setInlineIncludes(false);

        //keep include directives, even though they will be processed either way
        //NOTE: only set one of the two options at a time (e.g. setKeepIncludes OR setInlineIncludes)
        pp.setKeepIncludes(true);

        //keep the define directives, in the output
        pp.setKeepDefines(true);

        //you can set macros that are not defined in the source code
        pp.addMacro("DO_SWAP");

        File src = new File(SRC);
        File target = new File(TARGET);

        //if you use this the preprocessor will be executed in debug mode
//        pp.debug();

        //src file or directory
        //target directory
        pp.preprocess(src, target);
    }

    @Test
    public void testReduceToExternalFeatures() {
        Set<String> features = new HashSet<>();
        features.add("DO_SWAP");
        features.add("NUMBER");

        ReduceToExternalFeatures controller = new ReduceToExternalFeatures(features);

        PreprocessorAPI pp = new PreprocessorAPI(controller);

        //add locations for includes
        SYSTEM_INCLUDE_PATHS.forEach(pp::addSystemIncludePath);

        //insert code from header files into output?
        pp.setInlineIncludes(false);

        //keep include directives, even though they will be processed either way
        //NOTE: only set one of the two options at a time
        pp.setKeepIncludes(true);

        //keep the define directives, in the output
        pp.setKeepDefines(true);

        File src = new File(SRC);
        File target = new File(TARGET);

        //if you use this the preprocessor will be executed in debug mode
//        pp.debug();

        //src file or directory
        //target directory
        pp.preprocess(src, target);

        controller.printMacros();
    }
}