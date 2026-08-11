package com.shiftscheduler.server.filter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ApiAccessLogFilterTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private final ApiAccessLogFilter filter = new ApiAccessLogFilter();
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(ApiAccessLogFilter.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    void doFilter_logsApiAccessInfo() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/staffs");
        when(request.getQueryString()).thenReturn("page=1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getAttribute("staffId")).thenReturn(12L);
        when(request.getAttribute("roleLevel")).thenReturn("MASTER");
        when(response.getStatus()).thenReturn(200);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertTrue(
                listAppender.list.stream()
                        .anyMatch(event -> event.getFormattedMessage().contains("API access: method=GET path=/api/staffs?page=1 status=200")
                                && event.getFormattedMessage().contains("staffId=12")
                                && event.getFormattedMessage().contains("roleLevel=MASTER")));
    }
}