package dev.mathalama.rabotyagaci.common.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private final GlobalAsyncExceptionHandler globalAsyncExceptionHandler;

    public AsyncConfig(GlobalAsyncExceptionHandler globalAsyncExceptionHandler) {
        this.globalAsyncExceptionHandler = globalAsyncExceptionHandler;
    }

    // In Spring Boot 3.2+, when `spring.threads.virtual.enabled=true` is set,
    // the default Executor for @Async automatically switches to Virtual Threads.
    // Simply enabling async support with the @EnableAsync annotation is sufficient.

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return globalAsyncExceptionHandler;
    }
}