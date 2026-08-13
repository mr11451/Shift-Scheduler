export const DEFAULT_ROLE_LABELS = {
  MASTER: "マスター",
  CHIEF: "チーフ",
  MEMBER: "メンバー",
};

export function parseRoleLabels(rawValue = "") {
  if (!rawValue || typeof rawValue !== "string") {
    return { ...DEFAULT_ROLE_LABELS };
  }

  try {
    const parsed = JSON.parse(rawValue);
    if (!parsed || typeof parsed !== "object") {
      return { ...DEFAULT_ROLE_LABELS };
    }

    return {
      MASTER: String(parsed.MASTER || DEFAULT_ROLE_LABELS.MASTER),
      CHIEF: String(parsed.CHIEF || DEFAULT_ROLE_LABELS.CHIEF),
      MEMBER: String(parsed.MEMBER || DEFAULT_ROLE_LABELS.MEMBER),
    };
  } catch {
    return { ...DEFAULT_ROLE_LABELS };
  }
}
