package com.shiftscheduler.server.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.annotation.RequireRole;
import com.shiftscheduler.server.dto.StaffCreateRequest;
import com.shiftscheduler.server.dto.StaffCreateResponse;
import com.shiftscheduler.server.dto.StaffResponse;
import com.shiftscheduler.server.dto.StaffUpdateRequest;
import com.shiftscheduler.server.service.StaffService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/staffs")
public class StaffApiController {
    private final StaffService staffService;

    public StaffApiController(StaffService staffService) {
        this.staffService = staffService;
    }

    /**
     * GET /api/staffs - List staff visible to the requester (falls back to group/active filters
     * when unauthenticated, used by internal callers).
     */
    @GetMapping
    public ResponseEntity<List<StaffResponse>> listStaffs(
            HttpServletRequest request,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String roleLevel) {
        List<Staff> staffs;

        Long requesterStaffId = getAuthenticatedStaffId(request);
        if (requesterStaffId != null) {
            staffs = staffService.getSelectableStaffsForRequester(requesterStaffId);
        } else if (groupId != null) {
            staffs = staffService.getStaffByGroup(groupId);
        } else {
            staffs = staffService.getAllActiveStaff();
        }

        List<StaffResponse> responses = staffs.stream()
                .map(staffService::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/staffs/{staffId} - Retrieve a single staff member
     */
    @GetMapping("/{staffId}")
    public ResponseEntity<StaffResponse> getStaff(@PathVariable Long staffId) {
        return staffService.getStaffById(staffId)
                .map(staff -> ResponseEntity.ok(staffService.convertToResponse(staff)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/staffs/permission-targets - List same-group staff the requester (a MEMBER)
     * could request calendar-view permission from
     */
    @GetMapping("/permission-targets")
    public ResponseEntity<List<StaffResponse>> listCalendarViewPermissionTargets(HttpServletRequest request) {
        Long requesterStaffId = getAuthenticatedStaffId(request);
        if (requesterStaffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<StaffResponse> responses = staffService.getCalendarViewPermissionTargets(requesterStaffId).stream()
                .map(staffService::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * POST /api/staffs - Create a new staff member and, for MEMBER role, issue initial login credentials.
     * Updater staff ID is resolved from JWT-authenticated request context.
     */
    @PostMapping
    public ResponseEntity<StaffCreateResponse> createStaff(@RequestBody StaffCreateRequest request, HttpServletRequest httpRequest) {
        // Validate required fields
        if (request.getStaffName() == null || request.getStaffName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getResponsibility() == null || request.getResponsibility().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getRoleLevel() == null) {
            return ResponseEntity.badRequest().build();
        }

        Long updaterStaffId = getAuthenticatedStaffId(httpRequest);
        if (updaterStaffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        StaffCreateResponse response = staffService.createStaffWithInitialLogin(updaterStaffId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/staffs/{staffId} - Update a staff member's editable fields.
     * Updater staff ID is resolved from JWT-authenticated request context.
     */
    @PutMapping("/{staffId}")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable Long staffId,
            @RequestBody StaffUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long updaterStaffId = getAuthenticatedStaffId(httpRequest);
            if (updaterStaffId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Staff staff = staffService.updateStaff(updaterStaffId, staffId, request);
            return ResponseEntity.ok(staffService.convertToResponse(staff));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/staffs/{staffId} - Deactivate a staff member (logical delete)
     */
    @DeleteMapping("/{staffId}")
    public ResponseEntity<Void> deactivateStaff(@PathVariable Long staffId) {
        try {
            staffService.deactivateStaff(staffId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** POST /api/staffs/{staffId}/force-logout - Invalidate a staff member's active session. */
    @PostMapping("/{staffId}/force-logout")
    @RequireRole(roles = {"CHIEF", "MASTER"})
    public ResponseEntity<Void> forceLogout(@PathVariable Long staffId) {
        try {
            staffService.forceLogout(staffId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/staffs/{staffId}/reactivate - Restore a previously deactivated staff member
     */
    @PostMapping("/{staffId}/reactivate")
    public ResponseEntity<StaffResponse> reactivateStaff(@PathVariable Long staffId) {
        try {
            Staff staff = staffService.reactivateStaff(staffId);
            return ResponseEntity.ok(staffService.convertToResponse(staff));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Extract the authenticated staff ID set by the JWT filter, if present.
     */
    private Long getAuthenticatedStaffId(HttpServletRequest request) {
        Object staffId = request.getAttribute("staffId");
        if (staffId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
