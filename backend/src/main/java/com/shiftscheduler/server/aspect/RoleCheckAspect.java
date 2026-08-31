package com.shiftscheduler.server.aspect;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import com.shiftscheduler.server.annotation.RequireRole;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RoleCheckAspect {
    
    /**
     * Enforce {@link RequireRole} on any annotated method by checking the JWT-derived
     * roleLevel request attribute against the annotation's allowed roles.
     */
    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証情報が見つかりません。");
        }
        
        HttpServletRequest request = attributes.getRequest();
        String roleLevel = (String) request.getAttribute("roleLevel");
        
        if (roleLevel == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "認証が必要です。");
        }
        
        String[] allowedRoles = requireRole.roles();
        if (allowedRoles.length > 0 && !Arrays.asList(allowedRoles).contains(roleLevel)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "このリソースにアクセスする権限がありません。");
        }
    }
}
