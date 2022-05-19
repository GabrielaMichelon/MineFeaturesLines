package org.anarres.cpp.featureExpr;

import java.util.LinkedList;
import java.util.List;

public class CondExpr extends FeatureExpression{

    private FeatureExpression expr;

    private FeatureExpression thenExpr;

    private FeatureExpression elseExpr;

    public CondExpr(FeatureExpression expr, FeatureExpression thenExpr, FeatureExpression elseExpr) {
        super();
        setExpr(expr);
        setThenExpr(thenExpr);
        setElseExpr(elseExpr);
    }

    public FeatureExpression getExpr() {
        return expr;
    }

    public FeatureExpression getThenExpr() {
        return thenExpr;
    }

    public FeatureExpression getElseExpr() {
        return elseExpr;
    }

    public void setExpr(FeatureExpression expr) {
        this.expr = expr;
        this.expr.setParent(this);
    }

    public void setThenExpr(FeatureExpression thenExpr) {
        this.thenExpr = thenExpr;
        this.thenExpr.setParent(this);
    }

    public void setElseExpr(FeatureExpression elseExpr) {
        this.elseExpr = elseExpr;
        this.elseExpr.setParent(this);
    }

    public List<FeatureExpression> getChildren() {
        List<FeatureExpression> children = new LinkedList<FeatureExpression>();
        children.add(expr);
        children.add(thenExpr);
        children.add(elseExpr);
        return children;
    }

    @Override
    public String toString() {
        return expr.toString() + " ? " + thenExpr.toString() + " : " + elseExpr.toString();
    }

    public boolean equals(FeatureExpression other) {
        if(other == this){
            return true;
        }
        if(!(other instanceof CondExpr)){
            return false;
        }
        return this.expr.equals(((CondExpr)other).expr)
                && this.thenExpr.equals(((CondExpr)other).thenExpr)
                && this.elseExpr.equals(((CondExpr)other).elseExpr);
    }

    public CondExpr clone() {
        return new CondExpr(expr.clone(), thenExpr.clone(), elseExpr.clone());
    }

    public boolean replace(FeatureExpression child, FeatureExpression newChild) {
        if(child == this.expr){
            setExpr(newChild);
            return true;
        }
        if(child == this.thenExpr){
            setThenExpr((SingleTokenExpr)newChild);
            return true;
        }
        if(child == this.elseExpr){
            setElseExpr(newChild);
            return true;
        }
        return false;
    }
}
