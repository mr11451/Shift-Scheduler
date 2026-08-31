package com.shiftscheduler.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.StaffRepository;
import com.shiftscheduler.server.util.PasswordUtil;

@Component
public class InitializeTestPasswordsRunner implements CommandLineRunner {
  
  private final StaffRepository staffRepository;

  public InitializeTestPasswordsRunner(StaffRepository staffRepository) {
    this.staffRepository = staffRepository;
  }

  /**
   * On startup, assign a deterministic test password to any staff record that has none yet
   * (development convenience only).
   */
  @Override
  public void run(String... args) throws Exception {
    // Set test passwords for all staffs that don't have a password yet
    var staffs = staffRepository.findAll();
    for (Staff staff : staffs) {
      if (staff.getPasswordHash() == null || staff.getPasswordHash().isEmpty()) {
        // Generate test password based on staff code
        String testPassword = "test_" + staff.getStaffCode().toLowerCase();
        String hashedPassword = PasswordUtil.hashPassword(testPassword);
        staff.setPasswordHash(hashedPassword);
        staffRepository.save(staff);
        System.out.println("✓ Initialized test password for staff: " + staff.getStaffCode() + " (password: " + testPassword + ")");
      }
    }
  }
}
