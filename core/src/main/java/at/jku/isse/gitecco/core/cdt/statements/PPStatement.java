package at.jku.isse.gitecco.core.cdt.statements;

import org.eclipse.cdt.core.dom.ast.*;

/**
 * Class to store Eclipse CDT preprocessor statements of certain types.
 */
public class PPStatement {
    private final IASTPreprocessorStatement statement;
    private final int lineStart;
    private final int lineEnd;

    /**
     * Creates a new PPStatement
     *
     * @param pps IASTPreprocessorStatement which should be checked.
     * @throws Exception - If the IASTPreprocessorStatement is not of the right types.
     */
    public PPStatement(IASTPreprocessorStatement pps) throws Exception {

        if (!(pps instanceof IASTPreprocessorElifStatement
                || pps instanceof IASTPreprocessorIfdefStatement
                || pps instanceof IASTPreprocessorIfStatement
                || pps instanceof IASTPreprocessorElseStatement
                || pps instanceof IASTPreprocessorIfndefStatement
                || pps instanceof IASTPreprocessorEndifStatement)) {
            throw new Exception("not the right preprocessor statement");
        }

        this.lineStart = pps.getFileLocation().getStartingLineNumber();
        this.lineEnd = pps.getFileLocation().getEndingLineNumber();
        this.statement = pps;
    }

    /**
     * Gets the ending line number of the PPStatement
     *
     * @return the ending line number
     */
    public int getLineEnd() {
        return lineEnd;
    }

    /**
     * Gets the starting line number of the PPStatement
     *
     * @return The starting line number of the PPStatement
     */
    public int getLineStart() {
        return lineStart;
    }

    /**
     * Extracts the condition string of a conditional preprocessor statement.
     * Only if given Statement is in fact conditional. Other statements are ignored.
     *
     * @return String Condition
     */
    public String getCondName() {
        IASTPreprocessorStatement s = statement;
        StringBuilder result = new StringBuilder();
        if (s instanceof IASTPreprocessorIfStatement) {
            IASTPreprocessorIfStatement is = (IASTPreprocessorIfStatement) s;
            result.append(is.getCondition());
        } else if (s instanceof IASTPreprocessorIfndefStatement) {
            IASTPreprocessorIfndefStatement is = (IASTPreprocessorIfndefStatement) s;
            result.append(is.getCondition());
        } else if (s instanceof IASTPreprocessorIfdefStatement) {
            IASTPreprocessorIfdefStatement is = (IASTPreprocessorIfdefStatement) s;
            result.append(is.getCondition());
        } else if (s instanceof IASTPreprocessorElifStatement) {
            IASTPreprocessorElifStatement is = (IASTPreprocessorElifStatement) s;
            result.append(is.getCondition());
        }

        return result.toString();
    }
}

