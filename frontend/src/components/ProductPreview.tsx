"use client";
import { useState } from "react";
import Link from "next/link";
import { Icon } from "./icons";
export function ProductPreview() {
  const [beauty, setBeauty] = useState(false);
  return (
    <>
      <div
        className="preview-switch segmented"
        aria-label="사업장 미리보기 선택"
      >
        {["레슨 / 스튜디오", "뷰티 / 예약샵"].map((label, i) => (
          <button
            key={label}
            className={beauty === (i === 1) ? "selected" : ""}
            aria-pressed={beauty === (i === 1)}
            onClick={() => setBeauty(i === 1)}
          >
            {label}
          </button>
        ))}
      </div>
      <div aria-live="polite">
        {" "}
        <div className="preview">
          <div className="browser-bar">
            <div>
              <i />
              <i />
              <i />
            </div>
            <span>
              Studio OS / {beauty ? "오브제 뷰티" : "오브제 스튜디오"}
            </span>
            <span>↗</span>
          </div>
          <div className="preview-app">
            <aside>
              <div className="wordmark">
                <span className="brand-mark">S</span>Studio OS
              </div>
              <span className="preview-studio">
                {beauty ? "오브제 뷰티" : "오브제 스튜디오"} ⌄
              </span>
              {(beauty
                ? [
                    ["home", "홈"],
                    ["calendar", "예약 일정"],
                    ["users", "고객"],
                    ["check", "시술 기록"],
                    ["ticket", "결제"],
                    ["home", "재방문"],
                    ["settings", "설정"],
                  ]
                : [
                    ["home", "홈"],
                    ["calendar", "수업 일정"],
                    ["check", "출석"],
                    ["users", "회원"],
                    ["ticket", "이용권"],
                    ["settings", "설정"],
                  ]
              ).map(([icon, label], i) => (
                <div
                  key={label}
                  className={`preview-nav ${i === 0 ? "active" : ""}`}
                >
                  <Icon name={icon} />
                  {label}
                </div>
              ))}
              <div className="preview-side-note">
                <span className="dot success" /> 모든 준비가 끝났어요
              </div>
            </aside>
            <div className="preview-content">
              <div className="preview-heading">
                <div>
                  <span>2026년 9월 9일 수요일</span>
                  <h2>
                    {beauty
                      ? "고객을 맞이하는 하루"
                      : "좋은 수업이 시작되는 하루"}
                  </h2>
                  <p>오늘 사업장의 흐름을 확인하세요.</p>
                </div>
                <Link
                  className="button small"
                  href={
                    beauty
                      ? "/onboarding?mode=beauty"
                      : "/onboarding?mode=lesson"
                  }
                >
                  운영 화면 보기 ↗
                </Link>
              </div>
              <div className="preview-stats">
                {(beauty
                  ? [
                      ["오늘 예약", "3", "오늘 방문할 고객"],
                      ["신규 고객", "1", "처음 만나는 고객"],
                      ["결제 예정", "2", "예약금 확인 포함"],
                      ["재방문 예정", "1", "다시 만날 고객"],
                    ]
                  : [
                      ["오늘 수업", "3", "개인 2 · 그룹 1"],
                      ["예약 회원", "6", "오늘 만날 회원"],
                      ["결제 확인", "1", "확인이 필요해요"],
                      ["이용 중인 회원", "6", "함께하는 회원"],
                    ]
                ).map(([a, b, c]) => (
                  <div key={a}>
                    <span>{a}</span>
                    <strong>
                      {b}
                      <small>
                        {[
                          "오늘 수업",
                          "오늘 예약",
                          "결제 확인",
                          "결제 예정",
                        ].includes(a)
                          ? "건"
                          : "명"}
                      </small>
                    </strong>
                    <p>{c}</p>
                  </div>
                ))}
              </div>
              <div className="preview-panels">
                <section>
                  <div className="preview-panel-title">
                    <strong>{beauty ? "오늘 예약 현황" : "오늘 일정"}</strong>
                    <span>전체 보기 ↗</span>
                  </div>
                  {(beauty
                    ? [
                        ["10:00", "젤 네일", "김하늘 · 60분", "예약금 대기"],
                        ["11:30", "속눈썹 펌", "박지우 · 60분", "확정"],
                        ["16:00", "네일 케어", "윤서아 · 30분", "신규"],
                      ]
                    : [
                        ["10:00", "개인 레슨", "김하늘 · 1 / 1명", "개인"],
                        [
                          "11:30",
                          "그룹 베이직",
                          "박지우 외 3명 · 4 / 6명",
                          "그룹",
                        ],
                        ["16:00", "개인 레슨", "김하늘 · 1 / 1명", "개인"],
                      ]
                  ).map(([t, n, m, k]) => (
                    <div className="preview-lesson" key={t}>
                      <time>{t}</time>
                      <div>
                        <strong>{n}</strong>
                        <span>{m}</span>
                      </div>
                      <em>{k}</em>
                    </div>
                  ))}
                </section>
                <section>
                  <div className="preview-panel-title">
                    <strong>운영 알림</strong>
                    <span className="dot" />
                  </div>
                  <div className="preview-alert">
                    <span className="alert-icon">↻</span>
                    <div>
                      <strong>
                        {beauty
                          ? "다시 만날 때가 됐어요"
                          : "다음 수업도 함께하도록"}
                      </strong>
                      <p>
                        {beauty
                          ? "윤서아 님의 방문 후 4주가 지났어요."
                          : "윤서아 님의 이용권이 2회 남았어요."}
                      </p>
                      <span>{beauty ? "재방문 확인 →" : "재등록 확인 →"}</span>
                    </div>
                  </div>
                  <div className="preview-alert">
                    <span className="alert-icon amber">₩</span>
                    <div>
                      <strong>결제 확인이 필요해요</strong>
                      <p>
                        {beauty
                          ? "김하늘 님의 예약금 20,000원을 확인해주세요."
                          : "윤서아 님의 입금을 확인해주세요."}
                      </p>
                      <span>결제 내역 보기 →</span>
                    </div>
                  </div>
                </section>
              </div>
              <div className="preview-bottom">
                <span className="dot success" /> 예약부터 다음 방문까지, 하나의
                흐름으로.
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
