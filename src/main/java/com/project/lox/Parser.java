package com.project.lox;

import java.util.ArrayList;
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
            if (match(TokenType.VAR))
                return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
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
        if (match(TokenType.PRINT))
            return printStatement();
        if (match(TokenType.LEFT_BRACE))
            return block();
        return expressionStatement();
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
        Expr expr = equality();
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
            return primary();
        }
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