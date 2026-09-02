import { describe, expect, it } from "vitest";
import { getShiftPeriod } from "./shiftPeriod";

describe("getShiftPeriod", () => {
  it("builds a cross-month period for a configured closing day", () => {
    const period = getShiftPeriod(new Date(2026, 8, 2), 25);

    expect(period.startDate).toEqual(new Date(2026, 7, 26));
    expect(period.endDate).toEqual(new Date(2026, 8, 25));
    expect(period.key).toBe("2026-09-25");
  });

  it("uses the month end when the configured day does not exist", () => {
    const period = getShiftPeriod(new Date(2026, 1, 10), 31);

    expect(period.startDate).toEqual(new Date(2026, 1, 1));
    expect(period.endDate).toEqual(new Date(2026, 1, 28));
  });
});