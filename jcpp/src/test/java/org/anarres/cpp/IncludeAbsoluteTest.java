package org.anarres.cpp;

import com.google.common.io.CharStreams;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author shevek
 */
public class IncludeAbsoluteTest {

    private static final Logger LOG = LoggerFactory.getLogger(IncludeAbsoluteTest.class);

    @Test
    public void testAbsoluteInclude() throws Exception {
        File file = new File("src/test/resources/absolute.h");
        assertTrue(file.exists());

        String input = "#include <" + file.getAbsolutePath().replace('\\', '/') + ">\n";
        LOG.info("Input: " + input);
        Preprocessor pp = new Preprocessor();
        pp.addInput(new StringLexerSource(input, true));
        Reader r = new CppReader(pp);
        String output = CharStreams.toString(r);
        r.close();
        LOG.info("Output: " + output);
        assertTrue(output.contains("absolute-result"));
    }
}
