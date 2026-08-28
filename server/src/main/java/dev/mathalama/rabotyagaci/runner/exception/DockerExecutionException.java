package dev.mathalama.rabotyagaci.runner.exception;

import dev.mathalama.rabotyagaci.common.exception.BusinessException;

public class DockerExecutionException extends BusinessException {
    public DockerExecutionException(String message) {
        super(message);
    }

    public DockerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
