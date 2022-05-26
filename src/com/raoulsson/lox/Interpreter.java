package com.raoulsson.lox;

import java.util.List;

/*
In Lox, values are created by literals, computed by expressions,
and stored in variables. The user sees these as Lox objects, but
they are implemented in the underlying language our interpreter
is written in. That means bridging the lands of Lox’s dynamic
typing and Java’s static types. A variable in Lox can store a
value of any (Lox) type, and can even store values of different
types at different points in time.

    Lox type            Java representation

    Any Lox value       Object
    nil                 null
    Boolean             Boolean
    number              Double
    string              String

Our parser can now produce statement syntax trees, so the
next and final step is to interpret them. As in expressions,
we use the Visitor pattern, but we have a new visitor interface,
Stmt.Visitor, to implement since statements have their own base
class.

Unlike expressions, statements produce no values, so the return
type of the visit methods is Void, not Object.
 */
public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    /*
    The (new) entry point to Interpreter.
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                System.out.print(new AstStmtExprPrinter().print(statement) + " → ");   // Until Chapter 8
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /*
    That’s the statement analogue to the evaluate() method we have
    for expressions.
     */
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /*
    The (old) entry point to Interpreter.

    The Interpreter’s public API is simply one method. This
    takes in a syntax tree for an expression and evaluates it.
    If that succeeds, evaluate() returns an object for the
    result value. interpret() converts that to a string and
    shows it to the user.

    Chapter 7: Our interpreter is able to visit statements now,
    but we have some work to do to feed them to it...

    We rename it to interpretExpr.
     */
    void interpretExpr(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    /*
    To convert a Lox value to a string.

    This is another of those pieces of code like isTruthy()
    that crosses the membrane between the user’s view of Lox
    objects and their internal representation in Java.
     */
    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    /*
    The leaves of an expression tree—the atomic bits of syntax
    that all other expressions are composed of—are literals.
    Literals are almost values already, but the distinction is
    important. A literal is a bit of syntax that produces a value.
    A literal always appears somewhere in the user’s source code.
    Lots of values are produced by computation and don’t exist
    anywhere in the code itself. Those aren’t literals. A literal
    comes from the parser’s domain. Values are an interpreter
    concept, part of the runtime’s world.

    So, much like we converted a literal token into a literal
    syntax tree node in the parser, now we convert the literal
    tree node into a runtime value. That turns out to be trivial.
     */
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    /*
    The next simplest node to evaluate is grouping—the node
    you get as a result of using explicit parentheses in an
    expression.

    A grouping node has a reference to an inner node for the
    expression contained inside the parentheses. To evaluate
    the grouping expression itself, we recursively evaluate
    that subexpression and return it.
     */
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    /*
    Like grouping, unary expressions have a single subexpression
    that we must evaluate first. The difference is that the unary
    expression itself does a little work afterwards.

    First, we evaluate the operand expression. Then we apply the
    unary operator itself to the result of that. There are two
    different unary expressions, identified by the type of the
    operator token.

    Shown here is -, which negates the result of the subexpression.
    The subexpression must be a number. Since we don’t statically
    know that in Java, we cast it before performing the operation.
    This type cast happens at runtime when the - is evaluated.

    That’s the core of what makes a language dynamically typed right
    there. You can start to see how evaluation recursively traverses
    the tree. We can’t evaluate the unary operator itself until after
    we evaluate its operand subexpression. That means our interpreter
    is doing a post-order traversal—each node evaluates its children
    before doing its own work.
    */
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable
        return null;
    }

    /*
    This simply forwards to the environment which does the heavy
    lifting to make sure the variable is defined. With that, we’ve
    got rudimentary variables working. Try this out:

        var a = 1;
        var b = 2;
        print a + b;

    We can’t reuse code yet, but we can start to build up programs
    that reuse data.
     */
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    /*
    For obvious reasons, it’s similar to variable declaration.
    It evaluates the right- hand side to get the value, then
    stores it in the named variable. Instead of using define()
    on Environment, it calls assign().

    The last thing the visit() method does is return the assigned
    value. That’s because assignment is an expression that can be
    nested inside other expressions, like so:

        var a = 1;
        print a = 2; // "2".

    Our interpreter can now create, read, and modify variables. It’s
    about as sophisticated as early BASICs. Global variables are simple,
    but writing a large program when any two chunks of code can
    accidentally step on each other’s state is no fun. We want local
    variables, which means it’s time for scope.
     */
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    /*
        The last expression tree class, binary operators.

        The + operator can also be used to concatenate two
        strings. To handle that, we don’t just assume the
        operands are a certain type and cast them, we
        dynamically check the type and choose the appropriate
        operation. This is why we need our object representation
        to support instanceof.
         */
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperand(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperand(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperand(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperand(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return left + (String) right;
                }
                /*
                checkNumberOperand ?

                Since + is overloaded for numbers and strings, it already has code to
                check the types. All we need to do is fail if neither of the two success
                cases match.
                */
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperand(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperand(expr.operator, left, right);
                return (double) left * (double) right;
        }

        // Unreachable
        return null;
    }

    /*
    We rely on this helper method which simply sends
    the expression back into the interpreter’s visitor
    implementation.
     */
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /*
    Lox follows Ruby’s simple rule: false and nil are
    falsey and everything else is truthy.
     */
    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof Boolean) {
            return (boolean) object;
        }
        return true;
    }

    /*
    Unlike the comparison operators which require numbers,
    the equality operators support operands of any type,
    even mixed ones. You can’t ask Lox if 3 is less than
    "three", but you can ask if it’s equal to it.
     */
    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null) {
            return false;
        }
        return left.equals(right);
    }

    /*
    Checks to see if the operator of an arithmetic operation is a number.
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operand must be a number");
    }

    /*
    Same as above but for two operands.
     */
    private void checkNumberOperand(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    /*
    To execute a block, we create a new environment for the block’s scope
    and pass it off to this other method.

    This new method executes a list of statements in the context of a
    given environment. Up until now, the environment field in Interpreter
    always pointed to the same environment—the global one. Now, that field
    represents the current environment. That’s the environment that
    corresponds to the innermost scope containing the code to be executed.

    To execute code within a given scope, this method updates the interpreter’s
    environment field, visits all of the statements, and then restores the
    previous value. As is always good practice in Java, it restores the previous
    environment using a finally clause. That way it gets restored even if an
    exception is thrown.

    Surprisingly, that’s all we need to do in order to fully support local
    variables, nesting, and shadowing.
     */
    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    /*
        We evaluate the inner expression using our existing evaluate() method and
        discard the value. Then we return null. Java requires that to satisfy the
        special capitalized Void return type. Weird, but what can you do?
         */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    /*
    The print statement’s visit method isn’t much different.
    Before discarding the expression’s value, we convert it to
    a string using the stringify() method we introduced in the
    last chapter and then dump it to stdout.
     */
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    /*
    If the variable has an initializer, we evaluate it. If not,
    we have another choice to make. We could have made this a
    syntax error in the parser by requiring an initializer.
    Most languages don’t, though, so it feels a little harsh to
    do so in Lox.

    We could make it a runtime error. We’d let you define an
    uninitialized variable, but if you accessed before assigning
    to it, a runtime error would occur. It’s not a bad idea, but
    most dynamically typed languages don’t do that. Instead, we’ll
    keep it simple and say that Lox sets a variable to nil if it
    isn’t explicitly initialized.

        var a;
        print a; // "nil".

    Thus, if there isn’t an initializer, we set the value to null,
    which is the Java representation of Lox’s nil value. Then we
    tell the environment to bind the variable to that value.
     */
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }
}
