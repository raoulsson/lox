package com.raoulsson.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// https://opensource.apple.com/source/Libc/Libc-320/include/sysexits.h
public class Lox {

    static boolean hadError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: lox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    /*
    Lox is a scripting language, which means it executes directly from
    source. Our interpreter supports two ways of running code. If you
    start jlox from the command line and give it a path to a file, it
    reads the file and executes it.
     */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
    }

    /*
    If you want a more intimate conversation with your interpreter,
    you can also run it interactively. Fire up jlox without any
    arguments, and it drops you into a prompt where you can enter and
    execute code one line at a time.
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (; ; ) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            /*
            We need to reset this flag in the interactive loop. If the
            user makes a mistake, it shouldn’t kill their entire session.
             */
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        // print the tokens.
        for (Token token : tokens) {
            System.out.println(token);
        }
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();

        /*
        We’ll use this to ensure we don’t try to execute code that has
        a known error. Also, it lets us exit with a non-zero exit code
        like a good command line citizen should.
         */
        if (hadError) {
            return;
        }

        System.out.println(new AstPrinter().print(expression));
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

}

