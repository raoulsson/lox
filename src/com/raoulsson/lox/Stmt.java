package com.raoulsson.lox;

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

State and statements go hand in hand. Since statements, by definition,
don’t evaluate to a value, they need to do something else to be useful.
That something is called a side effect. It could mean producing
user-visible output or modifying some state in the interpreter that can
be detected later. The latter makes them a great fit for defining
variables or other named entities.
*/
public abstract class Stmt {

    /*
     */
    abstract <R> R accept(Visitor<R> visitor);

    /*
    Any class that needs to operate on the Stmt data, can implement the Visitor<R>
    interface. Currently: Interpreter

    With method overloading, we would only specify

        R visit(Expr expr)

    and do the dispatching within this method (ExpressionStatement, PrintStatement, ...)
    */
    interface Visitor<R> {
        R visitExpressionStmt(Expression stmt);

        R visitPrintStmt(Print stmt);
    }

    /*
    An expression statement lets you place an expression where a
    statement is expected. They exist to evaluate expressions that
    have side effects. You may not notice them, but you use them
    all the time in C, Java, and other languages. Any time you see
    a function or method call followed by a ;, you’re looking at an
    expression statement.
    */
    public static class Expression extends Stmt {

        final Expr expression;

        public Expression(Expr expression) {
            this.expression = expression;
        }

        /*
        We have no clue who the visitor is, but we accept him and give ourselves to him.
        */
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }

        /*
        Intermediate impl
        */
        @Override
        public String toString() {
            return new AstPrinter().print(expression);
        }

    }

    /*
    A print statement evaluates an expression and displays the result
    to the user. I admit it’s weird to bake printing right into the
    language instead of making it a library function. Doing so is a
    concession to the fact that we’re building this interpreter one
    chapter at a time and want to be able to play with it before it’s
    all done. To make print a library function, we’d have to wait until
    we had all of the machinery for defining and calling functions before
    we could witness any side effects.
    */
    public static class Print extends Stmt {

        final Expr expression;

        public Print(Expr expression) {
            this.expression = expression;
        }

        /*
        We have no clue who the visitor is, but we accept him and give ourselves to him.
        */
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }

        /*
        Intermediate impl
        */
        @Override
        public String toString() {
            return new AstPrinter().print(expression);
        }

    }

}
