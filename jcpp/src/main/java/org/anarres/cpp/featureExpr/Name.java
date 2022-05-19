package org.anarres.cpp.featureExpr;

import org.anarres.cpp.Token;

public class Name extends SingleTokenExpr {

    public Name(Token token) {
        super(token);
    }

    public Name clone() {
        return new Name(getToken());
    }
}
