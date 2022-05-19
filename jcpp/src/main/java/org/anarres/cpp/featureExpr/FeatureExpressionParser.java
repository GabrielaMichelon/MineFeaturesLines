package org.anarres.cpp.featureExpr;

import org.anarres.cpp.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class FeatureExpressionParser {

    private Source source;


    private Token t;

    private Token la;

    public FeatureExpressionParser(Source source) {
        this.source = source;
    }

    public FeatureExpressionParser(List<Token> tokens) {
        this(new FixedTokenSource(tokens));
    }

    public FeatureExpressionParser(String expression) {
        this(new StringLexerSource(expression));
    }

    public FeatureExpression parse() {
        scan();
        return Expr();
    }

    private Token token() {
        try {
            Token tok = source.nextToken();
            while (tok.getType() == Token.WHITESPACE || tok.getType() == Token.CCOMMENT || tok.getType() == Token.CPPCOMMENT) {
                tok = source.nextToken();
            }
            return tok;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LexerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void scan() {
        t = la;
        la = token();
    }

    private void check(int type) {
        if (la.getType() == type) {
            scan();
        } else if(la.getText().contains("b4_location_if") || la.getText().contains("b4_locations_if") || la.getText().contains("<eof>")|| la.getText().contains("b4_api_PREFIX")) {
            System.out.println("!!!!!!");
        }else {
            error("Expected " + type + " but was: " + la);
        }
    }

    private void error(String msg) {
        throw new InternalException(msg);
    }

    private FeatureExpression Expr() {
        return AssignExpr();
    }

    private FeatureExpression AssignExpr() {
        FeatureExpression lhs = CondExpr();
        Token op = AssignOp();
        if (op != null) {
            scan();
            FeatureExpression rhs = CondExpr();
            return new AssignExpr(lhs, new SingleTokenExpr(op), rhs);
        }
        return lhs;
    }

    private Token AssignOp() {
        if (la.getType() == '='
                || la.getType() == Token.PLUS_EQ
                || la.getType() == Token.SUB_EQ
                || la.getType() == Token.MULT_EQ
                || la.getType() == Token.DIV_EQ
                || la.getType() == Token.MOD_EQ
                || la.getType() == Token.AND_EQ
                || la.getType() == Token.OR_EQ
                || la.getType() == Token.XOR_EQ
                || la.getType() == Token.LAND_EQ
                || la.getType() == Token.LOR_EQ
                || la.getType() == Token.LSH_EQ
                || la.getType() == Token.RSH_EQ) {

            return la;
        }
        return null;
    }

    private FeatureExpression CondExpr() {
        FeatureExpression expr = LogOrExpr();
        if (la.getType() == '?') {
            scan();
            FeatureExpression thenExpr = Expr();
            check(':');
            FeatureExpression elseExpr = CondExpr();
            return new CondExpr(expr, thenExpr, elseExpr);
        }
        return expr;
    }

    private FeatureExpression LogOrExpr() {
        FeatureExpression lhs = LogAndExpr();
        while (la.getType() == Token.LOR) {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = LogAndExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression LogAndExpr() {
        FeatureExpression lhs = OrExpr();
        while (la.getType() == Token.LAND) {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = OrExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression OrExpr() {
        FeatureExpression lhs = XorExpr();
        while (la.getType() == '|') {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = XorExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression XorExpr() {
        FeatureExpression lhs = AndExpr();
        while (la.getType() == '^') {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = AndExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression AndExpr() {
        FeatureExpression lhs = EqlExpr();
        while (la.getType() == '&') {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = EqlExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression EqlExpr() {
        FeatureExpression lhs = RelExpr();
        while (la.getType() == Token.EQ || la.getType() == Token.NE) {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = RelExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression RelExpr() {
        FeatureExpression lhs = ShiftExpr();
        while (la.getType() == '<' || la.getType() == '>' || la.getType() == Token.GE || la.getType() == Token.LE) {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = ShiftExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression ShiftExpr() {
        FeatureExpression lhs = AddExpr();
        while (la.getType() == Token.LSH || la.getType() == Token.RSH) {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = AddExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression AddExpr() {
        FeatureExpression lhs = MultExpr();
        while (la.getType() == '+' || la.getType() == '-') {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = MultExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression MultExpr() {
        FeatureExpression lhs = UnaryExpr();
        while (la.getType() == '*' || la.getType() == '/' || la.getType() == '%') {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression rhs = UnaryExpr();
            lhs = new InfixExpr(lhs, op, rhs);
        }
        return lhs;
    }

    private FeatureExpression UnaryExpr() {
        if (la.getType() == Token.INC || la.getType() == Token.DEC) {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression expr = UnaryExpr();
            return new PrefixExpr(op, expr);
        } else if (la.getType() == '&' || la.getType() == '*' || la.getType() == '+' || la.getType() == '-' || la.getType() == '~' || la.getType() == '!') {
            SingleTokenExpr op = new SingleTokenExpr(la);
            scan();
            FeatureExpression expr = UnaryExpr();
            return new PrefixExpr(op, expr);
        } else {
            return PostfixExpr();
        }
    }

    private FeatureExpression PostfixExpr() {
        FeatureExpression lastExpr = Primary();
        if (lastExpr instanceof Name && ((Name) lastExpr).getToken().getText().equals("defined") && la.getType() == Token.IDENTIFIER) {
            MacroCall call = MacroCall(lastExpr);
            lastExpr = call;
        } else {
            while (la.getType() == '[' || la.getType() == ']' || la.getType() == '.' || la.getType() == Token.ARROW || la.getType() == '(' || la.getType() == Token.INC || la.getType() == Token.DEC) {
                if (la.getType() == '[') {
                    scan();
                    FeatureExpression index = Expr();
                    lastExpr = new ArrayAccess(lastExpr, index);
                } else if (la.getType() == ']') {
                    SingleTokenExpr op = new SingleTokenExpr(la);
                    scan();
                    check(Token.IDENTIFIER);
                    lastExpr = new Pointer(lastExpr, op, new Name(t));
                } else if (la.getType() == '.') {
                    SingleTokenExpr op = new SingleTokenExpr(la);
                    scan();
                    check(Token.IDENTIFIER);
                    lastExpr = new Pointer(lastExpr, op, new Name(t));
                } else if (la.getType() == Token.ARROW) {
                    SingleTokenExpr op = new SingleTokenExpr(la);
                    scan();
                    check(Token.IDENTIFIER);
                    lastExpr = new Pointer(lastExpr, op, new Name(t));
                } else if (la.getType() == '(') {
                    scan();
                    MacroCall call = MacroCall(lastExpr);
                    check(')');
                    lastExpr = call;
                } else if (la.getType() == Token.INC) {
                    SingleTokenExpr op = new SingleTokenExpr(la);
                    scan();
                    lastExpr = new PostfixExpr(lastExpr, op);
                } else if (la.getType() == Token.DEC) {
                    SingleTokenExpr op = new SingleTokenExpr(la);
                    scan();
                    lastExpr = new PostfixExpr(lastExpr, op);
                } else {
                    System.err.println("ERROR");
                }
            }
        }
        return lastExpr;
    }


    private FeatureExpression Primary() {
        if (la.getType() == Token.IDENTIFIER) {
            scan();
            return new Name(t);
        } else if (la.getType() == Token.NUMBER) {
            scan();
            return new NumberLiteral(t);
        } else if (la.getType() == Token.CHARACTER) {
            scan();
            return new CharacterLiteral(t);
        } else if (la.getType() == Token.STRING) {
            scan();
            return new StringLiteral(t);
        } else if (la.getType() == '(') {
            scan();
            FeatureExpression expr = Expr();
            check(')');
            return new ParenthesizedExpr(expr);
        } else if (la.getType() == ')') {
            scan();
            return new CharacterLiteral(t);
        } else if (la.getType() == ']') {
            scan();
            return new Name(t);
        } else if (la.getType() == '[') {
            scan();
            return new Name(t);
        }
        error("Expected Primary but was: " + la);
        return null;
    }

    private MacroCall MacroCall(FeatureExpression name) {
        Name n = (Name) name;
        List<FeatureExpression> arguments = new LinkedList<FeatureExpression>();
        arguments.add(AssignExpr());
        while (la.getType() == ',') {
            scan();
            arguments.add(AssignExpr());
        }
        return new MacroCall(n, arguments);
    }

}