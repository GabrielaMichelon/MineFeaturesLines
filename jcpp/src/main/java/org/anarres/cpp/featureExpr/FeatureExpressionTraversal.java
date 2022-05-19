package org.anarres.cpp.featureExpr;

import java.util.HashSet;
import java.util.Set;

public abstract class FeatureExpressionTraversal {

    private Set<FeatureExpression> skipChildren;

    public FeatureExpressionTraversal() {
        this.skipChildren = new HashSet<FeatureExpression>();
    }

    protected void skipChildren(FeatureExpression expr){
        this.skipChildren.add(expr);
    }

    protected boolean doSkipChildren(FeatureExpression expr){
        return this.skipChildren.contains(expr);
    }

    public abstract void preVisit(FeatureExpression expr);

    public abstract void postVisit(FeatureExpression expr);
}
