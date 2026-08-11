package com.shiftscheduler.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.dto.LoginRequest;
import com.shiftscheduler.server.dto.LoginResponse;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.util.JwtTokenUtil;
import com.shiftscheduler.server.util.PasswordUtil;

@Service
public class AuthenticationService {
    
    @Autowired
    private StaffRepository staffRepository;
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Find staff by staff code
        Staff staff = staffRepository.findByStaffCode(request.staffCode())
            .orElseThrow(() -> new IllegalArgumentException("スタッフコードまたはパスワードが正しくありません。"));
        
        // Check if staff is active
        if (!staff.getIsActive()) {
            throw new IllegalArgumentException("このスタッフアカウントは無効です。");
        }
        
        // Verify password
        if (staff.getPasswordHash() == null || !PasswordUtil.verifyPassword(request.password(), staff.getPasswordHash())) {
            throw new IllegalArgumentException("スタッフコードまたはパスワードが正しくありません。");
        }
        
        // Generate JWT token
        String token = JwtTokenUtil.generateToken(staff.getId(), staff.getStaffCode(), staff.getRoleLevel().name());
        
        return new LoginResponse(
            staff.getId(),
            staff.getStaffCode(),
            staff.getStaffName(),
            staff.getRoleLevel().name(),
            token
        );
    }
}
