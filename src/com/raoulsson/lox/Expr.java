package com.raoulsson.lox;

import java.util.List;

// Generated code by com.raoulsson.tools.GenerateAst
/*
expression  → literal | unary | binary | grouping ;
*/
public abstract class Expr {

    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
    }

    abstract <R> R accept(Visitor<R> visitor);

	/*
	Binary → expression operator expression ;
	*/
    public static class Binary extends Expr {

        final Expr left;
        final Token operator;
        final Expr right;

        public Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }

    }

	/*
	Grouping → "(" expression ")" ;
	*/
    public static class Grouping extends Expr {

        final Expr expression;

        public Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

    }

	/*
	Literal → NUMBER | STRING | "true" | "false" | "nil" ;
	*/
    public static class Literal extends Expr {

        final Object value;

        public Literal(Object value) {
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

    }

	/*
	Unary → ("-"|"!")expression ;
	*/
    public static class Unary extends Expr {

        final Token operator;
        final Expr right;

        public Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

    }

}
