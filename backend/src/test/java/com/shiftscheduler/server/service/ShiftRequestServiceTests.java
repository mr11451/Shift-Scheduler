package com.shiftscheduler.server.service;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shiftscheduler.server.api.ShiftRequestCreateRequest;
import com.shiftscheduler.server.domain.ShiftRequest;
import com.shiftscheduler.server.domain.ShiftRequestStatus;
import com.shiftscheduler.server.domain.ShiftType;
import com.shiftscheduler.server.domain.Staff;
import com.shiftscheduler.server.repository.ShiftRequestRepository;
import com.shiftscheduler.server.repository.ShiftTypeRepository;
import com.shiftscheduler.server.repository.StaffRepository;

@ExtendWith(MockitoExtension.class)
class ShiftRequestServiceTests {

  @Mock
  private ShiftRequestRepository shiftRequestRepository;

  @Mock
  private StaffRepository staffRepository;

  @Mock
  private ShiftTypeRepository shiftTypeRepository;

  @InjectMocks
  private ShiftRequestService shiftRequestService;

  @Test
  void createShiftRequest_rejectsPastWorkDate() {
    Long staffId = 1L;
    Staff staff = new Staff();
    staff.setId(staffId);

    when(staffRepository.findById(staffId)).thenReturn(Optional.of(staff));

    ShiftType shiftType = new ShiftType();
    shiftType.setId(10L);
    shiftType.setShiftName("日勤");
    when(shiftTypeRepository.findById(20L)).thenReturn(Optional.of(shiftType));

    ShiftRequestCreateRequest request = new ShiftRequestCreateRequest();
    request.setWorkDate(LocalDate.now().minusDays(1));
    request.setDesiredShiftTypeId(20L);

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> shiftRequestService.createShiftRequest(staffId, request)
    );

    assertTrue(exception.getMessage().contains("過去"));
  }

  @Test
  void deleteShiftRequest_rejectsPastWorkDate() {
    Long staffId = 1L;
    Staff staff = new Staff();
    staff.setId(staffId);

    ShiftRequest shiftRequest = new ShiftRequest();
    shiftRequest.setId(100L);
    shiftRequest.setStaff(staff);
    shiftRequest.setStatus(ShiftRequestStatus.DRAFT);
    shiftRequest.setWorkDate(LocalDate.now().minusDays(1));

    when(shiftRequestRepository.findById(100L)).thenReturn(Optional.of(shiftRequest));

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> shiftRequestService.deleteShiftRequest(staffId, 100L)
    );

    assertTrue(exception.getMessage().contains("過去"));
  }
}
