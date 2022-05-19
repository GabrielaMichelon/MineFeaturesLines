package org.anarres.cpp.featureExpr;

import org.anarres.cpp.Token;

public class CharacterLiteral extends SingleTokenExpr {

    public CharacterLiteral(Token token) {
        super(token);
    }

    public CharacterLiteral clone() {
        return new CharacterLiteral(getToken());
    }
}
