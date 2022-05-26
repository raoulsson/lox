package com.raoulsson.lox;

import java.util.ArrayList;
import java.util.List;

import static com.raoulsson.lox.TokenType.*;

/*
The only remaining piece is parsing—transmogrifying a sequence of tokens into
one of those syntax trees.

We define a factor expression as a flat sequence of multiplications and divisions.
This matches the same syntax as the previous rule, but better mirrors the code we’ll
write to parse Lox. We use the same structure for all of the other binary operator
precedence levels, giving us this complete expression grammar:

expression  → assignment ;
assignment  → IDENTIFIER "=" assignment | equality ;
equality    → comparison ( ( "!=" | "==" ) comparison )* ;
comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term        → factor ( ( "-" | "+" ) factor )* ;
factor      → unary ( ( "/" | "*" ) unary )* ;
unary       → ("!"|"-") unary | primary ;
Variable    → "var" IDENTIFIER ( "=" expression )? ";" ;
primary     → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" | IDENTIFIER ;

A recursive descent parser is a literal translation of the grammar’s rules straight
into imperative code. Each rule becomes a function. The body of the rule translates
to code roughly like:

Grammar notation        Code representation

Terminal                Code to match and consume a token
Nonterminal             Call to that rule’s function
|                       if or switch statement
* or +                  while or for loop
?                       if statement

The “recursive” part of recursive descent is because when a grammar rule refers to
itself—directly or indirectly—that translates to a recursive function call.

While in recursive descent, the rules it is in, is not stored explicitly in fields.
Instead, we use Java’s own call stack to track what the parser is doing. Each rule
in the middle of being parsed is a call frame on the stack.

For a walk through example, checkout: https://github.com/raoulsson/lox/blob/b3c83de31e09a47e3a21cdb1284a558da53bd170/src/com/raoulsson/lox/Parser.java
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class Parser {
    /*
    Like the scanner, the parser consumes a flat input sequence,
    only now we’re reading tokens instead of characters. We store
    the list of tokens and use current to point to the next token
    eagerly waiting to be parsed.
     */
    private final List<Token> tokens;
    private int current = 0;

    /*
    The parser gets handed in the tokens the scanner produced earlier.
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /*
    This parses a series of statements, as many as it can find until it
    hits the end of the input. This is a pretty direct translation of
    the program rule into recursive descent style. We must also chant a
    minor prayer to the Java Verbosity Gods since we are using ArrayList
    now.
     */
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            //statements.add(statement());
            statements.add(declaration());
        }
        return statements;
    }

    /*
    A program is a list of statements, and we parse one of those statements
    using this method.

    A little bare bones, but we’ll fill it in with more statement types later.
    We determine which specific statement rule is matched by looking at the
    current token. A print token means it’s obviously a print statement.

    If the next token doesn’t look like any known kind of statement, we assume
    it must be an expression statement. That’s the typical final fallthrough
    case when parsing a statement, since it’s hard to proactively recognize
    an expression from its first token.
     */
    private Stmt statement() {
        if(match(PRINT)) return printStatement();
        return expressionStatement();
    }

    /*
    Since we already matched and consumed the print token itself, we
    don’t need to do that here. We parse the subsequent expression,
    consume the terminating semicolon, and emit the syntax tree.
     */
    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    /*
    As always, the recursive descent code follows the grammar rule.
    The parser has already matched the var token, so next it requires
    and consumes an identifier token for the variable name.
    Then, if it sees an = token, it knows there is an initializer
    expression and parses it. Otherwise, it leaves the initializer null.
    Finally, it consumes the required semicolon at the end of the
    statement. All this gets wrapped in a Stmt.Var syntax tree node
    and we’re groovy.
     */
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    /*
    If we didn't match a print statement, we must have one of these.
    Similar to the previous method, we parse an expression followed
    by a semicolon. We wrap that Expr in a Stmt of the right type and return it.
     */
    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    /*
    Entry point to the parser.

    The parser’s parse() method that parses and returns a single expression
    was a temporary hack to get the last chapter up and running. Now that
    our grammar has the correct starting rule, program, we can turn parse()
    into the real deal.

    We keep it, renamed to parseToExpr
     */
    Expr parseToExpr() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    /*
    We’re going to run straight through the expression grammar now
    and translate each rule to Java code. The first rule, expression,
    simply expands to the equality rule, so that’s straightforward.

        expression  → equality ;

    Each method for parsing a grammar rule produces a syntax tree for
    that rule and returns it to the caller. When the body of the rule
    contains a nonterminal—a reference to another rule—we call that
    other rule’s method.
     */
    private Expr expression() {
        //return equality();
        return assignment();
    }

    /*
    Here is where it gets tricky. A single token lookahead recursive
    descent parser can’t see far enough to tell that it’s parsing an
    assignment until after it has gone through the left-hand side and
    stumbled onto the =. You might wonder why it even needs to. After
    all, we don’t know we’re parsing a + expression until after we’ve
    §finished parsing the left operand.

    The difference is that the left-hand side of an assignment isn’t an
    expression that evaluates to a value. It’s a sort of pseudo-expression
    that evaluates to a “thing” you can assign to. In:

        var a = "before";
        a = "value";

    On the second line, we don’t evaluate a (which would return the string
    “before”). We figure out what variable a refers to, so we know where
    to store the right- hand side expression’s value. The classic terms
    for these two constructs are l- value and r-value. All of the expressions
    that we’ve seen so far that produce values are r-values. An l-value
    “evaluates” to a storage location that you can assign into.
    We want the syntax tree to reflect that an l-value isn’t evaluated like a
    normal expression. That’s why the Expr.Assign node has a Token for the
    left-hand side, not an Expr. The problem is that the parser doesn’t know
    it’s parsing an l-value until it hits the =. In a complex l-value, that
    may occur many tokens later:

        makeList().head.next = node;

    We only have a single token of lookahead, so what do we do? We use a little
    trick, and it looks like so (see implementation below).

    Most of the code for parsing an assignment expression looks similar to the
    other binary operators like +. We parse the left-hand side, which can be any
    expression of higher precedence. If we find an =, we parse the right-hand
    side and then wrap it all up in an assignment expression tree node.

    One slight difference from binary operators is that we don’t loop to build up
    a sequence of the same operator. Since assignment is right-associative, we
    instead recursively call assignment() to parse the right-hand side.

    The trick is that right before we create the assignment expression node, we
    look at the left-hand side expression and figure out what kind of assignment
    target it is. We convert the r-value expression node into an l-value
    representation.

    This trick works because it turns out that every valid assignment target
    happens to also be valid syntax as a normal expression. Consider a complex
    field assignment like:

        newPoint(x + 2, 0).y = 3;

    The left-hand side of that assignment could also work as a valid expression:

        newPoint(x + 2, 0).y;

    (Where the first example sets the field, the second gets it.)

    This means we can parse the left-hand side as if it were an expression and
    then after the fact produce a syntax tree that turns it into an assignment
    target. If the left-hand side expression isn’t a valid assignment target,
    we fail with a syntax error. That ensures we report an error on code like:

        a + b = c;

    Right now, the only valid target is a simple variable expression, but we’ll
    add fields later. The end result of this trick is an assignment expression
    tree node that knows what it is assigning to and has an expression subtree
    for the value being assigned. All with only a single token of lookahead and
    no backtracking.
     */
    private Expr assignment() {
        Expr expr = equality();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /*
    This declaration() method is the method we call repeatedly
    when parsing a series of statements in a block or a script,
    so it’s the right place to synchronize when the parser goes
    into panic mode. The whole body of this method is wrapped in
    a try block to catch the exception thrown when the parser
    begins error recovery. This gets it back to trying to parse
    the beginning of the next statement or declaration.

    The real parsing happens inside the try block. First, it
    looks to see if we’re at a variable declaration by looking
    for the leading var keyword. If not, it falls through to the
    existing statement() method that parses print and expression
    statements.

    Remember how statement() tries to parse an expression statement
    if no other statement matches? And expression() reports a syntax
    error if it can’t parse an expression at the current token? That
    chain of calls ensures we report an error if a valid declaration
    or statement isn’t parsed.
     */
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /*
        equality → comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /*
        comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /*
        term        → factor ( ( "-" | "+" ) factor )* ;
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /*
        factor      → unary ( ( "/" | "*" ) unary )* ;
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /*
        unary       → ("!"|"-") unary | primary ;
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    /*
        primary     → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;
     */
    private Expr primary() {
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expected expression.");
    }

    /*
    Checks if the current token is of one in types. If so, it moves the token reader head
    to the next token (aka: current++ in advance()).
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /*
    We handled a token, time to move to the next if not at end.
     */
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    /*
    Thanks to the way the Scanner is implemented, the token sequence always ends with EOF.
     */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    /*
    Look ahead if the next token is of a certain type
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    /*
    Look at the current token but do not move the reader head, don't consume it.
     */
    private Token peek() {
        return tokens.get(current);
    }

    /*
    Get the previous token. Needed as match moves the head, and we often need the last
    token as operator.
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /*
    Some tokens don't produce an output, but they still have to be encountered and
    consumed, removed from sight. The only candidate token at this point is RiGHT_PAREN.
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    /*
    This is a simple sentinel class we use, to unwind the parser. The error()
    method returns it instead of throwing because we want to let the calling
    method inside the parser decide whether to unwind or not. Some parse
    errors occur in places where the parser isn’t likely to get into a weird
    state, and we don’t need to synchronize. In those places, we simply report
    the error and keep on truckin'.

    For example, Lox limits the number of arguments you can pass to a function.
    If you pass too many, the parser needs to report that error, but it can
    and should simply keep on parsing the extra arguments instead of freaking
    out and going into panic mode.

    Return an error with context. To throw or present it to the user in
    a meaningful way, is the client's job. Thus, we don't throw it.
     */
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    /*
    With recursive descent, the parser’s state—which rules it is in
    the middle of recognizing—is not stored explicitly in fields.
    Instead, we use Java’s own call stack to track what the parser
    is doing. Each rule in the middle of being parsed is a call frame
    on the stack. In order to reset that state, we need to clear out
    those call frames.

    The natural way to do that in Java is exceptions. When we want to
    synchronize, we throw that ParseError object. Higher up in the
    method for the grammar rule we are synchronizing to, we’ll catch it.
    Since we synchronize on statement boundaries, we’ll catch the
    exception there. After the exception is caught, the parser is in
    the right state. All that’s left is to synchronize the tokens.

    We want to discard tokens until we’re right at the beginning of the
    next statement. That boundary is pretty easy to spot—it’s one of the
    main reasons we picked it. After a semicolon, we’re probably finished
    with a statement. Most statements start with a keyword—for, if, return,
    var, etc. When the next token is any of those, we’re probably about to
    start a statement.

    It discards tokens until it thinks it found a statement boundary.
    After catching a ParseError, we’ll call this, and then we are hopefully
    back in sync. When it works well, we have discarded tokens that would
    have likely caused cascaded errors anyway, and now we can parse the rest
    of the file starting at the next statement.
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    private static class ParseError extends RuntimeException {
    }

}
