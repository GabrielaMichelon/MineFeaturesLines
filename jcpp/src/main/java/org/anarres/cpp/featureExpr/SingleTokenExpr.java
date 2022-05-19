package org.anarres.cpp.featureExpr;

import org.anarres.cpp.Token;

import java.util.LinkedList;
import java.util.List;

public class SingleTokenExpr extends FeatureExpression {

    private final Token token;

    public SingleTokenExpr(Token token) {
        super();
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public List<FeatureExpression> getChildren() {
        return new LinkedList<FeatureExpression>();
    }

    public String toString() {
        return token.getText();
    }

    public boolean equals(FeatureExpression other) {
        if(other == this){
            return true;
        }
        if(!(other instanceof SingleTokenExpr)){
            return false;
        }
        return this.token.getText().equals(((SingleTokenExpr)other).token.getText());
    }

    public SingleTokenExpr clone() {
        return new SingleTokenExpr(token);
    }

    public boolean replace(FeatureExpression child, FeatureExpression newChild) {
        return false;
    }
}
