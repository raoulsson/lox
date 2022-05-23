package com.raoulsson.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.raoulsson.lox.TokenType.*;

/*
The first step in any compiler or interpreter is scanning. The scanner takes
in raw source code as a series of characters and groups it into a series of
chunks we call tokens. These are the meaningful “words” and “punctuation”
that make up the language’s grammar.
Scanning is a good starting point for us too because the code isn’t very hard
— pretty much a switch statement with delusions of grandeur. It will help us
warm up before we tackle some of the more interesting material later. By the
end of this chapter, we’ll have a full-featured, fast scanner that can take
any string of Lox source code and produce the tokens that we’ll feed into the
parser in the next chapter.
 */
class Scanner {
    /*
    The start and current fields are offsets that index into the string.
    The start field points to the first character in the lexeme being
    scanned, and current points at the character currently being considered.
    The line field tracks what source line current is on, so we can produce
    tokens that know their location.
     */
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    /*
    To handle keywords, we see if the identifier’s lexeme is one of
    the reserved words. If so, we use a token type specific to that
    keyword. We define the set of reserved words in a map:
     */
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    /*
    We store the raw source code as a simple string, and we have a list
    ready to fill with tokens we’re going to generate. The aforementioned
    loop that does that looks like this method.
     */
    Scanner(String source) {
        this.source = source;
    }

    /*
    The scanner works its way through the source code, adding tokens until
    it runs out of characters. Then it appends one final “end of file” token.
    That isn’t strictly needed, but it makes our parser a little cleaner.
     */
    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /*
    Then we have one little helper function that tells us if we’ve
    consumed all the characters.
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /*
    Each turn of the loop, we scan a single token. This is the real
    heart of the scanner. We’ll start simple. Imagine if every lexeme
    is only a single character long. All you need to do is consume the
    next character and pick a token type for it. Several lexemes are
    only a single character in Lox, so let’s start with those.
     */
    private void scanToken() {
        // c is where we are at, current is set to next after calling advance()
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;
            /*
            We have single-character lexemes working, but that doesn’t cover
            all of Lox’s operators. What about !? It’s a single character,
            right? Sometimes, yes, but if the very next character is an equals
            sign, then we should instead create a != lexeme.
             */
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                /*
                This is similar to the other two-character operators, except that
                when we find a second /, we don’t end the token yet. Instead, we
                keep consuming characters until we reach the end of the line.
                Comments are lexemes, but they aren’t meaningful, and the parser
                doesn’t want to deal with them. So when we reach the end of the
                comment, we don’t call addToken(). When we loop back around to start
                the next lexeme, start gets reset and the comment’s lexeme disappears
                in a puff of smoke.
                 */
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    // We got division
                    addToken(SLASH);
                }
                break;
            /*
            While we’re at it, now’s a good time to skip over those other
            meaningless characters: newlines and whitespace.
             */
            case ' ':
            case '\r':
            case '\t':
                /*
                When encountering whitespace, we simply go back to the
                beginning of the scan loop. That starts a new lexeme
                after the whitespace character.
                 */
                break;
            /*
            For newlines, we do the same thing, but we also increment
            the line counter. (This is why we used peek() to find the
            newline ending a comment instead of match(). We want that
            newline to get us here, so we can update line.)
             */
            case '\n':
                line++;
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(c)) {
                    /*
                    Once we know we are in a number, we branch to a separate
                    method to consume the rest of the literal, like we do with
                    strings.
                     */
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    /*
                    Before we get too far in, let’s take a moment to think about
                    errors at the lexical level. What happens if a user throws a
                    source file containing some characters Lox doesn’t use, like
                    @#^ at our interpreter? Right now, those characters get
                    silently discarded. They aren’t used by the Lox language, but
                    that doesn’t mean the interpreter can pretend they aren’t there.
                    Instead, we report an error.
                    Note that the erroneous character is still consumed by the
                    earlier call to advance(). That’s important so that we don’t
                    get stuck in an infinite loop. Note also that we keep scanning.
                    There may be other errors later in the program.
                     */
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    /*
    The advance() method consumes the next character in the source
    file and returns it.
     */
    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    /*
    Where advance() is for input, addToken() is for output. It grabs
    the text of the current lexeme and creates a new token for it.
     */
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    /*
    When the token has a value, literal represents it, as for numbers:

        new Token(NUMBER, "48",  48.0, <line>)

    Here a Lox "int" is represented as a Java "double". Or for strings:

        new Token(STRING, "hello", hello, <line>)
     */
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    /*
    It’s like a conditional advance(). We only consume the current
    character if it’s what we’re looking for.
    Using match(), we recognize these lexemes in two stages. When we
    reach, for example, !, we jump to its switch case. That means we
    know the lexeme starts with !. Then we look at the next character
    to determine if we’re on a != or merely a !.
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    /*
    It’s sort of like advance(), but doesn’t consume the character. This is
    called lookahead. Since it only looks at the current unconsumed character,
    we have one character of lookahead. The smaller this number is, generally,
    the faster the scanner runs. The rules of the lexical grammar dictate how
    much lookahead we need. Fortunately, most languages in wide use only peek
    one or two characters ahead.
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /*
    Like with comments, we consume characters until we hit the " that
    ends the string. We also gracefully handle running out of input
    before the string is closed and report an error for that. For no
    particular reason, Lox supports multi-line strings. There are pros
    and cons to that, but prohibiting them was a little more complex
    than allowing them, so I left them in. That does mean we also need
    to update line when we hit a newline inside a string.
    Finally, the last interesting bit is that when we create the token,
    we also produce the actual string value that will be used later by
    the interpreter. Here, that conversion only requires a substring()
    to strip off the surrounding quotes. If Lox supported escape
    sequences like \n, we’d unescape those here.
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }
        // The closing ".
        advance();
        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    /*
    To recognize the beginning of a number lexeme, we look for any digit.
    It’s kind of tedious to add cases for every decimal digit, so we’ll
    stuff it in the default case instead.
     */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /*
    We consume as many digits as we find for the integer part of
    the literal. Then we look for a fractional part, which is a
    decimal point (.) followed by at least one digit. If we do have
    a fractional part, again, we consume as many digits as we can
    find.
     */
    private void number() {
        while (isDigit(peek())) advance();
        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /*
    Looking past the decimal point requires a second character of lookahead
    since we don’t want to consume the . until we’re sure there is a digit
    after it. So we add:
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /*
    When isAlpha(), advance until isAlphaNumeric(peek()) ends. text
    can then be either a keyword or else an identifier.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    /*
    So we begin by assuming any lexeme starting with a letter
    or underscore is an identifier.
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    /*
    A character or a digit. Used to find out the ending of a literal (that
    starts with either a char or an underscore.
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
