import { Config } from "@/demo/types/studio";
export const moduleDefaults = {
  mode: "lesson" as "lesson" | "beauty",
  lessonModule: true,
  appointmentModule: false,
  attendanceEnabled: true,
  staffAssignment: false,
  services: "젤 네일, 케어",
  duration: 60,
  depositEnabled: true,
  depositAmount: 20000,
  treatmentHistory: true,
  photoHistory: false,
  revisitEnabled: true,
  revisitDays: 28,
  noShowEnabled: true,
};
export function recommend(c: Config, mode: "lesson" | "beauty"): Config {
  return {
    ...c,
    mode,
    lessonModule: mode === "lesson",
    appointmentModule: mode === "beauty",
    attendanceEnabled: mode === "lesson",
    business: mode === "lesson" ? "필라테스" : "네일",
    reservation: true,
  };
}
export function stepLabels(c: Config) {
  return [
    "사업장 형태",
    c.appointmentModule ? "예약과 담당자" : "수업 방식",
    c.appointmentModule ? "서비스와 예약금" : "이용권 종류",
    "예약 정책",
    c.appointmentModule ? "시술 기록" : "출석과 차감",
    c.appointmentModule ? "재방문 관리" : "재등록 알림",
    "첫 설정",
  ];
}
export function configValid(c: Config) {
  return (
    !!c.name.trim() &&
    (c.lessonModule || c.appointmentModule) &&
    (!c.lessonModule || (c.models.length > 0 && c.passTypes.length > 0)) &&
    (!c.appointmentModule ||
      (c.services.split(",").some((service) => service.trim()) &&
        c.duration > 0 &&
        (!c.revisitEnabled || c.revisitDays > 0)))
  );
}

// Preview-only category boundary; production authorization belongs to Spring.
export const demoSubtypes = {
  lesson: ["댄스", "필라테스", "요가", "PT", "폴댄스", "보컬/음악", "기타 레슨"],
  beauty: ["네일", "속눈썹", "붙임머리", "왁싱", "1인 미용", "기타 뷰티"],
};
export function normalizeDemoConfig(c: Config): Config {
  const mode = c.mode === "beauty" ? "beauty" : "lesson";
  return { ...c, mode, lessonModule: mode === "lesson", appointmentModule: mode === "beauty",
    business: demoSubtypes[mode].includes(c.business) ? c.business : demoSubtypes[mode][0] };
}
