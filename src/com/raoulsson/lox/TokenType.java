package com.raoulsson.lox;

/*
4.2.1) Token type

Keywords are part of the shape of the language’s grammar, so the parser
often has code like, “If the next token is while then do...” That means
the parser wants to know not just that it has a lexeme for some identifier,
but that it has a reserved word, and which keyword it is.

The parser could categorize tokens from the raw lexeme by comparing the
strings, but that’s slow and kind of ugly. Instead, at the point that we
recognize a lexeme, we also remember which kind of lexeme it represents.
We have a different type for each keyword, operator, bit of punctuation,
and literal type.
 */
public enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    // Literals.
    IDENTIFIER, STRING, NUMBER,
    // Keywords.
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,
    EOF
}
