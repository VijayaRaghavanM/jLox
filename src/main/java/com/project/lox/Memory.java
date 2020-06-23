package com.project.lox;

import java.util.HashMap;
import java.util.Map;

class Memory {
    final Memory enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Memory() {
        enclosing = null;
    }

    Memory(Memory enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        if (this.enclosing != null)
            return enclosing.get(name);
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'");
    }

    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        if (this.enclosing != null) {
            this.enclosing.assign(name, value);
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "' in " + enclosing.values.toString());
    }
}