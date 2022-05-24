package com.raoulsson.lox;

import java.util.List;
/*
I know static imports are considered bad style by some, but they save me from
having to sprinkle TokenType. all over the scanner and parser. Forgive me, but
every character counts in a book.
 */
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
primary     → NUMBER | STRING | "true" | "false" | "nil" | "(" expression ")" ;

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

As a walk through example, we use the one from the book:

    -123 * (45.67)

To run it, feed the file "lox_test_sources/parser_example_from_book.lox" to Lox.

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
 */
public class Parser {

    private static class ParseError extends RuntimeException {}

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
    Entry point to the parser
     */
    Expr parse() {
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
        /*
        STEP 1: Starting from the start of the tokens, nothing happens yet,
        but expression yields equality in the grammar, so...
         */
        return equality();
    }

    /*
        equality → comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private Expr equality() {
        /*
        STEP 2: equality yields comparison on the left side, so...
         */
        Expr expr = comparison();   // comparison

        // ( ... )*
        while(match(BANG_EQUAL, EQUAL_EQUAL)) { // ( "!=" | "==" )
            Token operator = previous();
            Expr right = comparison();  // comparison
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /*
    Checks if the current token is of one in types. If so, it moves the token reader head
    to the next token (aka: current++ in advance()).
     */
    private boolean match(TokenType... types) {
        for(TokenType type : types) {
            /*
            STEP 7: We pass MINUS into check().
             */
            if (check(type)) {
                /*
                STEP 11: We match a MINUS token and advance the head.
                 */
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd()) {
            /*
            STEP 12: Advance, or consume the token. From a reading perspective, we are done with it.
             */
            current++;
        }
        return previous();
    }

    /*
    Thanks to the way the Scanner is implemented, the token sequence always ends with EOF.
     */
    private boolean isAtEnd() {
        /*
        STEP 9: The current type is MINUS, not EOF, we return false.
         */
        return peek().type == EOF;
    }

    private boolean check(TokenType type) {
        /*
        STEP 8: Check if we are at the end.
         */
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    private Token peek() {
        /*
        STEP 10: Now we actually look at the data at the current position, which is 0.
        We return the token: MINUS.
         */
        return tokens.get(current);
    }

    private Token previous() {
        /*
        STEP 14: We get the MINUS token
         */
        return tokens.get(current - 1);
    }

    /*
        comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     */
    private Expr comparison() {
        /*
        STEP 3: comparison yields term on the left side, so...
         */
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
        /*
        STEP 4: term yields factor on the left side, so...
         */
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
        /*
        STEP 5: factor yields unary on the left side, so...
         */
        Expr expr = unary();
        /*
        STEP 20: Token one (MINUS) and two (NUMBER 123.0) are dealt with and sit in expr now.
         */
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
        /*
        STEP 6: We haven't touched the tokens yet. This happens somewhere within match
        or below. We check if it is either a ! or a -.
         */
        /*
        STEP 16: We match again, but we got a literal NUMBER token next, so we don't enter
        the if block. (Steps omitted).
         */
        if (match(BANG, MINUS)) {
            /*
            STEP 13: We have a MINUS token. Since we advanced the reading head, we need
            to get the previous as the operator token.
             */
            Token operator = previous();
            /*
            STEP 15: We got the - of -123.0 but don't know about the NUMBER token with value
            123.0 yet. We recurse to unary, as the grammar allows this (e.g. !!!123.0). In
            our case, we expect the next token to be a primary with value 123.0.
             */
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        /*
        STEP 17: unary yield unary or primary. We know we got NUMBER and descent.
         */
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
        /*
        STEP 18: We got NUMBER and enter the if...
         */
        if (match(NUMBER, STRING)) {
            /*
            STEP 19: Match advanced to the next token, so we get the previous token, our
            NUMBER 123.0 and return a new Literal with the literal value of 123.0.
             */
            return new Expr.Literal(previous().literal);
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        // https://stackoverflow.com/questions/70999675/how-does-this-recursive-descent-parser-match-specific-operators
        // If we got right down to primary() and still nothing matches, then the token we have cannot possibly be the
        // start of an expression. In this case, throw an error.
        throw error(peek(), "Expected expression.");
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

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
}
