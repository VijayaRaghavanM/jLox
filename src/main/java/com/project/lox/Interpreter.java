package com.project.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.project.lox.Expr.Assign;
import com.project.lox.Expr.Binary;
import com.project.lox.Expr.Call;
import com.project.lox.Expr.Get;
import com.project.lox.Expr.Grouping;
import com.project.lox.Expr.Literal;
import com.project.lox.Expr.Logical;
import com.project.lox.Expr.Set;
import com.project.lox.Expr.Super;
import com.project.lox.Expr.This;
import com.project.lox.Expr.Unary;
import com.project.lox.Expr.Variable;
import com.project.lox.Stmt.Block;
import com.project.lox.Stmt.Class;
import com.project.lox.Stmt.Expression;
import com.project.lox.Stmt.Function;
import com.project.lox.Stmt.If;
import com.project.lox.Stmt.Print;
// import com.project.lox.Stmt.Return;
import com.project.lox.Stmt.Var;
import com.project.lox.Stmt.While;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    Memory globals = new Memory();
    private Memory memory = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new LoxCallable() {

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public String toString() {
                return "<native function>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    private String stringify(Object value) {
        if (value == null)
            return "nil";
        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0"))
                text = text.substring(0, text.length() - 2);
            return text;
        }
        return value.toString();
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return isEqual(left, right);
            case BANG_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return !isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                } else if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                throw new RuntimeError(expr.operator, "Operands do not match");
            default:
                return null;
        }
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double)
            return;
        throw new RuntimeError(operator, "Operands must be numbers");
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null)
            return true;
        if (left == null)
            return false;
        return left.equals(right);
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
            default:
                return null;
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return (Boolean) object;
        if (object instanceof Double)
            return (Double) object != 0.0;
        return true;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        memory.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return memory.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }

    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            memory.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements, new Memory(memory));
        return null;
    }

    void executeBlock(List<Stmt> statements, Memory memory) {
        Memory previous = this.memory;
        try {
            this.memory = memory;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.memory = previous;
        }
    }

    @Override
    public Void visitIfStmt(If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) {
        Object left = evaluate(expr.left);
        if ((expr.operator.type == TokenType.OR && isTruthy(left)
                || (expr.operator.type == TokenType.AND && !isTruthy(left))))
            return left;
        return evaluate(expr.right);
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }
        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + " arguments, but got " + arguments.size());
        }
        return function.call(this, arguments);
    }

    @Override
    public Void visitFunctionStmt(Function stmt) {
        LoxFunction function = new LoxFunction(stmt, memory, false);
        memory.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null)
            value = evaluate(stmt.value);
        throw new Return(value);
    }

    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    @Override
    public Void visitClassStmt(Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class");
            }
        }
        memory.define(stmt.name.lexeme, null);
        if (stmt.superclass != null) {
            memory = new Memory(memory);
            memory.define("super", superclass);
        }
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(method, memory, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }
        LoxClass cls = new LoxClass(stmt.name.lexeme, (LoxClass) superclass, methods);
        if (superclass != null) {
            memory = memory.enclosing;
        }
        memory.assign(stmt.name, cls);
        return null;
    }

    @Override
    public Object visitGetExpr(Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }
        throw new RuntimeError(expr.name, "Only insances have properties");
    }

    @Override
    public Object visitSetExpr(Set expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields");
        }
        Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(This expr) {
        return lookupVariable(expr.keyword, expr);
    }

    @Override
    public Object visitSuperExpr(Super expr) {
        int distance = locals.get(expr);
        LoxClass superclass = (LoxClass) memory.getAt(distance, "super");
        LoxInstance object = (LoxInstance) memory.getAt(distance - 1, "this");
        LoxFunction method = superclass.findMethod(expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'");
        }
        return method.bind(object);
    }

}