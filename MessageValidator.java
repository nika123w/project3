package com.messenger;

import java.util.regex.Pattern;

public class MessageValidator {
    private static MessageValidator instance;
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\n");

    private MessageValidator() {}

    public static synchronized MessageValidator getInstance() {
        if (instance == null) {
            instance = new MessageValidator();
        }
        return instance;
    }

    public boolean isValidMessage(String message) {
        return message != null && !message.isEmpty() && !NEWLINE_PATTERN.matcher(message).find();
    }
}
