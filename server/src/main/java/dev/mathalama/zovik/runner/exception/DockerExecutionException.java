package dev.mathalama.zovik.runner.exception;

import dev.mathalama.zovik.common.exception.BusinessException;

public class DockerExecutionException extends BusinessException {
    public DockerExecutionException(String message) {
        super(message);
    }

    public DockerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
