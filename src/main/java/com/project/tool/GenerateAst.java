package com.project.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: generate_ast <output_dir>");
            System.exit(1);
        }
        String outputDir = args[0];
        List<String> expressions = Arrays.asList("Binary: Expr left, Token operator, Expr right",
                "Grouping: Expr expression", "Literal: Object value", "Unary: Token operator, Expr right",
                "Variable: Token name", "Assign: Token name, Expr value",
                "Logical: Expr left, Token operator, Expr right");
        defineAst(outputDir, "Expr", expressions);

        List<String> statements = Arrays.asList("Block: List<Stmt> statements", "Expression: Expr expression",
                "Print: Expr expression", "Var: Token name, Expr initializer",
                "If: Expr condition, Stmt thenBranch, Stmt elseBranch", "While: Expr condition, Stmt body");
        defineAst(outputDir, "Stmt", statements);

    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        writer.println("package com.project.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        defineVisitor(writer, baseName, types);
        // The AST classes
        for (String type : types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }
        writer.println();
        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");
        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writer.println(
                    "        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
        }
        writer.println("    }");
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fields) {
        writer.println("    static class " + className + " extends " + baseName + " {");
        String[] fieldList = fields.split(", ");
        // Visitor Pattern
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("        return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");

        // Fields
        writer.println();
        for (String field : fieldList) {
            writer.println("        final " + field + ";");
        }
        writer.println();
        // Constructor
        writer.println("        " + className + "(" + fields + ") {");

        // Store parameters in fields
        for (String field : fieldList) {
            String name = field.split(" ")[1];
            writer.println("            this." + name + " = " + name + ";");
        }
        writer.println("        }");
        // End of the class
        writer.println("    }");
        writer.println();
    }
}