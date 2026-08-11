package com.shiftscheduler.server.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.shiftscheduler.server.util.JwtTokenUtil;
import io.jsonwebtoken.JwtException;

@Component
@Order(2)
public class JwtAuthenticationFilter implements Filter {
    
    // API URLs that do not require authentication
    private static final List<String> PUBLIC_URLS = Arrays.asList(
        "/api/login"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Only API routes are authenticated by this filter.
        if (!requestPath.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Allow OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check if path is public
        if (isPublicUrl(requestPath)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Get JWT token from Authorization header
        String authHeader = httpRequest.getHeader("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json; charset=UTF-8");
            httpResponse.getWriter().write("{\"error\": \"認証が必要です。トークンが見つかりません。\"}");
            return;
        }
        
        try {
            // Validate token
            JwtTokenUtil.validateToken(token);
            
            // Add token to request attributes for later use
            httpRequest.setAttribute("authToken", token);
            httpRequest.setAttribute("staffId", JwtTokenUtil.getStaffIdFromToken(token));
            httpRequest.setAttribute("roleLevel", JwtTokenUtil.getRoleLevelFromToken(token));
            
            chain.doFilter(request, response);
        } catch (JwtException e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json; charset=UTF-8");
            httpResponse.getWriter().write("{\"error\": \"トークンが無効です。\"}");
        }
    }
    
    private boolean isPublicUrl(String path) {
        return PUBLIC_URLS.stream().anyMatch(publicUrl -> {
            if (publicUrl.endsWith("/")) {
                return path.startsWith(publicUrl);
            }
            return path.equals(publicUrl);
        });
    }
}
