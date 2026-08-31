// Whether the current user may select targetStaffId in a staff/calendar picker: always allowed
// for themselves, otherwise only if they're viewable or have approved calendar-view access.
export function canSelectTarget({ targetStaffId, currentStaffId, viewableStaffIds = [], approvedTargetStaffIds = [] }) {
  const normalizedTargetId = Number(targetStaffId);
  const normalizedCurrentId = Number(currentStaffId);

  if (!Number.isFinite(normalizedTargetId)) {
    return false;
  }

  if (Number.isFinite(normalizedCurrentId) && normalizedTargetId === normalizedCurrentId) {
    return true;
  }

  return viewableStaffIds.some((staffId) => Number(staffId) === normalizedTargetId)
    || approvedTargetStaffIds.some((staffId) => Number(staffId) === normalizedTargetId);
}
