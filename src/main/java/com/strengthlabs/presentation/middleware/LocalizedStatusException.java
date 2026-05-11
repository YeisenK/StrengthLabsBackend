package com.strengthlabs.presentation.middleware;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LocalizedStatusException extends ResponseStatusException {

    private final String messageKey;

    public LocalizedStatusException(HttpStatus status, String messageKey) {
        super(status, messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
