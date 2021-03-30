package com.arthas.core.command.model;

public class MessageModel extends ResultModel {
    private String message;

    public MessageModel() {
    }

    public MessageModel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String getType() {
        return "message";
    }
}
