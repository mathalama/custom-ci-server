package dev.mathalama.zovik.pipeline.exception;

import dev.mathalama.zovik.common.exception.BusinessException;

/**
 * Thrown when pipeline definition YAML configuration is invalid or cannot be parsed.
 */
public class PipelineParseException extends BusinessException {

    public PipelineParseException(String message) {
        super(message);
    }

    public PipelineParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
