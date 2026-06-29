package com.poprc.demo.exception;

public class ArquivoInvalidoException extends RuntimeException {
    public ArquivoInvalidoException(String message) {
        super(message);
    }
}