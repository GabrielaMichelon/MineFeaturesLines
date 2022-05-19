package org.anarres.cpp;

import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class MyTests {

    private static final String cfile = "src\\test\\resources\\my\\MinTest.c";

//    private static final String cfile = "src\\test\\resources\\my";

    @Test
    public void testMain() throws Exception {
        Main.main(new String[]{cfile, "-I", "C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include", "-I", "C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include/ssp"});
    }

    @Test
    public void testOnlyExpandMacrosInIfs() {
        PreprocessorAPI pp = new PreprocessorAPI(new OnlyExpandMacrosInIfsController());

        //add locations for includes
        pp.addSystemIncludePath("C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include");
        pp.addSystemIncludePath("C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include/ssp");

        //insert code from header files into output?
        pp.setInlineIncludes(false);

        //keep include directives, even though they will be processed either way
        //NOTE: only set one of the two options at a time
        pp.setKeepIncludes(true);

        //keep the define directives, in the output
        pp.setKeepDefines(true);

        //you can set macros that are not defined in the source code
        pp.addMacro("DO_SWAP");

        File src = new File(cfile);
        File target = new File("processed");

        //if you use this the preprocessor will be executed in debug mode
//        pp.debug();

        //src file or directory
        //target directory
        pp.preprocess(src, target, null,null);
    }

    @Test
    public void testReduceToExternalFeatures() {
        Set<String> features = new HashSet<String>();
        features.add("DO_SWAP");
        features.add("NUMBER");

        ReduceToExternalFeatures controller = new ReduceToExternalFeatures(features);

        PreprocessorAPI pp = new PreprocessorAPI(controller);

        //add locations for includes
        pp.addSystemIncludePath("C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include");
        pp.addSystemIncludePath("C:/Program Files (x86)/Dev-Cpp/MinGW64/lib/gcc/x86_64-w64-mingw32/4.9.2/include/ssp");

        //insert code from header files into output?
        pp.setInlineIncludes(false);

        //keep include directives, even though they will be processed either way
        //NOTE: only set one of the two options at a time
        pp.setKeepIncludes(true);

        //keep the define directives, in the output
        pp.setKeepDefines(true);

        File src = new File(cfile);
        File target = new File("processed");

        //if you use this the preprocessor will be executed in debug mode
//        pp.debug();

        //src file or directory
        //target directory
        pp.preprocess(src, target, null,null);

        controller.printMacros();
    }
}