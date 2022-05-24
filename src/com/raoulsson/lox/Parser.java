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
unary       → unary ( ( "/" | "*" ) unary )* ; →("!"|"-")unary | primary ;
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
        return equality();
    }

    /*
        equality → comparison ( ( "!=" | "==" ) comparison )* ;
     */
    private Expr equality() {
        Expr expr = comparison();   // comparison

        // ( ... )*
        while(match(BANG_EQUAL, EQUAL_EQUAL)) { // ( "!=" | "==" )
            Token operator = previous();
            Expr right = comparison();  // comparison
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for(TokenType type: types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == type;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

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
