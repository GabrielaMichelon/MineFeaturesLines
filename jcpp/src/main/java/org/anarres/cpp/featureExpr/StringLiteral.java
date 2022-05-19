package org.anarres.cpp.featureExpr;

import org.anarres.cpp.Token;

public class StringLiteral extends SingleTokenExpr {

    public StringLiteral(Token token) {
        super(token);
    }

    public StringLiteral clone() {
        return new StringLiteral(getToken());
    }
}
