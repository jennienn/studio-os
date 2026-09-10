"use client";
import { recommend, demoSubtypes } from "@/demo/lib/config";
import { Config } from "@/demo/types/studio";
export const steps = [
  "사업장 정보",
  "수업 방식",
  "이용권 종류",
  "예약 정책",
  "출석과 차감",
  "재등록 알림",
  "첫 설정",
];
export function ConfigFields({
  step,
  c,
  set,
  lockCategory = false,
}: {
  step: number;
  c: Config;
  set: (c: Config) => void;
  lockCategory?: boolean;
}) {
  const field = (key: keyof Config, label: string, options?: string[]) => (
    <label className="config-field" key={key}>
      <span>{label}</span>
      {options ? (
        <select
          className="input"
          value={String(c[key])}
          onChange={(e) => set({ ...c, [key]: e.target.value })}
        >
          {options.map((x) => (
            <option key={x}>{x}</option>
          ))}
        </select>
      ) : (
        <input
          className="input"
          required
          min={0}
          type={typeof c[key] === "number" ? "number" : "text"}
          value={String(c[key])}
          onChange={(e) =>
            set({
              ...c,
              [key]:
                typeof c[key] === "number"
                  ? Number(e.target.value)
                  : e.target.value,
            })
          }
        />
      )}
    </label>
  );
  const choices = (key: "models" | "passTypes", values: string[]) => (
    <div className="select-card-grid two">
      {values.map((x) => (
        <button
          type="button"
          aria-pressed={c[key].includes(x)}
          className={`select-card ${c[key].includes(x) ? "selected" : ""}`}
          key={x}
          onClick={() =>
            set({
              ...c,
              [key]: c[key].includes(x)
                ? c[key].filter((v) => v !== x)
                : [...c[key], x],
            })
          }
        >
          <strong>{x}</strong>
          <span>{c[key].includes(x) ? "✓ 선택됨" : "선택하기"}</span>
        </button>
      ))}
    </div>
  );
  const check = (key: keyof Config, label: string) => (
    <label className="check-line">
      <input
        type="checkbox"
        checked={Boolean(c[key])}
        onChange={(e) => set({ ...c, [key]: e.target.checked })}
      />
      {label}
    </label>
  );
  return (
    <div className="config-fields">
      {step === 1 && (
        <>
          {!lockCategory && <div className="select-card-grid two">
            {[
              [
                "lesson",
                "레슨 / 스튜디오",
                "댄스, 필라테스, PT, 요가, 개인레슨 등",
              ],
              [
                "beauty",
                "뷰티 / 예약 서비스",
                "네일, 속눈썹, 붙임머리, 왁싱 등",
              ],
            ].map(([mode, label, desc]) => (
              <button
                type="button"
                aria-pressed={c.mode === mode}
                className={`select-card ${c.mode === mode ? "selected" : ""}`}
                key={mode}
                onClick={() => set(recommend(c, mode as "lesson" | "beauty"))}
              >
                <strong>{label}</strong>
                <span>{desc}</span>
              </button>
            ))}
          </div>}
          <p className="muted">
            선택한 사업장 유형의 화면을 미리 확인합니다.
          </p>
          {field("name", "사업장 이름")}
          {field("business", "업종", demoSubtypes[c.mode])}
          {field("memberSize", "고객 규모", [
            "1–30명",
            "31–100명",
            "101–300명",
            "300명 이상",
          ])}
          {field("staffSize", "운영 인원", [
            "나 혼자",
            "2–5명",
            "6–10명",
            "10명 이상",
          ])}
        </>
      )}
      {step === 2 &&
        c.lessonModule &&
        choices("models", ["개인 레슨", "그룹 레슨", "자유 이용"])}
      {step === 3 &&
        c.lessonModule &&
        choices("passTypes", ["회차권", "기간권", "월정액", "단회권"])}
      {step === 2 && c.appointmentModule && (
        <>
          {check("reservation", "1:1 예약 사용")}
          {check("staffAssignment", "예약별 담당자 배정")}
        </>
      )}
      {step === 3 && c.appointmentModule && (
        <>
          {field("services", "제공 서비스 (쉼표로 구분)")}
          {field("duration", "예상 소요 시간 (분)")}
          {check("depositEnabled", "예약금 사용")}
          {c.depositEnabled && field("depositAmount", "기본 예약금 (원)")}
        </>
      )}
      {step === 5 && c.appointmentModule && (
        <>
          {check("treatmentHistory", "시술 이력과 메모 관리")}
          {check("photoHistory", "시술 사진 기록 사용")}
        </>
      )}
      {step === 6 && c.appointmentModule && (
        <>
          {check("revisitEnabled", "재방문 주기 관리")}
          {c.revisitEnabled && field("revisitDays", "재방문 주기 (일)")}
          {check("noShowEnabled", "노쇼 상태 관리")}
          {!c.lessonModule && check("notifications", "대시보드 운영 알림 표시")}
        </>
      )}
      {step === 4 && (
        <>
          <label className="check-line">
            <input
              type="checkbox"
              checked={c.reservation}
              onChange={(e) => set({ ...c, reservation: e.target.checked })}
            />{" "}
            사전 예약을 받습니다
          </label>
          {c.reservation && (
            <>
              {field("opens", "예약 오픈 · 예약 며칠 전")}
              {field("closes", "예약 마감 · 예약 몇 시간 전")}
              {field("cancellation", "취소 마감 · 예약 몇 시간 전")}
              {c.lessonModule &&
                field("late", "마감 후 취소", ["회차 차감", "회차 유지"])}
            </>
          )}
        </>
      )}
      {step === 5 && c.lessonModule && (
        <>
          {check("attendanceEnabled", "출석 관리 사용")}
          {field("attendanceBy", "출석 담당", [
            "운영자 직접",
            "강사",
            "회원 체크인",
          ])}
          {field("deduction", "기본 회차 차감 시점", [
            "예약 시",
            "출석 시",
            "수업 완료 시",
          ])}
          <p className="muted">
            이용권을 만들 때 개별 차감 기준을 지정할 수 있어요.
          </p>
        </>
      )}
      {step === 6 && c.lessonModule && (
        <>
          {field("remaining", "잔여 몇 회 이하부터 알릴까요?")}
          {field("expiry", "만료 며칠 전부터 알릴까요?")}
          <label className="check-line">
            <input
              type="checkbox"
              checked={c.notifications}
              onChange={(e) => set({ ...c, notifications: e.target.checked })}
            />{" "}
            대시보드에 재등록 알림 표시
          </label>
        </>
      )}
    </div>
  );
}
