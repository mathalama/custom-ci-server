package dev.mathalama.rabotyagaci.common.config;

import dev.mathalama.rabotyagaci.build.api.event.BuildCreatedEvent;
import dev.mathalama.rabotyagaci.build.api.event.OrchestratorInternalErrorEvent;
import dev.mathalama.rabotyagaci.build.domain.TriggerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GlobalAsyncExceptionHandlerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private GlobalAsyncExceptionHandler globalAsyncExceptionHandler;

    @Test
    void testHandleUncaughtExceptionPublishesFallbackEvent() throws NoSuchMethodException {
        // Arrange
        RuntimeException exception = new RuntimeException("Test exception");
        Method method = TestClass.class.getMethod("testMethod");
        Long expectedBuildId = 123L;
        BuildCreatedEvent event = new BuildCreatedEvent(expectedBuildId, 456L, TriggerType.MANUAL, Instant.now());

        // Act
        globalAsyncExceptionHandler.handleUncaughtException(exception, method, event);

        // Assert
        ArgumentCaptor<OrchestratorInternalErrorEvent> captor = ArgumentCaptor.forClass(OrchestratorInternalErrorEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        
        OrchestratorInternalErrorEvent publishedEvent = captor.getValue();
        assertEquals(expectedBuildId, publishedEvent.buildId());
        assertEquals(exception, publishedEvent.cause());
        assertNotNull(publishedEvent.occurredAt());
    }

    @Test
    void testHandleUncaughtExceptionWithoutBuildId() throws NoSuchMethodException {
        // Arrange
        RuntimeException exception = new RuntimeException("Test exception");
        Method method = TestClass.class.getMethod("testMethod");
        Object eventWithoutBuildId = new Object(); // No buildId method

        // Act
        globalAsyncExceptionHandler.handleUncaughtException(exception, method, eventWithoutBuildId);

        // Assert
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void testHandleUncaughtExceptionWithRecoveryFailure() throws NoSuchMethodException {
        // Arrange
        RuntimeException exception = new RuntimeException("Test exception");
        Method method = TestClass.class.getMethod("testMethod");
        Long expectedBuildId = 123L;
        BuildCreatedEvent event = new BuildCreatedEvent(expectedBuildId, 456L, TriggerType.MANUAL, Instant.now());

        doThrow(new RuntimeException("Recovery failed")).when(eventPublisher).publishEvent(any());

        // Act
        globalAsyncExceptionHandler.handleUncaughtException(exception, method, event);

        // Assert
        verify(eventPublisher).publishEvent(any(OrchestratorInternalErrorEvent.class));
        // We assert that it does not throw an exception out of handleUncaughtException
    }
    
    private static class TestClass {
        public void testMethod() {}
    }
}
