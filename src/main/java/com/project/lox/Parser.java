package com.project.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    private static class ParseError extends RuntimeException {
        private static final long serialVersionUID = -8844227722959357629L;
    }

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.FUN))
                return function("function");
            if (match(TokenType.VAR))
                return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt function(String kind) {
        Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name");
        List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Cannot have more than 255 parameters");
                }
                parameters.add(consume(TokenType.IDENTIFIER, "Expected parameter name"));
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after " + kind + " arguments");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body");
        Stmt.Block body = (Stmt.Block) block();
        return new Stmt.Function(name, parameters, body.statements);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expected a variable name");
        Expr initializer = null;
        if (match(TokenType.EQUAL))
            initializer = expression();
        consume(TokenType.SEMICOLON, "Expected a ; after Expression");
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.IF))
            return ifStatement();
        if (match(TokenType.WHILE))
            return whileStatement();
        if (match(TokenType.FOR))
            return forStatement();
        if (match(TokenType.PRINT))
            return printStatement();
        if (match(TokenType.RETURN))
            return returnStatement();
        if (match(TokenType.LEFT_BRACE))
            return block();
        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after return statement");
        return new Stmt.Return(keyword, value);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after for");

        Stmt initializer;
        if (match(TokenType.SEMICOLON))
            initializer = null;
        else if (match(TokenType.VAR))
            initializer = varDeclaration();
        else
            initializer = expressionStatement();
        Expr condition = null;
        if (!check(TokenType.SEMICOLON))
            condition = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after for condition");
        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN))
            increment = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after for increment");
        Stmt body = statement();

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }
        if (condition == null)
            condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after while");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after while condition");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after if");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after if condition");
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(TokenType.RIGHT_BRACE, "Expected a '}' after the end of a block");
        return new Stmt.Block(statements);
    }

    private Stmt expressionStatement() {
        Expr expression = expression();
        consume(TokenType.SEMICOLON, "Expected a ; after Expression");
        return new Stmt.Expression(expression);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(TokenType.SEMICOLON, "Expected a ; after Expression");
        return new Stmt.Print(value);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();
        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target");
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(TokenType.OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = addition();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr multiplication() {
        Expr expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        } else {
            return call();
        }
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Number of arguments cannot exceed 255");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        Token paren = consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        Expr expr = new Expr.Literal(null);
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        } else if (match(TokenType.TRUE))
            expr = new Expr.Literal(true);
        else if (match(TokenType.FALSE))
            expr = new Expr.Literal(false);
        else if (match(TokenType.NIL))
            expr = new Expr.Literal(null);

        else if (match(TokenType.NUMBER, TokenType.STRING)) {
            expr = new Expr.Literal(previous().literal);
        } else if (match(TokenType.LEFT_PAREN)) {
            expr = expression();
            consume(TokenType.RIGHT_PAREN, "EXPECTED ')' after expression");
            expr = new Expr.Grouping(expr);
        } else {
            throw error(peek(), "Expected expression.");
        }
        return expr;
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (!isAtEnd())
            current++;
        return previous();
    }

    private boolean check(TokenType type) {
        if (isAtEnd())
            return false;
        return peek().type == type;
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON)
                return;
        }
        switch (peek().type) {
            case CLASS:
            case FUN:
            case VAR:
            case FOR:
            case IF:
            case WHILE:
            case PRINT:
            case RETURN:
                return;
            default:
                break;
        }
        advance();
    }

}