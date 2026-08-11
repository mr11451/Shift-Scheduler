import { describe, it, expect } from "vitest";
import { parseConfirmedMonths, isMonthConfirmed, getMonthStatus } from "./shiftMonthStatus";

describe("shift month confirmation helpers", () => {
  it("parses confirmed month values from a text setting", () => {
    expect(parseConfirmedMonths("2026-01,2026-02\n2026-03")).toEqual(["2026-01", "2026-02", "2026-03"]);
    expect(parseConfirmedMonths("")).toEqual([]);
  });

  it("checks whether a given month is confirmed", () => {
    expect(isMonthConfirmed("2026-03", ["2026-01", "2026-03"])).toBe(true);
    expect(isMonthConfirmed("2026-02", ["2026-01", "2026-03"])).toBe(false);
  });

  it("returns confirmed styling for confirmed months and pending styling otherwise", () => {
    expect(getMonthStatus({ monthKey: "2026-03", isConfirmed: true })).toMatchObject({
      kind: "confirmed",
      label: "確定済み",
    });
    expect(getMonthStatus({ monthKey: "2026-04", isConfirmed: false })).toMatchObject({
      kind: "pending",
      label: "確定前",
    });
  });
});
