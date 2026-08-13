import { describe, expect, it } from "vitest";
import { isHolidayDate, parseHolidayDates, parseHolidayDatesFromCsv, parseHolidayWeekdays } from "./holidayDates";

describe("holiday date helpers", () => {
  it("parses free-form holiday date text", () => {
    expect(parseHolidayDates("2026-01-01, 2026/05/06\n2026年7月7日")).toEqual([
      "2026-01-01",
      "2026-05-06",
      "2026-07-07",
    ]);
  });

  it("extracts normalized holiday dates from csv content", () => {
    const csv = [
      "date,name,notes",
      '2026/01/01,元日,"closed"',
      '2026年5月6日,振替休日,"closed"',
      '"2026-01-01",duplicate,skip',
    ].join("\n");

    expect(parseHolidayDatesFromCsv(csv)).toEqual([
      "2026-01-01",
      "2026-05-06",
    ]);
  });

  it("parses holiday weekdays from numeric or Japanese labels", () => {
    expect(parseHolidayWeekdays("0,6\n水")).toEqual([0, 3, 6]);
  });

  it("treats explicit dates and selected weekdays as holidays", () => {
    expect(isHolidayDate("2026-01-01", ["2026-01-01"], [])).toBe(true);
    expect(isHolidayDate("2026-01-03", [], [6])).toBe(true);
    expect(isHolidayDate("2026-01-05", [], [0, 6])).toBe(false);
  });
});