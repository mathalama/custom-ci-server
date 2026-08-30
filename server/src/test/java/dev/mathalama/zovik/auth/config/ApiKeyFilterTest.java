package dev.mathalama.zovik.auth.config;

import dev.mathalama.zovik.common.api.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyFilterTest {

    private ApiKeyFilter apiKeyFilter;
    private final String secretKey = "test-secret-api-key";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        apiKeyFilter = new ApiKeyFilter(secretKey);
    }

    @Test
    void testFilter_Success_WhenValidApiKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/projects");
        when(request.getHeader("X-API-Key")).thenReturn(secretKey);

        apiKeyFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testFilter_Bypassed_WhenWebhookUri() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/webhooks/github/1");

        apiKeyFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(request, never()).getHeader("X-API-Key");
    }

    @Test
    void testFilter_Fail_WhenInvalidApiKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/projects");
        when(request.getHeader("X-API-Key")).thenReturn("wrong-key");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        apiKeyFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response, times(1)).setStatus(401);
        verify(response, times(1)).setContentType(MediaType.APPLICATION_JSON_VALUE);

        String responseContent = stringWriter.toString();
        assertTrue(responseContent.contains("Unauthorized"));
    }
}
