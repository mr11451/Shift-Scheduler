const WEEKDAY_LABEL_TO_INDEX = {
  "0": 0,
  "1": 1,
  "2": 2,
  "3": 3,
  "4": 4,
  "5": 5,
  "6": 6,
  "日": 0,
  "月": 1,
  "火": 2,
  "水": 3,
  "木": 4,
  "金": 5,
  "土": 6,
};

export function parseHolidayDates(rawValue = "") {
  if (typeof rawValue !== "string") {
    return [];
  }

  return rawValue
    .split(/[\n,]+/)
    .map((value) => value.trim())
    .filter(Boolean)
    .map((value) => normalizeHolidayDate(value))
    .filter(Boolean);
}

export function parseHolidayDatesFromCsv(rawValue = "") {
  if (typeof rawValue !== "string") {
    return [];
  }

  const dateMatches = rawValue.match(/\d{4}\s*[-/年]\s*\d{1,2}\s*[-/月]\s*\d{1,2}\s*日?/g) || [];
  const uniqueDates = new Set(
    dateMatches
      .map((value) => normalizeHolidayDate(value))
      .filter(Boolean)
  );

  return Array.from(uniqueDates);
}

export function parseHolidayWeekdays(rawValue = "") {
  if (typeof rawValue !== "string") {
    return [];
  }

  const uniqueWeekdays = new Set(
    rawValue
      .split(/[\n,]+/)
      .map((value) => normalizeHolidayWeekday(value))
      .filter((value) => Number.isInteger(value) && value >= 0 && value <= 6)
  );

  return Array.from(uniqueWeekdays).sort((left, right) => left - right);
}

export function isHolidayDate(dateStr, holidayDates = [], holidayWeekdays = []) {
  if (holidayDates.includes(dateStr)) {
    return true;
  }

  if (typeof dateStr !== "string" || !dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
    return false;
  }

  const weekday = new Date(`${dateStr}T00:00:00`).getDay();
  return holidayWeekdays.includes(weekday);
}

function normalizeHolidayDate(value) {
  const trimmed = value.trim();

  if (!trimmed) {
    return "";
  }

  const normalized = trimmed.replace(/\s+/g, "");
  const match = normalized.match(/^(\d{4})[-/年](\d{1,2})[-/月](\d{1,2})日?$/);

  if (match) {
    const [, year, month, day] = match;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }

  return trimmed;
}

function normalizeHolidayWeekday(value) {
  const trimmed = String(value || "").trim();

  if (!trimmed) {
    return null;
  }

  return WEEKDAY_LABEL_TO_INDEX[trimmed] ?? null;
}
