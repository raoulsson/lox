package com.raoulsson.tools;

import com.raoulsson.lox.*;

public class TestRunAstPrinter {

    public static void main(String[] args) {
        // -123 * (45.67)
       Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                    new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(new Expr.Literal(45.67)));
        System.out.println(new AstPrinter().print(expression));
    }
}
