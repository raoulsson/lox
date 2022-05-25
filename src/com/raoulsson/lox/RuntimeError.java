package com.raoulsson.lox;

/*
It’s time for us to talk about runtime errors. I spilled a
lot of ink in the previous chapters talking about error handling,
but those were all syntax or static errors. Those are detected
and reported before any code is executed. Runtime errors are
failures that the language semantics demand we detect and report
while the program is running (hence the name).

Right now, if an operand is the wrong type for the operation being
performed, the Java cast will fail and the JVM will throw a
ClassCastException. That unwinds the whole stack and exits the
application, vomiting a Java stack trace onto the user. That’s
probably not what we want. The fact that Lox is implemented in Java
should be a detail hidden from the user. Instead, we want them to
understand that a Lox runtime error occurred, and give them an error
message relevant to our language and their program.


Unlike the Java cast exception, our class tracks the token that
identifies where in the user’s code the runtime error came from.
As with static errors, this helps the user know where to fix their
code.
 */
public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }

}
