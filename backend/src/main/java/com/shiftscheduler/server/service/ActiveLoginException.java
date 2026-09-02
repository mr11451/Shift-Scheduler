package com.shiftscheduler.server.service;

public class ActiveLoginException extends IllegalArgumentException {
    public ActiveLoginException() {
        super("このメンバーはすでに別の端末でログインしています。同時ログインはできません。");
    }
}