package com.example.sleepknowledge.authentication;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("이미 사용 중인 사용자 이름입니다.");
    }
}
