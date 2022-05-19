package org.anarres.cpp.featureExpr;

import java.util.LinkedList;
import java.util.List;

public class ArrayAccess extends FeatureExpression {

    private FeatureExpression array;

    private FeatureExpression index;

    public ArrayAccess(FeatureExpression array, FeatureExpression index) {
        setArray(array);
        setIndex(index);
    }

    public FeatureExpression getArray() {
        return array;
    }

    public void setArray(FeatureExpression array) {
        this.array = array;
        this.array.setParent(this);
    }

    public FeatureExpression getIndex() {
        return index;
    }

    public void setIndex(FeatureExpression index) {
        this.index = index;
        this.index.setParent(this);
    }

    public List<FeatureExpression> getChildren() {
        List<FeatureExpression> children = new LinkedList<FeatureExpression>();
        children.add(array);
        children.add(index);
        return children;
    }

    public String toString() {
        return array + "[" + index + "]";
    }

    public boolean equals(FeatureExpression other) {
        if(other == this){
            return true;
        }
        if(!(other instanceof ArrayAccess)){
            return false;
        }
        return this.array.equals(((ArrayAccess)other).array)
                && this.index.equals(((ArrayAccess)other).index);
    }

    public ArrayAccess clone() {
        return new ArrayAccess(array.clone(), index.clone());
    }

    public boolean replace(FeatureExpression child, FeatureExpression newChild) {
        if(child == this.array){
            setArray(newChild);
            return true;
        }
        if(child == this.index){
            setIndex(newChild);
            return true;
        }
        return false;
    }
}
