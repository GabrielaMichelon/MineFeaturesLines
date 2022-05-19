package at.jku.isse.gitecco.core.cdt;

import at.jku.isse.gitecco.core.cdt.statements.PPStatement;
import at.jku.isse.gitecco.core.tree.nodes.*;
import org.eclipse.cdt.core.dom.ast.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing features in the c++ file.
 */
public class FeatureParser {

    /**
     * Retains all the features contained in the file as a tree of features --> TreeFeature
     *
     * @param ppstatements
     * @param linecnt
     * @return The root of the tree.
     * @throws Exception
     */
    public SourceFileNode parseToTree(IASTPreprocessorStatement[] ppstatements, int linecnt, SourceFileNode srcfilenode) throws Exception {
        //create artificial BASE Node
        srcfilenode.setBase(new BaseNode(srcfilenode, linecnt));

        PPStatement pp;

        //var for the current block node to which conditionals are added
        ConditionBlockNode currentBlock;
        //var for current Conditional node which gets filled with children etc.
        ConditionalNode currentConditional = srcfilenode.getBaseNode();

        for (IASTPreprocessorStatement pps : ppstatements) {
            /* starting statements*/
            if (pps instanceof IASTPreprocessorIfStatement) {
                pp = new PPStatement(pps);
                currentBlock = new ConditionBlockNode(currentConditional);
                currentConditional.addChild(currentBlock);
                currentConditional = currentBlock.setIfBlock(new IFCondition(currentBlock, removeDefinedMacro(pp.getCondName())));
                currentConditional.setLineFrom(pp.getLineStart());
            } else if (pps instanceof IASTPreprocessorIfdefStatement) {
                pp = new PPStatement(pps);
                currentBlock = new ConditionBlockNode(currentConditional);
                currentConditional.addChild(currentBlock);
                currentConditional = currentBlock.setIfBlock(new IFDEFCondition(currentBlock, pp.getCondName()));
                currentConditional.setLineFrom(pp.getLineStart());

            } else if (pps instanceof IASTPreprocessorIfndefStatement) {
                pp = new PPStatement(pps);
                currentBlock = new ConditionBlockNode(currentConditional);
                currentConditional.addChild(currentBlock);
                currentConditional = currentBlock.setIfBlock(new IFNDEFCondition(currentBlock, pp.getCondName()));
                currentConditional.setLineFrom(pp.getLineStart());

                /* ending statements */
            } else if (pps instanceof IASTPreprocessorElifStatement) {
                pp = new PPStatement(pps);
                currentConditional.setLineTo(pp.getLineEnd());
                currentBlock = currentConditional.getParent();
                currentConditional = currentBlock.addElseIfBlock(new ELIFCondition(currentBlock, removeDefinedMacro(pp.getCondName())));
                currentConditional.setLineFrom(pp.getLineStart());

            } else if (pps instanceof IASTPreprocessorElseStatement) {
                pp = new PPStatement(pps);
                currentConditional.setLineTo(pp.getLineEnd());
                currentBlock = currentConditional.getParent();
                currentConditional = currentBlock.setElseBlock(new ELSECondition(currentBlock));
                currentConditional.setLineFrom(pp.getLineStart());

            } else if (pps instanceof IASTPreprocessorEndifStatement) {
                pp = new PPStatement(pps);
                currentConditional.setLineTo(pp.getLineEnd());
                currentBlock = currentConditional.getParent();
                currentConditional = currentBlock.getParent();

                /* define/undef statements: */
            } else if (pps instanceof IASTPreprocessorMacroDefinition) {
                IASTPreprocessorMacroDefinition md = (IASTPreprocessorMacroDefinition) pps;
                currentConditional.addDefineNode(
                        new Define(md.getName().toString(),
                                md.getExpansion(),
                                md.getFileLocation().getStartingLineNumber(),
                                currentConditional)
                );
            } else if (pps instanceof IASTPreprocessorUndefStatement) {
                IASTPreprocessorUndefStatement uds = (IASTPreprocessorUndefStatement) pps;
                currentConditional.addDefineNode(
                        new Undef(uds.getMacroName().toString(), uds.getFileLocation().getStartingLineNumber(), currentConditional)
                );
            } else if (pps instanceof IASTPreprocessorIncludeStatement) {
                IASTPreprocessorIncludeStatement is = (IASTPreprocessorIncludeStatement) pps;
                currentConditional.addInclude(
                        new IncludeNode(is.getName().toString(), is.getFileLocation().getStartingLineNumber(), currentConditional)
                );
            }
        }
        return srcfilenode;
    }

    //removes the defined() macro and replaces it by simply the variable
    //the variable will be defined therefore and coan will expand the macro --> should work.
    private String removeDefinedMacro(String s) {
        Pattern p = Pattern.compile("defined *\\((.*?)\\)");
        Matcher m = p.matcher(s);

        while (m.find())
            s = s.replaceFirst("defined *\\((.*?)\\)", m.group(1));

        return s.replace("defined", "").replace("not", "!");
    }

    //checks if a string contains space separated number
    //or a variable with a number at the beginning of the string
    private boolean hasNumberOcc(String s) {
        String strs[] = s.split(" ");
        for (String str : strs)
            if (str.matches("^\\d[0-9a-zA-Z]*$")) return true;
        return false;
    }
}
