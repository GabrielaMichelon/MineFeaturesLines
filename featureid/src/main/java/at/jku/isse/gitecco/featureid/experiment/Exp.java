package at.jku.isse.gitecco.featureid.experiment;

import at.jku.isse.gitecco.core.cdt.CDTHelper;
import at.jku.isse.gitecco.core.cdt.FeatureParser;
import at.jku.isse.gitecco.core.tree.nodes.SourceFileNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Exp {
    public static void main(String... args) throws CoreException {
        final String s = "C:\\obermanndavid\\git-ecco-test\\test_featureid\\betaflight\\src\\cli.c";

        List<String> codelist = null;
        try {
            codelist = Files.readAllLines(Paths.get(s), StandardCharsets.ISO_8859_1);
        } catch (IOException e1) {
            System.err.println("error reading file: "+s);
            e1.printStackTrace();
        }
        final String code = codelist.stream().collect(Collectors.joining("\n"));

        IASTTranslationUnit translationUnit = null;
        try {
            translationUnit = CDTHelper.parse(code.toCharArray());
        } catch (CoreException e1) {
            System.err.println("error parsing with CDT Core: "+s);
            e1.printStackTrace();
        }

        final IASTPreprocessorStatement[] ppstatements = translationUnit.getAllPreprocessorStatements();
        final FeatureParser featureParser = new FeatureParser();
        //actual tree building
        final SourceFileNode fn = new SourceFileNode(null, s);
        try {
            featureParser.parseToTree(ppstatements, codelist.size(), fn);
        } catch (Exception e) {
            System.err.println("error parsing to tree: "+s);
            e.printStackTrace();
        }

        System.out.println("done");

    }
}
