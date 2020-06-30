package com.project.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Memory closure;
    private boolean isInitializer;

    LoxFunction(Stmt.Function declaration, Memory closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Memory memory = new Memory(this.closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            memory.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(declaration.body, memory);
        } catch (Return returnValue) {
            if (isInitializer)
                return closure.getAt(0, "this");
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }

    public LoxFunction bind(LoxInstance instance) {
        Memory memory = new Memory(closure);
        memory.define("this", instance);
        return new LoxFunction(declaration, memory, isInitializer);
    }

}