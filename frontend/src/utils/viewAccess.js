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
