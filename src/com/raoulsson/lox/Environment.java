package com.raoulsson.lox;

import java.util.HashMap;
import java.util.Map;

/*
The bindings that associate variables to values need to be stored
somewhere. Ever since the Lisp folks invented parentheses, this
data structure has been called an environment.

There’s a Java Map in there to store the bindings. It uses bare
strings for the keys, not tokens. A token represents a unit of
code at a specific place in the source text, but when it comes
to looking up variables, all identifier tokens with the same name
should refer to the same variable (ignoring scope for now). Using
the raw string ensures all of those tokens refer to the same map
key.
 */
public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    /*
    Binds a new name to a value.

    Not exactly brain surgery, but we have made one interesting
    semantic choice. When we add the key to the map, we don’t
    check to see if it’s already present. That means that this
    program works:

        var a = "before";
        print a; // "before".
        var a = "after";
        print a; // "after".

    We could choose to make this an error instead. Making
    redefinition an error would help them find that bug.
    However, doing so interacts poorly with the REPL. So, to
    keep the two modes consistent, we’ll allow it—at least
    for global variables.
     */
    void define(String name, Object value) {
        values.put(name, value);
    }

    /*
    If the variable is found, it simply returns the value bound
    to it. But what if it’s not? Again, we have a choice.

        - Make it a syntax error.
        - Make it a runtime error.
        - Allow it and return some default value like nil.

    Lox is pretty lax, but the last option is a little too permissive
    to me. Making it a syntax error - a compile-time error - seems
    like a smart choice. Using an undefined variable is a bug, and
    the sooner you detect the mistake, the better.

    The problem is that using a variable isn’t the same as referring
    to it. You can refer to a variable in a chunk of code without
    immediately evaluating it if that chunk of code is wrapped inside
    a function. If we make it a static error to mention a variable
    before it’s been declared, it becomes much harder to define
    recursive functions.

    Since making it a static error makes recursive declarations too
    difficult, we’ll defer the error to runtime.
     */
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    /*
    The key difference between assignment and definition is that
    assignment is not allowed to create a new variable. In terms
    of our implementation, that means it’s a runtime error if the
    key doesn’t already exist in the environment’s variable map.
     */
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
