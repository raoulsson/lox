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
    /*
    The Visitor pattern is the most widely misunderstood pattern in all of Design
    Patterns, which is really saying something when you look at the software architecture
    excesses of the past couple of decades.
    The trouble starts with terminology. The pattern isn’t about “visiting” and the “accept”
     method in it doesn’t conjure up any helpful imagery either. Many think the pattern has to
    do with traversing trees, which isn’t the case at all. We are going to use it on a set of
    classes that are tree-like, but that’s a coincidence. As you’ll see, the pattern works as well on a
    single object. The Visitor pattern is really about approximating the functional style within an OOP
    language. It lets us add new columns to that table easily. We can define all of the behavior
    for a new operation on a set of types in one place, without having to touch the types themselves.
    It does this the same way we solve almost every problem in computer science: by adding a layer
    of indirection.
    */
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
