package org.anarres.cpp.featureExpr;

import org.anarres.cpp.Token;

public class NumberLiteral extends SingleTokenExpr {

    public NumberLiteral(Token token) {
        super(token);
    }

    public NumberLiteral clone() {
        return new NumberLiteral(getToken());
    }
}
