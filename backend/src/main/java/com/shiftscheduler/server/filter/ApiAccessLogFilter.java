package com.shiftscheduler.server.filter;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class ApiAccessLogFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ApiAccessLogFilter.class);

    /**
     * Log method/path/status/timing/staff for every /api/ request, passing the request through unchanged.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestPath = httpRequest.getRequestURI();
        if (!requestPath.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMillis = System.currentTimeMillis() - startTime;
            Object staffId = httpRequest.getAttribute("staffId");
            String roleLevel = (String) httpRequest.getAttribute("roleLevel");
            String queryString = httpRequest.getQueryString();
            String pathWithQuery = queryString == null ? requestPath : requestPath + "?" + queryString;

            logger.info(
                    "API access: method={} path={} status={} elapsedMs={} staffId={} roleLevel={}",
                    httpRequest.getMethod(),
                    pathWithQuery,
                    httpResponse.getStatus(),
                    elapsedMillis,
                    Objects.toString(staffId, "anonymous"),
                    Objects.toString(roleLevel, "anonymous"));
        }
    }
}