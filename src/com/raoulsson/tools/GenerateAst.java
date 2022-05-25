package com.raoulsson.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GenerateAst {

    private static boolean genExpr = true;

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Binary     : Expr left, Token operator, Expr right : expression operator expression ;",
                "Grouping   : Expr expression : \"(\" expression \")\" ; ",
                "Literal    : Object value : NUMBER | STRING | \"true\" | \"false\" | \"nil\" ;",
                "Unary      : Token operator, Expr right : (\"-\"|\"!\")expression ;"
        ));

        genExpr = false;

        defineAst(outputDir, "Stmt", Arrays.asList(
                "Expression : Expr expression : <comment>",
                "Print      : Expr expression : <comment>"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java.txt";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package com.raoulsson.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("// Generated code by com.raoulsson.tools.GenerateAst");
        if(genExpr) {
            writer.println("/*");
            writer.println("expression  → literal | unary | binary | grouping ;");
            writer.println("*/");
        } else {
            writer.println("/*");
            writer.println("The first rule is now program, which is the starting point for");
            writer.println("the grammar and represents a complete Lox script or REPL entry.");
            writer.println("A program is a list of statements followed by the special 'end");
            writer.println("of file' token. The mandatory end token ensures the parser consumes");
            writer.println("the entire input and doesn’t silently ignore erroneous unconsumed");
            writer.println("tokens at the end of a script.");
            writer.println();
            writer.println("program     → statement* EOF ;");
            writer.println("statement   → exprStmt | printStmt ;");
            writer.println("exprStmt    → expression \";\" ;");
            writer.println("printStmt   → \"print\" expression \";\" ;");
            writer.println();
            writer.println("There is no place in the grammar where both an expression and a");
            writer.println("statement is allowed. The operands of, say, + are always expressions,");
            writer.println("never statements. The body of a while loop is always a statement.");
            writer.println("*/");
        }
        writer.println("public abstract class " + baseName + " {");
        writer.println();

        if(genExpr) {
            defineVisitor(writer, baseName, types);

            writer.println("/*");
            writer.println("The Visitor pattern is the most widely misunderstood pattern in all of Design \nPatterns, " +
                    "which is really saying something when you look at the software architecture \nexcesses of the past " +
                    "couple of decades.\n" +
                    "The trouble starts with terminology. The pattern isn’t about “visiting” and the “accept”\n method in " +
                    "it doesn’t conjure up any helpful imagery either. Many think the pattern has to \ndo with traversing " +
                    "trees, which isn’t the case at all. We are going to use it on a set of \nclasses that are tree-like, " +
                    "but that’s a coincidence. As you’ll see, the pattern works as well on a \nsingle object. " +
                    "The Visitor pattern is really about approximating the functional style within an OOP \nlanguage. " +
                    "It lets us add new columns to that table easily. We can define all of the behavior \nfor a new " +
                    "operation on a set of types in one place, without having to touch the types themselves. \nIt does " +
                    "this the same way we solve almost every problem in computer science: by adding a layer \nof " +
                    "indirection.");
            writer.println();
            writer.println("All subclasses of Expr accept any class that implements the Visitor<R> interface.");
            writer.println("They pass themselves to the visitor and return it's return value of type R.");
            writer.println("*/");
            writer.println("    abstract <R> R accept(Visitor<R> visitor);");
            writer.println();
        }

        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            String comment = type.split(":")[2].trim();
            defineType(writer, baseName, className, fields, comment);
        }

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("/*");
        writer.println("Any class that needs to operate on the Expr data, can implement the Visitor<R>");
        writer.println("interface. Currently: AstPrinter");
        writer.println("");
        writer.println("With method overloading, we would only specify");
        writer.println();
        writer.println("    R visit(Expr expr)");
        writer.println();
        writer.println("and do the dispatching within this method (Binary, Grouping, ...)");
        writer.println("*/");
        writer.println("    interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase(Locale.ROOT) + ");");
        }

        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldsList, String comment) {
        String[] fields = fieldsList.split(", ");

        writer.println("/*");
        if(genExpr) {
            writer.println("" + className + " → " + comment);
        }
        writer.println("*/");
        writer.println("    public static class " + className + " extends " + baseName + " {");
        writer.println();
        for (String field : fields) {
            writer.println("        final " + field + ";");
        }
        writer.println();
        writer.println("        public " + className + "(" + fieldsList + ") {");
        for (String field : fields) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");

        writer.println();
        if(genExpr) {
            writer.println("/*");
            writer.println("We have no clue who the visitor is, but we accept him and give ourselves to him.");
            writer.println("*/");
            writer.println("        @Override");
            writer.println("        <R> R accept(Visitor<R> visitor) {");
            writer.println("            return visitor.visit" + className + baseName + "(this);");
            writer.println("        }");
            writer.println();
        }
        writer.println("    }");
        writer.println();
    }
}
