package com.shiftscheduler.server.aspect;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.shiftscheduler.server.annotation.RequireRole;

@Aspect
@Component
public class RoleCheckAspect {
    
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
