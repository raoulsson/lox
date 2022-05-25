package com.raoulsson.lox;

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

 */
public class Interpreter implements Expr.Visitor<Object> {

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
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        // Unreachable
        return null;
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
                return (double)left > (double)right;
            case GREATER_EQUAL:
                return (double)left >= (double)right;
            case LESS:
                return (double)left < (double)right;
            case LESS_EQUAL:
                return (double)left <= (double)right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                break;
            case SLASH:
                return (double)left / (double)right;
            case STAR:
                return (double)left * (double)right;
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
}
