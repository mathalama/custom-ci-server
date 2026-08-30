package dev.mathalama.zovik.common.config;

import dev.mathalama.zovik.build.api.event.OrchestratorInternalErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Uncaught exception in @Async method: {}.{}", method.getDeclaringClass().getSimpleName(), method.getName(), ex);
        
        boolean buildIdFound = false;
        for (Object param : params) {
            if (param != null) {
                Long buildId = extractBuildId(param);
                if (buildId != null) {
                    buildIdFound = true;
                    log.error("Extracted buildId: {} from event {}. Publishing OrchestratorInternalErrorEvent to fail the build safely.", buildId, param.getClass().getSimpleName());
                    try {
                        eventPublisher.publishEvent(new OrchestratorInternalErrorEvent(buildId, ex, Instant.now()));
                    } catch (Exception recoveryEx) {
                        log.error("CRITICAL: Failed to publish OrchestratorInternalErrorEvent for build {}. Recovery handler threw exception!", buildId, recoveryEx);
                    }
                    return; // Break after first buildId found to avoid duplicates
                }
            }
        }
        
        if (!buildIdFound) {
            log.error("CRITICAL: No buildId found in method parameters, cannot recover state for event. Event types: {}. This event type requires a 'buildId()' accessor for Global Async Recovery to work. State may be corrupted or hung.", getParamTypes(params));
        }
    }

    private String getParamTypes(Object[] params) {
        if (params == null || params.length == 0) return "none";
        java.util.List<String> types = new java.util.ArrayList<>();
        for (Object p : params) {
            types.add(p != null ? p.getClass().getSimpleName() : "null");
        }
        return String.join(", ", types);
    }

    private Long extractBuildId(Object event) {
        try {
            Method buildIdMethod = event.getClass().getMethod("buildId");
            Object result = buildIdMethod.invoke(event);
            if (result instanceof Long id) {
                return id;
            }
        } catch (NoSuchMethodException e) {
            // Event doesn't have buildId() method, ignore
        } catch (Exception e) {
            log.warn("Failed to extract buildId from event: {}", event.getClass().getSimpleName(), e);
        }
        return null;
    }
}
