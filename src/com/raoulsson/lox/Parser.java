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

expression  → equality ;
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

As a walk through example, we use the one from the book, the Lox source being:

    -123 * (45.67)

To run it, feed the file "lox_test_sources/parser_example_from_book_1.lox" to Lox.

First the scanner produces these tokens:

    1: MINUS
    2: NUMBER 123.0
    3: STAR
    4: LEFT_PAREN
    5: NUMBER 45.67
    6: RIGHT_PAREN
    7: EOF

that are then fed into the parser, which produces this syntax tree as output:

   (* (- 123.0) (group 45.67))

In the comments, refer to "STEP n", as we go along to see how the data is used
and produced.
The actual values that are produced in the example, are given in comments like
such:

    return equality();    // 45.67 // (* (- 123.0) (group 45.67))

where 45.67 here would be the first value and so on, as we step along. It is
not so difficult to understand the code in principle by simply mapping the
grammar to code and get a feel for it, however, it also helps to go through
the code with the magnifying glass and see what "recursive descent" actually
does.
As another helper, I added comments like "GOTO-N" to the code, so that it
would be clear where to return to.
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
            statements.add(statement());
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
            // GOTO-11 - A jump back marker to help the reader to navigate the actual code flow.
            Expr expr = expression();
            /*
            STEP 32: We are finally done.
            TOKEN : EOF
            Return Binary (* (- 123.0) (group 45.67)) to client
             */
            return expr;
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
        /*
        STEP 1: expression  → equality ;
        TOKEN : MINUS (technically this happens in match(), but logically true)
         */
        /*
        STEP 14: The token within the brackets is a new expression, so we start from the top
        TOKEN : NUMBER 45.67
         */
        // GOTO-7
        Expr expr = equality();
        /*
        STEP 25:
        TOKEN : RIGHT_PAREN
        Return 45.67 to GOTO-8
         */
        /*
        STEP 31:
        TOKEN : EOF
        Return Binary (* (- 123.0) (group 45.67)) to GOTO-11
         */
        return expr;    // 45.67 // (* (- 123.0) (group 45.67))
    }

    /*
        equality → comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private Expr equality() {
        /*
        STEP 2: equality → comparison ( ( "!=" | "==" ) comparison )* ;
        TOKEN : MINUS
         */
        /*
        STEP 15:
        TOKEN : NUMBER 45.67
         */
        // GOTO-6
        Expr expr = comparison();   // 45.67 // (* (- 123.0) (group 45.67))

        // ( ... )*
        while (match(BANG_EQUAL, EQUAL_EQUAL)) { // ( "!=" | "==" )
            Token operator = previous();
            Expr right = comparison();  // comparison
            expr = new Expr.Binary(expr, operator, right);
        }
        /*
        STEP 24:
        TOKEN : RIGHT_PAREN
        Return 45.67 to GOTO-7
         */
        /*
        STEP 30:
        TOKEN : EOF
        Return Binary (* (- 123.0) (group 45.67)) to GOTO-7
         */
        return expr;    // 45.67 // (* (- 123.0) (group 45.67))
    }

    /*
        comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     */
    private Expr comparison() {
        /*
        STEP 3: comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
        TOKEN : MINUS
         */
        /*
        STEP 16:
        TOKEN : NUMBER 45.67
         */
        // GOTO-5
        Expr expr = term(); // 45.67 // (* (- 123.0) (group 45.67))

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        /*
        STEP 23:
        TOKEN : RIGHT_PAREN
        Return 45.67 to GOTO-6
         */
        /*
        STEP 29:
        TOKEN : EOF
        Return Binary (* (- 123.0) (group 45.67)) to GOTO-6
         */
        return expr;    // 45.67 // (* (- 123.0) (group 45.67))
    }

    /*
        term        → factor ( ( "-" | "+" ) factor )* ;
     */
    private Expr term() {
        /*
        STEP 4: term        → factor ( ( "-" | "+" ) factor )* ;
        TOKEN : MINUS
         */
        /*
        STEP 17:
        TOKEN : NUMBER 45.67
         */
        // GOTO-4
        Expr expr = factor();   // 45.67 // (* (- 123.0) (group 45.67))

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        /*
        STEP 22:
        TOKEN : RIGHT_PAREN
        Return 45.67 to GOTO-5
         */
        /*
        STEP 28:
        TOKEN : EOF
        Return Binary (* (- 123.0) (group 45.67)) to GOTO-5
         */
        return expr;    // 45.67 // (* (- 123.0) (group 45.67))
    }

    /*
        factor      → unary ( ( "/" | "*" ) unary )* ;
     */
    private Expr factor() {
        /*
        STEP 5: factor      → unary ( ( "/" | "*" ) unary )* ;
        TOKEN : MINUS
         */
        /*
        STEP 18:
        TOKEN : NUMBER 45.67
         */
        // GOTO-2
        Expr expr = unary();    // (- 123.0) // 45.67

        while (match(SLASH, STAR)) {
            Token operator = previous();    // *
            /*
            STEP 11: We match STAR and enter the while
            TOKEN : LEFT_PAREN
            */
            // GOTO-3
            Expr right = unary();   // (group 45.67)
            /*
            STEP 27:
                expr = (- 123.0)
                operator = *
                right = (group 45.67)
            TOKEN : EOF
            Return Binary (* (- 123.0) (group 45.67)) to GOTO-4
             */
            expr = new Expr.Binary(expr, operator, right);
        }
        /*
        STEP 21:
        TOKEN : RIGHT_PAREN
        Return 45.67 to GOTO-4
         */
        return expr;    // 45.67 // (* (- 123.0) (group 45.67))
    }

    /*
        unary       → ("!"|"-") unary | primary ;
     */
    private Expr unary() {
        /*
        STEP 6: Matching in unary
        TOKEN : MINUS
         */
        /*
        STEP 8: Matching in primary
        TOKEN : NUMBER 123.0
         */
        if (match(BANG, MINUS)) {
            /*
            STEP 7: We matched with MINUS. match() moves head, previous token is MINUS
            TOKEN : NUMBER 123.0
             */
            Token operator = previous();    // MINUS
            // GOTO-1
            Expr right = unary();   // 123.0
            /*
            STEP 8.5:
            TOKEN : STAR
            Return Unary (- 123.0) to GOTO-1
            */
            return new Expr.Unary(operator, right); // (- 123.0)
        }
        /*
        STEP 9:
        TOKEN : NUMBER 123.0
        Return 123 to GOTO-1
         */
        /*
        STEP 12: We skip the if and enter primary()
        TOKEN : LEFT_PAREN
        Reappearance in STEP 26
         */
        /*
        STEP 19:
        TOKEN : NUMBER 45.67
        Return 45.67 to GOTO-2
         */
        /*
        STEP 26:
        TOKEN : EOF
        Return (group 45.67) to GOTO-3
         */
        // GOTO-9
        return primary();   // 123.0 // 45.67 // (group 45.67)
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
            /*
            STEP 10:
            TOKEN : STAR
            Return literal number 123.0 to GOTO-1
             */
            /*
            STEP 20:
            TOKEN : RIGHT_PAREN
            Return literal number 45.67 to GOTO-2
             */
            return new Expr.Literal(previous().literal);    // 123.0  // 45.67
        }
        if (match(LEFT_PAREN)) {
            /*
            STEP 13: primary in parens forwards token to be handled as expression:
                primary     → ... | "(" expression ")" ;
            TOKEN : NUMBER 45.67
             */
            // GOTO-8
            Expr expr = expression();   // 45.67

            consume(RIGHT_PAREN, "Expect ')' after expression.");
            /*
            STEP 26: we consumed RIGHT_PAREN, cause after expression in left parens, right
            parens must follow.
            TOKEN : EOF
            Return (group 45.67) to GOTO-9
             */
            return new Expr.Grouping(expr); // (group 45.67)
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
    This is a simple sentinel class we use to unwind the parser. The error()
    method returns it instead of throwing because we want to let the calling
    method inside the parser decide whether to unwind or not. Some parse
    errors occur in places where the parser isn’t likely to get into a weird
    state and we don’t need to synchronize. In those places, we simply report
    the error and keep on truckin’.

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
    After catching a ParseError, we’ll call this and then we are hopefully
    back in sync. When it works well, we have discarded tokens that would
    have likely caused cascaded errors anyway and now we can parse the rest
    of the file starting at the next statement.

    (Used in later chapters, when Statements are introduced).
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
