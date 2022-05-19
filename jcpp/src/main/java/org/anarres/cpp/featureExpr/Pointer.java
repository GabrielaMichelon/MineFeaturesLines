package org.anarres.cpp.featureExpr;

import java.util.LinkedList;
import java.util.List;

public class Pointer extends FeatureExpression {

    private FeatureExpression pointer;

    private SingleTokenExpr operator;

    private Name pointee;

    public Pointer(FeatureExpression pointer, SingleTokenExpr operator, Name pointee) {
        super();
        setPointer(pointer);
        setOperator(operator);
        setPointee(pointee);
    }

    public FeatureExpression getPointer() {
        return pointer;
    }

    public void setPointer(FeatureExpression pointer) {
        this.pointer = pointer;
        this.pointer.setParent(this);
    }

    public SingleTokenExpr getOperator() {
        return operator;
    }

    public void setOperator(SingleTokenExpr operator) {
        this.operator = operator;
        this.operator.setParent(this);
    }

    public Name getPointee() {
        return pointee;
    }

    public void setPointee(Name pointee) {
        this.pointee = pointee;
        this.pointee.setParent(this);
    }

    public List<FeatureExpression> getChildren() {
        List<FeatureExpression> children = new LinkedList<FeatureExpression>();
        children.add(pointer);
        children.add(operator);
        children.add(pointee);
        return children;
    }

    public String toString() {
        return pointer + "" + operator + "" + pointee;
    }

    public boolean equals(FeatureExpression other) {
        if(other == this){
            return true;
        }
        if(!(other instanceof Pointer)){
            return false;
        }
        return this.pointer.equals(((Pointer)other).pointer)
                && this.operator.equals(((Pointer)other).operator)
                && this.pointee.equals(((Pointer)other).pointee);
    }

    public Pointer clone() {
        return new Pointer(pointer.clone(), operator.clone(), pointee.clone());
    }

    public boolean replace(FeatureExpression child, FeatureExpression newChild) {
        if(child == this.pointer){
            setPointer(newChild);
            return true;
        }
        if(child == this.operator){
            if(newChild instanceof SingleTokenExpr){
                setOperator((SingleTokenExpr)newChild);
                return true;
            }
        }
        if(child == this.pointee){
            if(newChild instanceof Name){
                setPointee((Name)newChild);
                return true;
            }
        }
        return false;
    }
}
