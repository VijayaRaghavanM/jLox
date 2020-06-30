package com.project.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private LoxClass cls;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass cls) {
        this.cls = cls;
    }

    @Override
    public String toString() {
        return "<instanceof " + cls.name + ">";
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }
        LoxFunction method = cls.findMethod(name.lexeme);
        if (method != null)
            return method.bind(this);
        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'");
    }

    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);

    }
}
