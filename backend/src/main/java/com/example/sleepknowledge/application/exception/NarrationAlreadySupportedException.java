package com.example.sleepknowledge.application.exception;

public class NarrationAlreadySupportedException extends RuntimeException {

    public NarrationAlreadySupportedException() {
        super("이미 음성이 제공되고 있거나 생성 중인 이야기입니다.");
    }
}
