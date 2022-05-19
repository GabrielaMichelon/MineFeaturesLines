package org.anarres.cpp;

import org.anarres.cpp.featureExpr.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class ReduceToExternalFeatures extends PreprocessorControlListener {

    private final Set<String> externalFeatures;

    private Preprocessor pp;

    //mapping feature expression to macro
    private Map<String, Map<String, Macro>> macros;

    public ReduceToExternalFeatures(Set<String> externalFeatures) {
        this.externalFeatures = externalFeatures;
        this.macros = new HashMap<String, Map<String, Macro>>();
    }

    public boolean addMacro(Macro m, Source source) {
        FeatureExpression stateExpr = currentStateCondition();
        addMacro(m, stateExpr);
        return true;
    }

    private void addMacro(Macro m, FeatureExpression expr){
        System.out.println("addMacro: " + m.getName() + " : " + expr);
        Map<String, Macro> byExpr = this.macros.get(m.getName());
        if(byExpr == null){
            byExpr = new HashMap<String, Macro>();
            byExpr.put(FeatureExpressionSimplification.simplify(negate(expr)).toString(), null); //add negated expr so that we also cover the case where the macro is never defined
            this.macros.put(m.getName(), byExpr);
        } else {
            //update previous expressions with not expr
            Map<String, Macro> updatedExpressions = new HashMap<String, Macro>();
            for(Map.Entry<String, Macro> entry : byExpr.entrySet()){
                FeatureExpression updated = conjunct(new FeatureExpressionParser(entry.getKey()).parse(), negate(expr));
                //simplify
                updated= FeatureExpressionSimplification.simplify(updated);
                updatedExpressions.put(updated.toString(), entry.getValue());
            }
            this.macros.put(m.getName(), updatedExpressions);
            byExpr = updatedExpressions;
        }
        //insert m with current expr
        byExpr.put(expr.toString(), m);
    }

    public boolean removeMacro(Macro m, Source source) {
        //remove macro and update conditions
        FeatureExpression stateExpr = currentStateCondition();
        removeMacro(m, stateExpr);
        return true;
    }

    private void removeMacro(Macro m, FeatureExpression expr){
        System.out.println("removeMacro: " + m.getName() + " : " + expr);
        Map<String, Macro> byExpr = this.macros.get(m.getName());
        if(byExpr != null){
            //update previous expressions with not expr
            Map<String, Macro> updatedExpressions = new HashMap<String, Macro>();
            for(Map.Entry<String, Macro> entry : byExpr.entrySet()){
                FeatureExpression updated = conjunct(new FeatureExpressionParser(entry.getKey()).parse(), negate(expr));
                //simplify
                updated= FeatureExpressionSimplification.simplify(updated);
                updatedExpressions.put(updated.toString(), entry.getValue());
            }
            this.macros.put(m.getName(), updatedExpressions);
            byExpr = updatedExpressions;
        } else {
            byExpr = new HashMap<String, Macro>();
            this.macros.put(m.getName(), byExpr);
            byExpr.put(negate(expr).toString(), m);
        }
        byExpr.put(expr.toString(), null);
    }

    private FeatureExpression currentStateCondition(){
        if(pp != null){
            FeatureExpression stateExpr = null;
            for(State state : pp.getStates()){
                FeatureExpression expr = stateCondition(state);
                if(stateExpr == null){
                    stateExpr = expr;
                } else {
                    stateExpr = conjunct(stateExpr, expr);
                }
            }
            return simplify(stateExpr);
        } else {
            return new FeatureExpressionParser("1").parse();
        }
    }

    private FeatureExpression stateCondition(State state){
        if(state.getTokens() != null) {
            FeatureExpression stateExpr = null;
            int i = 1;
            for (List<Token> tokens : state.getTokens()) {
                Token first = tokens.get(0);
                boolean negate = first.getText().equals("ifndef");
                FeatureExpressionParser parser = new FeatureExpressionParser(tokens.subList(1, tokens.size()));
                FeatureExpression expr = parser.parse();
                //negate if we are inside else or elif
                if(state.sawElse() || i < state.getTokens().size()){
                    if(expr instanceof  NumberLiteral){
                        if(((NumericValue)((NumberLiteral) expr).getToken().getValue()).doubleValue() == 0){
                            expr = new FeatureExpressionParser("1").parse();
                        } else {
                            expr = new FeatureExpressionParser("0").parse();
                        }
                    } else if(!negate){
                        expr = negate(expr);
                    }
                } else if(negate){
                    expr = negate(expr);
                }

                if(stateExpr == null){
                    stateExpr = expr;
                } else {
                    stateExpr = conjunct(stateExpr, expr);
                }
                i++;
            }
            return stateExpr;
        } else {
            return new FeatureExpressionParser("1").parse();
        }
    }

    private FeatureExpression simplify(FeatureExpression ex){
        //simplify
        ex = FeatureExpressionSimplification.simplify(ex);
        if(ex instanceof  NumberLiteral){
            if(((NumericValue)((NumberLiteral) ex).getToken().getValue()).doubleValue() != 0){
                return new FeatureExpressionParser("1").parse();
            }
        }
        return ex;
    }

    private boolean isFalse(FeatureExpression ex){
        //simplify
        ex = FeatureExpressionSimplification.simplify(ex);
        if(ex instanceof  NumberLiteral){
            if(((NumericValue)((NumberLiteral) ex).getToken().getValue()).doubleValue() == 0){
                return true;
            }
        }
        return false;
    }

    private FeatureExpression conjunct(FeatureExpression ex1 , FeatureExpression ex2){
        if(ex1.equals(ex2)){
            return ex1;
        }
        if(ex1 instanceof  NumberLiteral){
            if(((NumericValue)((NumberLiteral) ex1).getToken().getValue()).doubleValue() == 0){
                return new FeatureExpressionParser("0").parse();
            } else {
                return ex2;
            }
        } else if(ex2 instanceof  NumberLiteral){
            if(((NumericValue)((NumberLiteral) ex2).getToken().getValue()).doubleValue() == 0){
                return new FeatureExpressionParser("0").parse();
            } else {
                return ex1;
            }
        } else {
            return new FeatureExpressionParser("(" + ex1 + " && " + ex2 + ")").parse();
        }
    }

    private FeatureExpression disjunct(FeatureExpression ex1 , FeatureExpression ex2){
        if(ex1.equals(ex2)){
            return ex1;
        }
        if(ex1 instanceof  NumberLiteral){
            if(((NumericValue)((NumberLiteral) ex1).getToken().getValue()).doubleValue() == 0){
                return ex2;
            } else {
                return new FeatureExpressionParser("1").parse();
            }
        } else if(ex2 instanceof  NumberLiteral){
            if(((NumericValue)((NumberLiteral) ex2).getToken().getValue()).doubleValue() == 0){
                return ex1;
            } else {
                return new FeatureExpressionParser("1").parse();
            }
        } else {
            return new FeatureExpressionParser("(" + ex1 + " || " + ex2 + ")").parse();
        }
    }

    private FeatureExpression negate(FeatureExpression ex){
        return new FeatureExpressionParser("!(" + ex + ")").parse();
    }

    public boolean expandMacro(Macro m, Source source, int line, int column, boolean isInIf) {
        return isInIf;
    }

    public boolean include(@Nonnull Source source, int line, @Nonnull String name, boolean quoted, boolean next) {
        return true;
    }

    public boolean processIf(List<Token> condition, Source source, IfType type) {
        if (source instanceof FileLexerSource) {
            if (((FileLexerSource) source).getFile().equals(getFileCurrentlyProcessed())) {
                return false;
            }
        }
        return true;
    }

    public String getPartiallyProcessedCondition(List<Token> condition, Source source, IfType type, final Preprocessor pp) {
        this.pp = pp;
        if (source instanceof FileLexerSource) {
            if (((FileLexerSource) source).getFile().equals(getFileCurrentlyProcessed())) {
                if (type == IfType.IF || type == IfType.ELSIF || type == IfType.IFDEF || type == IfType.IFNDEF) {

                    FeatureExpression expr;

                    //convert IFDEF and IFNDEF to IF, IFNDEF has to consider negation
                    if(type == IfType.IFDEF) {
                        FeatureExpressionParser parser = new FeatureExpressionParser("defined(" + condition.get(0).getText() + ")");
                        expr = parser.parse();
                    } else if(type == IfType.IFNDEF){
                        FeatureExpressionParser parser = new FeatureExpressionParser("!defined(" + condition.get(0).getText() + ")");
                        expr = parser.parse();
                    } else {
                        FeatureExpressionParser parser = new FeatureExpressionParser(condition);
                        expr = parser.parse();
                    }

                    final Set<String> containedMacros = new HashSet<String>();
                    final Set<String> containedExternalFeatures = new HashSet<String>();
                    expr.traverse(new PreOrderTraversal() {
                        @Override
                        public void preVisit(FeatureExpression expr) {
                            if(expr instanceof MacroCall){
                                String macroName = ((MacroCall) expr).getName().toString();
                                if(macros.get(macroName) != null){
                                    containedMacros.add(macroName);
                                }
                                if(externalFeatures.contains(macroName)){
                                    containedExternalFeatures.add(macroName);
                                }
                            } else if(expr instanceof Name){
                                if(expr.getParent() instanceof MacroCall){
                                    if(((MacroCall) expr.getParent()).getName() == expr){
                                        return;
                                    }
                                }
                                String macroName = expr.toString();
                                if(macros.get(macroName) != null){
                                    containedMacros.add(macroName);
                                }
                                if(externalFeatures.contains(macroName)){
                                    containedExternalFeatures.add(macroName);
                                }
                            }
                        }
                    });

                    if(!containedMacros.isEmpty()) {
                        //get all combinations of conditions for the contained macros
                        Set<Map<String, String>> combinations = getMacroCombinations(containedMacros);
                        FeatureExpression ret = null;
                        for (final Map<String, String> combination : combinations) {
                            //get expression for current configuration

                            FeatureExpression configExpression = getConfigurationExpression(combination);
                            //filter combinations that can not evaluate to true ever (e.g. A && !A)
                            if(isFalse(configExpression)){
                                continue;
                            }

                            FeatureExpression ex = expr.clone(); //new FeatureExpressionParser(condition).parse();
                            ConditionTraversal traversal = new ConditionTraversal(ex, pp, combination);
                            ex.traverse(traversal);
                            if (ret == null) {
                                ret = conjunct(configExpression, traversal.getRoot());
                            } else {
                                ret = disjunct(ret, conjunct(configExpression, traversal.getRoot()));
                            }
                        }

                        if (ret != null) {
                            EvalTraversal traversal = new EvalTraversal(ret, pp);
                            ret.traverse(traversal);

                            System.out.println("Simplify:");
                            System.out.println(traversal.getRoot());
                            System.out.println(FeatureExpressionSimplification.simplify(traversal.getRoot()));

                            FeatureExpression simple = FeatureExpressionSimplification.simplify(traversal.getRoot());

                            if (type == IfType.IFDEF || type == IfType.IFNDEF) {
                                return "#if " + simple.toString();
                            }
                            return simple.toString();
                        }
                    } else {
//                        ConditionTraversal traversal = new ConditionTraversal(expr, pp, null);
//                        expr.traverse(traversal);
//                        EvalTraversal eval = new EvalTraversal(traversal.getRoot(), pp);
//                        traversal.getRoot().traverse(eval);

                        EvalTraversal eval = new EvalTraversal(expr, pp);
                        expr.traverse(eval);

                        System.out.println("Simplify2:");
                        System.out.println(eval.getRoot());
                        System.out.println(FeatureExpressionSimplification.simplify(eval.getRoot()));

                        FeatureExpression simple = FeatureExpressionSimplification.simplify(eval.getRoot());

                        if (type == IfType.IFDEF || type == IfType.IFNDEF) {
                            return "#if " + simple.toString();
                        }
                        return simple.toString();
                    }
                }
            }
        }
        return null;
    }

    private Set<Map<String, String>> getMacroCombinations(Set<String> containedMacros){
        String[] macroNames = containedMacros.toArray(new String[containedMacros.size()]);
        return getMacroCombinations(macroNames, 0);
    }

    private Set<Map<String, String>> getMacroCombinations(String[] macroNames, int index){
        if(index >= macroNames.length){
            return new HashSet<Map<String, String>>();
        }
        String macroName = macroNames[index];
        if(index == macroNames.length - 1){ //last macro
            Set<Map<String, String>> combinations = new HashSet<Map<String, String>>();
            Map<String, Macro> macrosByCondition = macros.get(macroName);
            for(Map.Entry<String, Macro> entry : macrosByCondition.entrySet()){
                Map<String, String> options = new HashMap<String, String>();
                options.put(macroName, entry.getKey());
                combinations.add(options);
            }
            return combinations;
        } else {
            Set<Map<String, String>> nextCombinations = getMacroCombinations(macroNames, index + 1);
            Set<Map<String, String>> combinations = new HashSet<Map<String, String>>();
            Map<String, Macro> macrosByCondition = macros.get(macroName);
            for(Map.Entry<String, Macro> entry : macrosByCondition.entrySet()){
                for(Map<String, String> nextEntry : nextCombinations){
                    Map<String, String> options = new HashMap<String, String>();
                    options.putAll(nextEntry);
                    options.put(macroName, entry.getKey());
                    combinations.add(options);
                }
            }
            return combinations;
        }
    }

    private FeatureExpression getConfigurationExpression(Map<String, String> combination) {
        FeatureExpression ret = null;
        for(Map.Entry<String, String> entry : combination.entrySet()){
            FeatureExpressionParser parser = new FeatureExpressionParser(entry.getValue());
            FeatureExpression expr = parser.parse();
            if(ret == null){
                ret = expr;
            } else {
                ret = conjunct(ret, expr);
            }
        }
        return ret;
    }

    private boolean containsFeature(FeatureExpression featureExpression){
        if(featureExpression instanceof Name){
            return externalFeatures.contains(featureExpression.toString());
        }
        for(FeatureExpression child : featureExpression.getChildren()){
            if(containsFeature(child)){
                return true;
            }
        }
        return false;
    }

    public void printMacros() {
        for(Map.Entry<String, Map<String, Macro>> macro : macros.entrySet()){
            for(Map.Entry<String, Macro> condition : macro.getValue().entrySet()){
                System.out.println(macro.getKey() + " : " + condition.getKey() + " : " + condition.getValue());
            }
        }
    }

    private class ConditionTraversal extends PostOrderTraversal{

        private FeatureExpression root;

        private final Preprocessor pp;

        private final Map<String, String> combination;

        public ConditionTraversal(FeatureExpression root, final Preprocessor pp, final Map<String, String> combination){
            super();
            this.root = root;
            this.pp = pp;
            this.combination = combination;
        }

        public FeatureExpression getRoot() {
            return root;
        }

        @Override
        public void postVisit(FeatureExpression visitedExpr) {
            String macroName = null;
            if(visitedExpr instanceof MacroCall){
                macroName = ((MacroCall) visitedExpr).getName().toString();
            } else if(visitedExpr instanceof Name){
                if(visitedExpr.getParent() instanceof MacroCall && (((MacroCall) visitedExpr.getParent()).getName() == visitedExpr || ((MacroCall) visitedExpr.getParent()).getName().toString().equals("defined"))){
                    return;
                }
                macroName = visitedExpr.toString();
            }

            if(macroName != null){
                String condition = null;
                 if(this.combination != null){
                    condition = this.combination.get(macroName);
                }
                //check for macro defined here, for macros that may be undefined conditionally
                if(condition == null){
                    if(macroName.equals("defined")){
//                        if (!containsFeature(visitedExpr)) {
                            if (visitedExpr instanceof MacroCall) {
                                FeatureExpression arg = ((MacroCall) visitedExpr).getArguments().get(0);
                                if (arg instanceof Name) {
                                    String cond = combination.get(arg.toString());
                                    if(cond != null) {
                                        Macro m = macros.get(arg.toString()).get(cond);
                                        if(m != null) {
//                                            FeatureExpressionParser parser = new FeatureExpressionParser(cond);
//                                            FeatureExpression condExpr = parser.parse();
//                                            replace(visitedExpr, condExpr);
                                            replace(visitedExpr, new FeatureExpressionParser("1").parse());
                                        } else {
                                            replace(visitedExpr, new FeatureExpressionParser("0").parse());
                                        }
                                    }
                                }
//                            }
                        }
                    } else {
                        try {
                            List<Token> expanded = pp.expand(visitedExpr.toString());
                            FeatureExpressionParser parser = new FeatureExpressionParser(expanded);
                            FeatureExpression expandedExpr = parser.parse();

                            if (!containsFeature(expandedExpr)) {
                                try {
                                    long val = pp.expr(expandedExpr.toString());
                                    parser = new FeatureExpressionParser(val + "");
                                    FeatureExpression evalExpr = parser.parse();
                                    expandedExpr = evalExpr;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (LexerException e) {
                                    e.printStackTrace();
                                }
                            }

                            replace(visitedExpr, expandedExpr);

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (LexerException e) {
                            e.printStackTrace();
                        }
                    }
                } else {

                    //macros in our mapping (should be most, other than predefined ones)
                    try {
                        Macro m = macros.get(macroName).get(condition);
                        if(m != null) {
                            List<Token> expanded = pp.expand(m, visitedExpr.toString());
                            FeatureExpressionParser parser = new FeatureExpressionParser(expanded);
                            FeatureExpression expandedExpr = parser.parse();

                            if (!containsFeature(expandedExpr)) {
                                try {
                                    long val = pp.expr(expandedExpr.toString());
                                    parser = new FeatureExpressionParser(val + "");
                                    FeatureExpression evalExpr = parser.parse();
                                    expandedExpr = evalExpr;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (LexerException e) {
                                    e.printStackTrace();
                                }
                            }
//                        expandedExpr = conjunct(expandedExpr, new FeatureExpressionParser(condition).parse());

                            replace(visitedExpr, expandedExpr);
                        } else {
                            replace(visitedExpr, new FeatureExpressionParser("0").parse());
//                            System.err.println("Tried to expand undefined macro");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (LexerException e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        private void replace(FeatureExpression expr, FeatureExpression replacement){
            if(expr == this.root){
                root = replacement;
            } else {
                expr.getParent().replace(expr, replacement);
            }
        }
    }

    private class EvalTraversal extends PreOrderTraversal{

        private FeatureExpression root;

        private final Preprocessor pp;

        public EvalTraversal(FeatureExpression root, final Preprocessor pp){
            super();
            this.root = root;
            this.pp = pp;
        }

        public void preVisit(FeatureExpression visitedExpr) {
            if(!containsFeature(visitedExpr)){
                if(visitedExpr != root && visitedExpr instanceof SingleTokenExpr){
                    return;
                }
                try {
                    long val = pp.expr(visitedExpr.toString());
                    FeatureExpressionParser parser = new FeatureExpressionParser(val + "");
                    FeatureExpression evalExpr = parser.parse();
                    replace(visitedExpr, evalExpr);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (LexerException e) {
                    e.printStackTrace();
                }
            }
        }

        private void replace(FeatureExpression expr, FeatureExpression replacement){
            if(expr == this.root){
                root = replacement;
            } else {
                expr.getParent().replace(expr, replacement);
            }
        }

        public FeatureExpression getRoot() {
            return root;
        }
    }
}
