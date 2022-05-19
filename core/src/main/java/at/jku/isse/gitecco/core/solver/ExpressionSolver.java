package at.jku.isse.gitecco.core.solver;

import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import org.anarres.cpp.Token;
import org.anarres.cpp.featureExpr.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.nary.cnf.LogOp;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;

import java.util.*;

/**
 * Class for finding positive solutions for preprocessor conditions
 */
public class ExpressionSolver {
    private String expr;
    private Model model;
    private final List<IntVar> vars;
    private final Stack<Variable> stack;
    private boolean isIntVar = false;

    /**
     * Create new solver with a given expression to solve.
     *
     * @param expr
     */
    public ExpressionSolver(String expr) {
        this.expr = expr;
        this.model = new Model();
        this.vars = new LinkedList<>();
        this.stack = new Stack<>();
    }

    /**
     * Create new empty solver.
     */
    public ExpressionSolver() {
        this.expr = "";
        this.model = new Model();
        this.vars = new LinkedList<>();
        this.stack = new Stack<>();
    }

    /**
     * Resets the solver so a new expression can be solved.
     */
    public void reset() {
        this.expr = "";
        this.model = new Model();
        this.vars.clear();
        this.stack.clear();
    }

    /**
     * Resets the solver and assigns a new expression to solve.
     *
     * @param expr
     */
    public void reset(String expr) {
        this.expr = expr;
        this.model = new Model();
        this.vars.clear();
        this.stack.clear();
    }

    /**
     * Sets a new expression for the solver.
     *
     * @param expr
     */
    public void setExpr(String expr) {
        this.expr = expr;
    }

    public Model getModel() {
        return this.model;
    }

    /**
     * Solves the expression currently assigned to this solver.
     * Returns a Map with the Feature as key and the value to be assigned as an Integer.
     *
     * @return The Map with the solution.
     */
    public Map<Feature, Integer> solve() {
        Map<Feature, Integer> assignments = new HashMap<>();

        if (this.expr.contains("'A' == '\\301'"))
            this.expr = this.expr.replace("'A' == '\\301'", "A == 301");
        if (this.expr.contains("__has_feature(address_sanitizer)"))
            this.expr = this.expr.replace("__has_feature(address_sanitizer)", "__has_feature)");
        if (this.expr.contains("QT_VERSION_CHECK(5, 0, 0)"))
            this.expr = this.expr.replace("QT_VERSION_CHECK(5, 0, 0)", "QT_VERSION_CHECK");
        if (this.expr.contains("'$' == 0x24 && '@' == 0x40 && '`' == 0x60 && '~' == 0x7e"))
            return null;
        //add the parsed problem to the solver model
        model.post(getBoolVarFromExpr(this.expr).extension());

        //acutal solving
        Solution solution = model.getSolver().findSolution();
        if (solution != null) {
            for (IntVar var : vars) {
                try {
                    assignments.put(new Feature(var.getName()), solution.getIntVal(var));
                } catch (SolverException se) {
                    System.out.println("var name: " + var.getName() + " exception:" + se);
                }
            }
        } else {
            //System.err.println("DEAD CODE: No solution found for " + expr);
            return null;
        }

        return Collections.unmodifiableMap(assignments);
    }

    /**
     * Builds a constraint from the given set of FeatureImplications for one feature.
     * This will build a chain of if then else expressions for the solver model.
     *
     * @param feature
     * @param implications
     */
    public void addClause(Feature feature, Queue<FeatureImplication> implications) {
        LogOp elsePart = null;
        //String constraint = "";

        while (!implications.isEmpty()) {
            FeatureImplication im = implications.remove();
            BoolVar ifPart = getBoolVarFromExpr(im.getCondition());
            BoolVar thenPart = getBoolVarFromExpr(im.getValue());

            if (elsePart == null) {
                elsePart = LogOp.ifThenElse(ifPart, thenPart, getBoolVarFromExpr(feature.getName()).not());
                //constraint += "if " + im.getCondition() + " then " + thenPart + " else (" ;
            } else {
                elsePart = LogOp.ifThenElse(ifPart, thenPart, elsePart);
                //constraint += "if " + im.getCondition() + " then " + im.getValue() + " else !( " + im.getValue() + ")";

            }
        }
        if (elsePart != null) model.addClauses(elsePart);
        //System.out.println(constraint);
    }

