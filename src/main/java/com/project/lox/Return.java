package com.project.lox;

public class Return extends RuntimeException {
    private static final long serialVersionUID = -2643165036014377972L;
    final Object value;

    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}