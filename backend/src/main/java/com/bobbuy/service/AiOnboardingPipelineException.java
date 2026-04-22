package com.bobbuy.service;

public class AiOnboardingPipelineException extends RuntimeException {
    private final String stage;
    private final String messageKey;

    public AiOnboardingPipelineException(String stage, String messageKey, String message) {
        super(message);
        this.stage = stage;
        this.messageKey = messageKey;
    }

    public String getStage() {
        return stage;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
