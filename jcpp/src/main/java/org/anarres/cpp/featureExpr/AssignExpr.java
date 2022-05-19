package org.anarres.cpp.featureExpr;

import java.util.LinkedList;
import java.util.List;

public class AssignExpr extends FeatureExpression {

    private FeatureExpression leftHandSide;

    private SingleTokenExpr operator;

    private FeatureExpression rightHandSide;

    public AssignExpr(FeatureExpression leftHandSide, SingleTokenExpr operator, FeatureExpression rightHandSide) {
        super();
        setLeftHandSide(leftHandSide);
        setOperator(operator);
        setRightHandSide(rightHandSide);
    }

    public FeatureExpression getLeftHandSide() {
        return leftHandSide;
    }

    public SingleTokenExpr getOperator() {
        return operator;
    }

    public FeatureExpression getRightHandSide() {
        return rightHandSide;
    }

    public void setLeftHandSide(FeatureExpression leftHandSide) {
        this.leftHandSide = leftHandSide;
        this.leftHandSide.setParent(this);
    }

    public void setOperator(SingleTokenExpr operator) {
        this.operator = operator;
        this.operator.setParent(this);
    }

    public void setRightHandSide(FeatureExpression rightHandSide) {
        this.rightHandSide = rightHandSide;
        this.rightHandSide.setParent(this);
    }

    public List<FeatureExpression> getChildren() {
        List<FeatureExpression> children = new LinkedList<FeatureExpression>();
        children.add(leftHandSide);
        children.add(operator);
        children.add(rightHandSide);
        return children;
    }

    @Override
    public String toString() {
        return leftHandSide + " " + operator + " " + rightHandSide;
    }

    public boolean equals(FeatureExpression other) {
        if(other == this){
            return true;
        }
        if(!(other instanceof AssignExpr)){
            return false;
        }
        return this.leftHandSide.equals(((AssignExpr)other).leftHandSide)
                && this.operator.equals(((AssignExpr)other).operator)
                && this.rightHandSide.equals(((AssignExpr)other).rightHandSide);
    }

    public AssignExpr clone() {
        return new AssignExpr(leftHandSide.clone(), operator.clone(), rightHandSide.clone());
    }

    public boolean replace(FeatureExpression child, FeatureExpression newChild) {
        if(child == this.leftHandSide){
            setLeftHandSide(newChild);
            return true;
        }
        if(child == this.operator){
            if(newChild instanceof SingleTokenExpr){
                setOperator((SingleTokenExpr)newChild);
                return true;
            }
        }
        if(child == this.rightHandSide){
            setRightHandSide(newChild);
            return true;
        }
        return false;
    }
}
