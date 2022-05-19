package org.anarres.cpp.featureExpr;

import java.util.LinkedList;
import java.util.List;

public class ParenthesizedExpr extends FeatureExpression {

    private FeatureExpression expr;

    public ParenthesizedExpr(FeatureExpression expr) {
        super();
        setExpr(expr);
    }

    public List<FeatureExpression> getChildren() {
        List<FeatureExpression> children = new LinkedList<FeatureExpression>();
        children.add(expr);
        return children;
    }

    public void setExpr(FeatureExpression expr) {
        this.expr = expr;
        this.expr.setParent(this);
    }

    public FeatureExpression getExpr() {
        return expr;
    }

    @Override
    public String toString() {
        return "(" + expr + ")";
    }

    public boolean equals(FeatureExpression other) {
        if(other == this){
            return true;
        }
        if(!(other instanceof ParenthesizedExpr)){
            return false;
        }
        return this.expr.equals(((ParenthesizedExpr)other).expr);
    }

    public ParenthesizedExpr clone() {
        return new ParenthesizedExpr(expr.clone());
    }

    public boolean replace(FeatureExpression child, FeatureExpression newChild) {
        if(child == this.expr){
            setExpr(newChild);
            return true;
        }
        return false;
    }
}
