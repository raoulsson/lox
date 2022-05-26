package com.raoulsson.lox;

public class AstStmtExprPrinter implements Stmt.Visitor<String> {

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        return null;
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression expr) {
        return expr.toString();
    }

    @Override
    public String visitPrintStmt(Stmt.Print expr) {
        return expr.toString();
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        return null;
    }

    public String print(Stmt stmt) {
        return stmt.accept(this);
    }

}
