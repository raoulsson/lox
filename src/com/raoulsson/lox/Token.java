package com.raoulsson.lox;

/*
4.2) Lexemes and Tokens

Here’s a line of Lox code:

    var language = "lox";

Here, var is the keyword for declaring a variable. That three-character
sequence “v-a-r” means something. But if we yank three letters out of
the middle of language, like “g-u-a”, those don’t mean anything on their
own.
That’s what lexical analysis is about. Our job is to scan through the
list of characters and group them together into the smallest sequences
that still represent something. Each of these blobs of characters is
called a lexeme. In that example line of code, the lexemes are:

    <var> <language> <=> <"lox"><;>

The lexemes are only the raw substrings of the source code. However, in
the process of grouping character sequences into lexemes, we also stumble
upon some other useful information. When we take the lexeme and bundle it
together with that other data, the result is a token.

4.2.2) Literal value

There are lexemes for literal values—numbers and strings and the like. Since
the scanner has to walk each character in the literal to correctly identify
it, it can also convert that textual representation of a value to the living
runtime object that will be used by the interpreter later.
 */
public class Token {

    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return type + " " + lexeme + " " + literal;
    }

}
