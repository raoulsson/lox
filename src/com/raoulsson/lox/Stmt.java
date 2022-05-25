package com.raoulsson.lox;

import java.util.List;

// Generated code by com.raoulsson.tools.GenerateAst
/*
The first rule is now program, which is the starting point for
the grammar and represents a complete Lox script or REPL entry.
A program is a list of statements followed by the special 'end
of file' token. The mandatory end token ensures the parser consumes
the entire input and doesn’t silently ignore erroneous unconsumed
tokens at the end of a script.

program     → statement* EOF ;
statement   → exprStmt | printStmt ;
exprStmt    → expression ";" ;
printStmt   → "print" expression ";" ;

There is no place in the grammar where both an expression and a
statement is allowed. The operands of, say, + are always expressions,
never statements. The body of a while loop is always a statement.
*/
public abstract class Stmt {

    /*
     */
    public static class Expression extends Stmt {

        final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

    }

    /*
     */
    public static class Print extends Stmt {

        final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

    }

}
