import { describe, it, expect } from "vitest";
import { canSelectTarget } from "./viewAccess";

describe("canSelectTarget", () => {
  it("allows the current staff to select themselves", () => {
    expect(canSelectTarget({ targetStaffId: 1, currentStaffId: 1, viewableStaffIds: [], approvedTargetStaffIds: [] })).toBe(true);
  });

  it("allows targets that are already in the viewable list", () => {
    expect(canSelectTarget({ targetStaffId: 2, currentStaffId: 1, viewableStaffIds: [2, 3], approvedTargetStaffIds: [] })).toBe(true);
  });

  it("allows targets that are already approved", () => {
    expect(canSelectTarget({ targetStaffId: 4, currentStaffId: 1, viewableStaffIds: [], approvedTargetStaffIds: [4] })).toBe(true);
  });

  it("blocks targets that are neither viewable nor approved", () => {
    expect(canSelectTarget({ targetStaffId: 5, currentStaffId: 1, viewableStaffIds: [2], approvedTargetStaffIds: [3] })).toBe(false);
  });
});
