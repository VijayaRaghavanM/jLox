package com.project.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    static boolean hadError = false;
    private static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [file]");
            System.exit(1);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError)
            System.exit(2);
        if (hadRuntimeError)
            System.exit(3);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        while (true) {
            System.out.print("> ");
            run(reader.readLine());
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        Parser parser = new Parser(tokens);
        Expr expression = parser.parse();
        printTokens(tokens);
        if (hadError)
            return;
        printAST(expression);
        printOutput(expression);
    }

    private static void printOutput(Expr expression) {
        System.out.println("Output:");
        interpreter.interpret(expression);
        System.out.println();
    }

    private static void printAST(Expr expression) {
        System.out.println("AST:");
        System.out.println(new AstPrinter().print(expression));
        System.out.println();
    }

    private static void printTokens(List<Token> tokens) {
        System.out.println("Tokens List:");
        for (Token token : tokens)
            System.out.println(token);
        System.out.println();
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    public static void runtimeError(RuntimeError error) {
        System.out.println("[line " + error.token.line + "] " + error.getMessage());
        hadRuntimeError = true;
    }
}