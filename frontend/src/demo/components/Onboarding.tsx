"use client";
import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useStudio } from "@/demo/lib/store";
import { configValid, stepLabels, recommend } from "@/demo/lib/config";
import { ConfigFields } from "./ConfigFields";
export function Onboarding() {
  const { ready } = useStudio();
  return ready ? (
    <Suspense
      fallback={<div className="empty-state">설정을 불러오는 중입니다.</div>}
    >
      <OnboardingForm />
    </Suspense>
  ) : (
    <div className="empty-state">설정을 불러오는 중입니다.</div>
  );
}
function OnboardingForm() {
  const { data, setData } = useStudio();
  const params = useSearchParams();
  const [c, set] = useState(() => {
    const mode = params.get("mode");
    return mode === "beauty" || mode === "lesson"
      ? recommend(data.config, mode)
      : data.config;
  });
  const [step, next] = useState(1);
  const [shortcut, choose] = useState("/dashboard");
  const router = useRouter();
  const valid = configValid(c);
  const steps = stepLabels(c);
  return (
    <div className="onboarding-shell">
      <aside className="onboarding-side">
        <a className="brand" href="/">
          <div className="brand-mark">S</div>
          <strong>Studio OS</strong>
        </a>
        <div className="step-list">
          {steps.map((s, i) => (
            <div
              key={s}
              className={`step-item ${step === i + 1 ? "active" : ""} ${step > i + 1 ? "done" : ""}`}
            >
              <div className="step-dot">{step > i + 1 ? "✓" : i + 1}</div>
              <strong>{s}</strong>
            </div>
          ))}
        </div>
        <p className="onboarding-help">
          운영 방식은 나중에 언제든 바꿀 수 있어요.
        </p>
      </aside>
      <main className="onboarding-main">
        <div className="onboarding-main-top">
          <span className="eyebrow">사업장 만들기 · {step} / 7</span>
          <a href="/" className="text-link">
            나가기
          </a>
        </div>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (step < 7) next(step + 1);
            else {
              setData((d) => ({ ...d, config: { ...c, name: c.name.trim() } }));
              router.push(shortcut);
            }
          }}
        >
          <section className="onboarding-content">
            <div className="progress">
              <span style={{ width: `${(step / 7) * 100}%` }} />
            </div>
            <h1 style={{ marginTop: 32 }}>
              {step === 1
                ? "어떤 형태의 사업장을 운영하시나요?"
                : steps[step - 1]}
            </h1>
            <p>
              {
                [
                  "사업장에 맞는 추천 설정으로 시작하세요.",
                  "예약과 운영 방식을 선택해주세요.",
                  "제공하는 상품과 서비스를 설정해주세요.",
                  "예약과 취소 기준을 정해주세요.",
                  "운영에 필요한 방문 기록을 선택해주세요.",
                  "고객의 다음 방문을 준비하세요.",
                  "설정을 확인하고 첫 운영을 시작하세요.",
                ][step - 1]
              }
            </p>
            <ConfigFields step={step} c={c} set={set} />
            {step === 7 && (
              <>
                <div className="review-card">
                  {[
                    ["사업장", c.name],
                    [
                      "사용 기능",
                      [
                        c.lessonModule ? "레슨" : "",
                        c.appointmentModule ? "예약 서비스" : "",
                      ]
                        .filter(Boolean)
                        .join(", "),
                    ],
                    ["업종", c.business],
                    ...(c.lessonModule
                      ? [
                          ["수업 방식", c.models.join(", ")],
                          [
                            "출석 관리",
                            c.attendanceEnabled ? "사용" : "사용 안 함",
                          ],
                        ]
                      : []),
                    ...(c.lessonModule
                      ? [["이용권", c.passTypes.join(", ")]]
                      : []),
                    ...(c.appointmentModule
                      ? [
                          ["서비스", `${c.services} · ${c.duration}분`],
                          [
                            "담당자 배정",
                            c.staffAssignment ? "사용" : "사용 안 함",
                          ],
                          [
                            "예약금",
                            c.depositEnabled
                              ? `${c.depositAmount.toLocaleString()}원`
                              : "사용 안 함",
                          ],
                          [
                            "기록",
                            `시술 ${c.treatmentHistory ? "사용" : "미사용"} · 사진 ${c.photoHistory ? "사용" : "미사용"}`,
                          ],
                          [
                            "재방문 / 노쇼",
                            `${c.revisitEnabled ? c.revisitDays + "일" : "재방문 끔"} · 노쇼 ${c.noShowEnabled ? "사용" : "미사용"}`,
                          ],
                        ]
                      : []),
                    [
                      "예약 정책",
                      c.reservation
                        ? `${c.opens}일 전 오픈 · ${c.closes}시간 전 마감 · 취소 ${c.cancellation}시간 전 · ${c.late}`
                        : "예약 없이 이용",
                    ],
                    ...(c.lessonModule
                      ? [["출석", `${c.attendanceBy} · ${c.deduction} 차감`]]
                      : []),
                    ...(c.lessonModule
                      ? [
                          [
                            "재등록",
                            `잔여 ${c.remaining}회 · 만료 ${c.expiry}일 전`,
                          ],
                        ]
                      : []),
                  ].map(([a, b]) => (
                    <div key={a}>
                      <span>{a}</span>
                      <strong>{b}</strong>
                    </div>
                  ))}
                </div>
                <h3>무엇부터 시작할까요?</h3>
                <div className="select-card-grid two">
                  {[
                    ...(c.lessonModule
                      ? [["/passes?new=1", "첫 이용권 만들기"]]
                      : []),
                    [
                      c.appointmentModule
                        ? "/appointments?new=1"
                        : "/classes?new=1",
                      c.appointmentModule ? "첫 예약 만들기" : "첫 수업 만들기",
                    ],
                    [
                      "/members?new=1",
                      c.lessonModule ? "첫 회원 등록" : "첫 고객 등록",
                    ],
                    ["/dashboard", "나중에 하기"],
                  ].map(([url, label]) => (
                    <button
                      type="button"
                      key={url}
                      onClick={() => choose(url)}
                      className={`select-card ${shortcut === url ? "selected" : ""}`}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </>
            )}
          </section>
          <div className="onboarding-footer">
            <button
              type="button"
              className="button"
              disabled={step === 1}
              onClick={() => next(step - 1)}
            >
              이전
            </button>
            <button className="button primary" disabled={!valid}>
              {step === 7 ? "사업장 시작하기" : "다음 →"}
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
