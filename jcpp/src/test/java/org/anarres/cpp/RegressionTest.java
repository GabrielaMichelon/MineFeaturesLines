/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.cpp;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.PatternFilenameFilter;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author shevek
 */
public class RegressionTest {

    private static final Logger LOG = LoggerFactory.getLogger(RegressionTest.class);

    public static Stream<Arguments> data() throws Exception {
        List<Arguments> out = new ArrayList<>();

        File dir = new File("src/test/resources/regression");
        for (File inFile : dir.listFiles(new PatternFilenameFilter(".*\\.in"))) {
            String name = Files.getNameWithoutExtension(inFile.getName());
            File outFile = new File(dir, name + ".out");
            out.add(Arguments.of(name, inFile, outFile));
        }

        return out.stream();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRegression(String name, File inFile, File outFile) throws Exception {
        String inText = Files.toString(inFile, Charsets.UTF_8);
        LOG.info("Read " + name + ":\n" + inText);
        CppReader cppReader = new CppReader(new StringReader(inText));
        String cppText = CharStreams.toString(cppReader);
        LOG.info("Generated " + name + ":\n" + cppText);
        if (outFile.exists()) {
            String outText = Files.toString(outFile, Charsets.UTF_8);
            LOG.info("Expected " + name + ":\n" + outText);
            assertEquals(outText, inText);
        }

    }
}
