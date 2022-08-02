package org.anarres.cpp;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.anarres.cpp.Token.EOF;
import static org.anarres.cpp.Token.INVALID;
import static org.junit.jupiter.api.Assertions.*;

public class ErrorTest {

    private boolean testError(Preprocessor p)
            throws LexerException,
            IOException {
        for (;;) {
            Token tok = p.token();
            if (tok.getType() == EOF)
                break;
            if (tok.getType() == INVALID)
                return true;
        }
        return false;
    }

    private void testError(String input) throws Exception {
        StringLexerSource sl;
        DefaultPreprocessorListener pl;
        Preprocessor p;

        /* Without a PreprocessorListener, throws an exception. */
        sl = new StringLexerSource(input, true);
        p = new Preprocessor();
        p.addFeature(Feature.CSYNTAX);
        p.addInput(sl);
        try {
            assertTrue(testError(p));
            fail("Lexing unexpectedly succeeded without listener.");
        } catch (LexerException e) {
            /* required */
        }

        /* With a PreprocessorListener, records the error. */
        sl = new StringLexerSource(input, true);
        p = new Preprocessor();
        p.addFeature(Feature.CSYNTAX);
        p.addInput(sl);
        pl = new DefaultPreprocessorListener();
        p.setListener(pl);
        assertNotNull(p.getListener(), "CPP has listener");
        assertTrue(testError(p));
        assertTrue(pl.getErrors() > 0, "Listener has errors");

        /* Without CSYNTAX, works happily. */
        sl = new StringLexerSource(input, true);
        p = new Preprocessor();
        p.addInput(sl);
        assertTrue(testError(p));
    }

    @Test
    public void testErrors() throws Exception {
        testError("\"");
        testError("'");
        // testError("''");
    }

}
