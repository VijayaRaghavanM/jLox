package com.project.lox;

import java.util.List;

abstract class Stmt {
    abstract <R> R accept(Visitor<R> visitor);
    interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
        R visitVarStmt(Var stmt);
    }
    static class Block extends Stmt {

    @Override
    <R> R accept(Visitor<R> visitor) {
        return visitor.visitBlockStmt(this);
    }

        final List<Stmt> statements;

        Block(List<Stmt> statements) {
            this.statements = statements;
        }
    }

    static class Expression extends Stmt {

    @Override
    <R> R accept(Visitor<R> visitor) {
        return visitor.visitExpressionStmt(this);
    }

        final Expr expression;

        Expression(Expr expression) {
            this.expression = expression;
        }
    }

    static class Print extends Stmt {

    @Override
    <R> R accept(Visitor<R> visitor) {
        return visitor.visitPrintStmt(this);
    }

        final Expr expression;

        Print(Expr expression) {
            this.expression = expression;
        }
    }

    static class Var extends Stmt {

    @Override
    <R> R accept(Visitor<R> visitor) {
        return visitor.visitVarStmt(this);
    }

        final Token name;
        final Expr initializer;

        Var(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }
    }


}