    /**
     * Helper method for parsing and traversing an expression given as String
     * If the last variable on the stack is not a boolean variable
     * we can assume the condition string was only a single number literal.
     * Thus we can translate this to a boolean variable just like the C preprocessor --> > 0 true else false.
     *
     * @param expr
     * @return
     */
    public BoolVar getBoolVarFromExpr(String expr) {
        isIntVar = false;
        if (expr.contains(" - 0"))
            expr = expr.replace(" - 0", "");
        if ((expr.contains("(])")))
            expr = expr.replace("(]) &&", "b4_location_if &&");
        if (expr.contains("]") && !expr.contains("[")) {
            expr = expr.replace("]", "b4_location_if");
        }
        if (expr.contains("b4_locations_if")) {
            expr = expr.replace("[", "");
            expr = expr.replace("]", "");
        }
        if (expr.contains("GLIB_CHECK_VERSION(")) {
            expr = "GLIB_CHECK_VERSION";
        }
        //System.out.println(expr);
        traverse(new FeatureExpressionParser(expr).parse());
        Variable var = stack.pop();

        return var.asBoolVar();
    }

    /**
     * Helper Method
     * Traverses the expression tree which was created before by the FeatureExpressionParser.
     *
     * @param expr the expression tree to be parsed.
     */
    public void traverse(FeatureExpression expr) {
        try {
            if (expr == null) return;
            ExpressionSolver ex = new ExpressionSolver();
            BoolVar boolVar;
            if (expr instanceof Name) {
                String name = ((Name) expr).getToken().getText();
                Variable check = checkVars(model, name);
                if (check == null) {
                    if (isIntVar) {
                        IntVar iv = model.intVar(name, Short.MIN_VALUE, Short.MAX_VALUE);
                        vars.add(iv);
                        stack.push(iv);
                    } else {
                        IntVar iv = model.intVar(name, 0, 1);
                        BoolVar bv = iv.ne(0).boolVar();
                        vars.add(iv);
                        stack.push(bv);
                    }
                } else {
                    if (check instanceof IntVar && !isIntVar) {
                        stack.push(((IntVar) check).ne(0).boolVar());
                    } else {
                        stack.push(check);
                    }
                }
            } else if (expr instanceof AssignExpr) {
                System.err.println("AssignExpr should not appear in a normal condition!");
            } else if (expr instanceof NumberLiteral) {
                try {
                    //contains little workaround for marlin Long number notation 160000L
                    if (((NumberLiteral) expr).getToken().getText().equals("2147483647"))
                        stack.push(model.intVar(Short.MIN_VALUE, Short.MAX_VALUE));
                    else if (((NumberLiteral) expr).getToken().getText().equals("10701UL"))
                        stack.push(model.intVar(Short.MIN_VALUE, Short.MAX_VALUE));
                    else
                        stack.push(model.intVar(Double.valueOf((((NumberLiteral) expr).getToken().getText().replaceAll("L", ""))).intValue()));
                } catch (NumberFormatException e) {
                    try {
                        stack.push(model.intVar(Long.decode((((NumberLiteral) expr).getToken().getText().replaceAll("L", ""))).intValue()));
                    } catch (NumberFormatException e1) {
                        System.err.println("the given number format is not compatible with the solver!" +
                                "\n number: " + ((NumberLiteral) expr).getToken().getText());
                        stack.push(model.intVar(Long.decode((((NumberLiteral) expr).getToken().getText().replaceAll("0000UL", ""))).intValue()));
                    }
                }
                isIntVar = true;
            } else if (expr instanceof SingleTokenExpr) {
                IntVar right;
                IntVar left;
                BoolVar bright;
                BoolVar bleft;
                SingleTokenExpr e = (SingleTokenExpr) expr;
                switch (e.getToken().getType()) {
                    case Token.GE:      //greater than or equal ">="
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.ge(right).boolVar());
                        break;
                    case Token.EQ:      //equal "=="
                        if (isIntVar) {
                            right = stack.pop().asIntVar();
                            left = stack.pop().asIntVar();
                            stack.push(left.eq(right).intVar());
                        } else {
                            bright = stack.pop().asBoolVar();
                            bleft = stack.pop().asBoolVar();
                            stack.push(bleft.eq(bright).boolVar());
                        }
                        break;
                    case Token.LE:      //less than or equal "<="
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.le(right).boolVar());
                        break;
                    case Token.LOR:     //logical or "||"
                        bright = stack.pop().asBoolVar();
                        bleft = stack.pop().asBoolVar();
                        stack.push(bleft.or(bright).boolVar());
                        break;
                    case Token.LAND:    //logical and "&&
                        bright = stack.pop().asBoolVar();
                        bleft = stack.pop().asBoolVar();
                        stack.push(bleft.and(bright).boolVar());
                        break;
                    case Token.NE:      //not equal "!="
                        if (isIntVar) {
                            right = stack.pop().asIntVar();
                            left = stack.pop().asIntVar();
                            stack.push(left.ne(right).intVar());
                        } else {
                            bright = stack.pop().asBoolVar();
                            bleft = stack.pop().asBoolVar();
                            stack.push(bleft.ne(bright).boolVar());
                        }
                        break;
                    case 60:            //less than "<"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.lt(right).boolVar());
                        break;
                    case 62:            //greater than ">"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.gt(right).boolVar());
                        break;
                    case 33:            //not "!"
                        bright = stack.pop().asBoolVar();
                        stack.push(bright.not());
                        break;
                    case 43:            //plus "+"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.add(right).intVar());
                        break;
                    case 45:            //minus "-"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.intVar());
                        stack.push(left.sub(right).intVar());
                        break;
                    case 42:            //mul "*"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.mul(right).intVar());
                        break;
                    case 47:            //div "/"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.div(right).intVar());
                        break;
                    case 37:            //modulo "%"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.mod(right).intVar());
                        break;
                    case 94:            //pow "^"
                        right = stack.pop().asIntVar();
                        left = stack.pop().asIntVar();
                        stack.push(left.pow(right).intVar());
                        break;
                    case 124:     // "|"
                        bright = stack.pop().asBoolVar();
                        bleft = stack.pop().asBoolVar();
                        stack.push(bleft.or(bright).boolVar());
                        break;
                    case 135:     // "]"
                        bright = stack.pop().asBoolVar();
                        break;
                    default:
                        System.err.println("unexpected token with token id: " + e.getToken().getType() + " and symbol: " + e.getToken().getText());
                        break;
                }
            } else if (expr instanceof CondExpr) {
                CondExpr e = (CondExpr) expr;
                //idea: parse that created expression and attach it instead of the CondExpr and continue to traverse again.
                String cond = "(!(" + e.getExpr() + ")||(" + e.getThenExpr() + "))&&((" + e.getExpr() + ")||(" + e.getElseExpr() + "))";
                traverse(new FeatureExpressionParser(cond).parse());
            } else if (expr instanceof PrefixExpr) {
                isIntVar = false;
                traverse(((PrefixExpr) expr).getExpr());
                if (((PrefixExpr) expr).getOperator().getToken().getType() == 45) {
                    IntVar ivar = stack.pop().asIntVar();
                    ivar.mul(-1);
                    stack.push(ivar);
                } else {
                    traverse(((PrefixExpr) expr).getOperator());
                }
            } else if (expr instanceof InfixExpr) {
                checkIntVar((InfixExpr) expr);
                traverse(((InfixExpr) expr).getLeftHandSide());
                checkIntVar((InfixExpr) expr);
                traverse(((InfixExpr) expr).getRightHandSide());
                checkIntVar((InfixExpr) expr);
                traverse(((InfixExpr) expr).getOperator());
            } else if (expr instanceof ParenthesizedExpr) {
                isIntVar = false;
                traverse(((ParenthesizedExpr) expr).getExpr());
            } else {
                System.err.println("unexpected node in AST: " + expr.toString() + " " + expr.getClass());
            }
        } catch (EmptyStackException emptyStackException) {
            System.out.println(this.expr);
        }
    }

    /**
     * Helper method: checks if an IntVar is needed or if a BoolVar is fine.
     * Sets the IntVar flag in case it is needed.
     *
     * @param expr
     */
    public void checkIntVar(InfixExpr expr) {
        int op = expr.getOperator().getToken().getType();

        if (op == 43 || op == 45 || op == 42 || op == 47 || op == 37 //266 == wirklich notwendig?
                || op == 94 || op == 267 || op == 266 || op == 283 || op == 60 || op == 62 || op == 275)
            isIntVar = true;
        else isIntVar = false;
    }

    public List<IntVar> getVars() {
        return vars;
    }


    /**
     * Helper Method
     * Checks if a variable with a given name exists already in a given model.
     * If it does the variable is returned. Otherwise null is returned.
     *
     * @param model
     * @param name
     * @return
     */
    public Variable checkVars(Model model, String name) {
        for (Variable var : model.getVars()) {
            if (var.getName().equals(name)) return var;
        }
        return null;
    }


}