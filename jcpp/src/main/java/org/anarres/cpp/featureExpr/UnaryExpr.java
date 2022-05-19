package org.anarres.cpp.featureExpr;

public abstract class UnaryExpr extends FeatureExpression {

    private FeatureExpression expr;

    private SingleTokenExpr operator;

    public UnaryExpr(FeatureExpression expr, SingleTokenExpr operator) {
        super();
        setExpr(expr);
        setOperator(operator);
    }

    public FeatureExpression getExpr() {
        return expr;
    }

    public void setExpr(FeatureExpression expr) {
        this.expr = expr;
        this.expr.setParent(this);
    }

    public SingleTokenExpr getOperator() {
        return operator;
    }

    public void setOperator(SingleTokenExpr operator) {
        this.operator = operator;
        this.operator.setParent(this);
    }

    public boolean equals(FeatureExpression other) {
        if(other == this){
            return true;
        }
        if(!(other instanceof UnaryExpr)){
            return false;
        }
        if(this.getClass() != other.getClass()){
            return false;
        }
        return this.expr.equals(((UnaryExpr)other).expr)
                && this.operator.equals(((UnaryExpr)other).operator);
    }

    public boolean replace(FeatureExpression child, FeatureExpression newChild) {
        if(child == this.expr){
            setExpr(newChild);
            return true;
        }
        if(child == this.operator){
            if(newChild instanceof SingleTokenExpr){
                setOperator((SingleTokenExpr)newChild);
                return true;
            }
        }
        return false;
    }
}
