package org.anarres.cpp.featureExpr;

import java.util.LinkedList;
import java.util.List;

public class PrefixExpr extends UnaryExpr {

    public PrefixExpr(SingleTokenExpr operator, FeatureExpression expr) {
        super(expr, operator);
    }

    public List<FeatureExpression> getChildren() {
        List<FeatureExpression> children = new LinkedList<FeatureExpression>();
        children.add(getOperator());
        children.add(getExpr());
        return children;
    }

    @Override
    public String toString() {
        return "" + getOperator() + getExpr();
    }

    public PrefixExpr clone() {
        return new PrefixExpr(getOperator().clone(), getExpr().clone());
    }
}
