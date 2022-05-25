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

    /*
    We make the field static so that successive calls to run() inside a REPL
    session reuse the same interpreter. That doesn’t make a difference now,
    but it will later when the interpreter stores global variables. Those
    variables should persist throughout the REPL session.
     */
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    /*
    This field plays a small but important role.

    If the user is running a Lox script from a file and a runtime error
    occurs, we set an exit code when the process quits to let the calling
    process know.

    Not everyone cares about shell etiquette, but we do.
     */
    static boolean hadRuntimeError = false;

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
        System.out.println("Reading LOX source file from: " + path);
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
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
        System.out.println("Source:\n" + source);
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        // print the tokens.
        System.out.println("Scanned tokens:");
        int c = 1;
        for (Token token : tokens) {
            System.out.println(c++ + ": " + token.toStringParserView());
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

        System.out.println("AST: " + new AstPrinter().print(expression));

        /*
        We have an entire language pipeline now: scanning, parsing, and execution.
        Congratulations, you now have your very own arithmetic calculator.

        As you can see, the interpreter is pretty bare bones. But the Interpreter
        class and the visitor pattern we’ve set up today form the skeleton that
        later chapters will stuff full of interesting guts—variables, functions,
        etc. Right now, the interpreter doesn’t do very much, but it’s alive!
         */
        interpreter.interpret(expression);
    }

    /*
    This reports an error at a given token. It shows the token’s location and the
    token itself. This will come in handy later since we use tokens throughout
    the interpreter to track locations in code.
     */
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

    /*
    For now we just print the errors. We could also collect them in some other fasion
    and display them later to the users IDE for example.
     */
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    /*
    If a runtime error is thrown while evaluating the expression, interpret()
    catches it. This lets us report the error to the user and then gracefully
    continue. All of our existing error reporting code lives in the Lox class,
    so we put this method there too.
     */
    public static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}

