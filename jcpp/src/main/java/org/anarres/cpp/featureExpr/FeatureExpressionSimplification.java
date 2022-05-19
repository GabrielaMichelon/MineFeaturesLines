package org.anarres.cpp.featureExpr;

import org.anarres.cpp.NumericValue;

public class FeatureExpressionSimplification {

    public static FeatureExpression simplify(FeatureExpression expr){
        FeatureExpression simple = expr.clone();
        FeatureExpression last = simple.clone();
        do{
            FeatureExpression simplify = simple.clone();
            SimplificationTraversal traversal = new SimplificationTraversal(simplify);
            simplify.traverse(traversal);
            last = simple.clone();
            simple = traversal.getRoot();
        }while(!simple.equals(last));
        return simple;
    }

    private static class SimplificationTraversal extends PostOrderTraversal {

        private FeatureExpression root;

        public SimplificationTraversal(FeatureExpression root) {
            this.root = root;
        }

        public FeatureExpression getRoot() {
            return root;
        }

        public void postVisit(FeatureExpression expr) {
            if (isNot(expr)) {
                simplifyNot(expr);
            }
            if(expr instanceof ParenthesizedExpr){
                simplifyParentheses(expr);
            }
            if(isAnd(expr)){
                simplifyAnd(expr);
            }
            if(isOr(expr)){
                simplifyOr(expr);
            }
        }

        private boolean isNot(FeatureExpression expr){
            return expr instanceof PrefixExpr && ((PrefixExpr) expr).getOperator().getToken().getText().equals("!");
        }

        private boolean isAnd(FeatureExpression expr){
            return expr instanceof InfixExpr && ((InfixExpr) expr).getOperator().getToken().getText().equals("&&");
        }

        private boolean isOr(FeatureExpression expr){
            return expr instanceof InfixExpr && ((InfixExpr) expr).getOperator().getToken().getText().equals("||");
        }

        private void simplifyNot(FeatureExpression expr){
            FeatureExpression notChild = ((PrefixExpr) expr).getExpr();

            if(notChild instanceof NumberLiteral){
                if(((NumericValue)((NumberLiteral) notChild).getToken().getValue()).doubleValue() == 0){
                    replace(expr, (new FeatureExpressionParser("1")).parse());
                } else {
                    replace(expr, (new FeatureExpressionParser("0")).parse());
                }
            }

            if(isNot(notChild)){
                replace(expr, ((PrefixExpr) notChild).getExpr());
            }
        }

        private void simplifyParentheses(FeatureExpression expr){
            if(expr instanceof ParenthesizedExpr){
                FeatureExpression internal = ((ParenthesizedExpr) expr).getExpr();
                if(internal instanceof ParenthesizedExpr){
                    replace(expr, internal);
                }
                if(isNot(internal)){
                    replace(expr, internal);
                }
                if(internal instanceof SingleTokenExpr || internal instanceof MacroCall){
                    replace(expr, internal);
                }
            }
        }

        private void simplifyAnd(FeatureExpression expr){
            if(isAnd(expr)){
                FeatureExpression ex1 = ((InfixExpr)expr).getLeftHandSide();
                FeatureExpression ex2 = ((InfixExpr)expr).getRightHandSide();
                if(ex1 instanceof  NumberLiteral){
                    if(((NumericValue)((NumberLiteral) ex1).getToken().getValue()).doubleValue() == 0){
                        replace(expr, new FeatureExpressionParser("0").parse());
                        return;
                    } else {
                        replace(expr, ex2);
                        return;
                    }
                } else if(ex2 instanceof  NumberLiteral){
                    if(((NumericValue)((NumberLiteral) ex2).getToken().getValue()).doubleValue() == 0){
                        replace(expr, new FeatureExpressionParser("0").parse());
                        return;
                    } else {
                        replace(expr, ex1);
                        return;
                    }
                }
                if(ex1.equals(ex2)){
                    replace(expr, ex1);
                    return;
                }
                if(isNot(ex1)){
                    FeatureExpression internal = ((PrefixExpr) ex1).getExpr();
                    if(internal.equals(ex2)){
                        replace(expr, new FeatureExpressionParser("0").parse());
                        return;
                    }
                    //also check if expr in not is in Parentheses, and then compare inside them
                    if(internal instanceof  ParenthesizedExpr){
                        if(((ParenthesizedExpr) internal).getExpr().equals(ex2)){
                            replace(expr, new FeatureExpressionParser("0").parse());
                            return;
                        }
                    }
                }
                if(isNot(ex2)){
                    FeatureExpression internal = ((PrefixExpr) ex2).getExpr();
                    if(internal.equals(ex1)){
                        replace(expr, new FeatureExpressionParser("0").parse());
                        return;
                    }
                    //also check if expr in not is in Parentheses, and then compare inside them
                    if(internal instanceof  ParenthesizedExpr){
                        if(((ParenthesizedExpr) internal).getExpr().equals(ex1)){
                            replace(expr, new FeatureExpressionParser("0").parse());
                            return;
                        }
                    }
                }
            }
        }

        private void simplifyOr(FeatureExpression expr){
            if(isOr(expr)){
                FeatureExpression ex1 = ((InfixExpr)expr).getLeftHandSide();
                FeatureExpression ex2 = ((InfixExpr)expr).getRightHandSide();
                if(ex1 instanceof  NumberLiteral){
                    if(((NumericValue)((NumberLiteral) ex1).getToken().getValue()).doubleValue() == 0){
                        replace(expr, ex2);
                        return;
                    } else {
                        replace(expr, new FeatureExpressionParser("1").parse());
                        return;
                    }
                } else if(ex2 instanceof  NumberLiteral){
                    if(((NumericValue)((NumberLiteral) ex2).getToken().getValue()).doubleValue() == 0){
                        replace(expr, ex1);
                        return;
                    } else {
                        replace(expr, new FeatureExpressionParser("1").parse());
                        return;
                    }
                }
                if(ex1.equals(ex2)){
                    replace(expr, ex1);
                    return;
                }
                if(isNot(ex1)){
                    FeatureExpression internal = ((PrefixExpr) ex1).getExpr();
                    if(internal.equals(ex2)){
                        replace(expr, new FeatureExpressionParser("1").parse());
                        return;
                    }
                    //also check if expr in not is in Parentheses, and then compare inside them
                    if(internal instanceof  ParenthesizedExpr){
                        if(((ParenthesizedExpr) internal).getExpr().equals(ex2)){
                            replace(expr, new FeatureExpressionParser("1").parse());
                            return;
                        }
                    }
                }
                if(isNot(ex2)){
                    FeatureExpression internal = ((PrefixExpr) ex2).getExpr();
                    if(internal.equals(ex1)){
                        replace(expr, new FeatureExpressionParser("1").parse());
                        return;
                    }
                    //also check if expr in not is in Parentheses, and then compare inside them
                    if(internal instanceof  ParenthesizedExpr){
                        if(((ParenthesizedExpr) internal).getExpr().equals(ex1)){
                            replace(expr, new FeatureExpressionParser("1").parse());
                            return;
                        }
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

}
