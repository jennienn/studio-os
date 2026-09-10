"use client";
import { useEffect, useState } from "react";
import { AppShell } from "./AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatStrip } from "@/components/StatStrip";
import { useStudio, owned } from "@/demo/lib/store";
import { Appointment } from "@/demo/types/studio";
export function ServiceOperations({ page }: { page: string }) {
  const { data, setData } = useStudio();
  const c = data.config;
  const [date, setDate] = useState("2026-09-09");
  const [query, search] = useState("");
  const [creating, create] = useState(false);
  const [message, tell] = useState("");
  useEffect(() => {
    if (new URLSearchParams(window.location.search).has("new")) create(true);
  }, []);
  const name = (id: string) =>
    data.members.find((m) => m.id === id)?.name || "고객";
  const patch = (id: string, changes: Partial<Appointment>) =>
    setData((d) => ({
      ...d,
      appointments: d.appointments.map((a) =>
        a.id === id ? { ...a, ...changes } : a,
      ),
    }));
  const daily = data.appointments.filter((a) => a.date === date);
  const history = data.appointments.filter((a) => a.status === "완료");
  const revisits = data.members
    .map((m) => ({
      m,
      last: history
        .filter((a) => a.member_id === m.id)
        .sort((a, b) => b.date.localeCompare(a.date))[0],
    }))
    .filter(
      ({ last }) =>
        last &&
        (new Date("2026-09-09").getTime() - new Date(last.date).getTime()) /
          86400000 >=
          c.revisitDays,
    );
  const titles: Record<string, string> = {
    dashboard: "오늘의 운영",
    members: "고객",
    appointments: "예약 일정",
    treatments: "시술 이력",
    payments: "결제",
    revisits: "재방문",
  };
  const enabled =
    (page !== "treatments" || c.treatmentHistory) &&
    (page !== "revisits" || c.revisitEnabled) &&
    (page !== "appointments" || c.reservation);
  function confirm(a: Appointment) {
    if (a.depositPaid) return;
    setData((d) => ({
      ...d,
      appointments: d.appointments.map((x) =>
        x.id === a.id ? { ...x, depositPaid: true } : x,
      ),
      payments: [
        ...d.payments,
        {
          ...owned(),
          member_id: a.member_id,
          date: "2026-09-09",
          amount: a.deposit,
        },
      ],
    }));
    tell("예약금 입금을 확인했습니다.");
  }
  function save(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const v = (k: string) => String(f.get(k) || "").trim();
    if (page === "members") {
      if (!v("name")) return;
      setData((d) => ({
        ...d,
        members: [
          ...d.members,
          {
            ...owned(),
            name: v("name"),
            phone: v("phone"),
            pass_id: "",
            used: 0,
            paid: true,
            first: "2026-09-09",
            expires: "",
          },
        ],
      }));
    } else {
      setData((d) => ({
        ...d,
        appointments: [
          ...d.appointments,
          {
            ...owned(),
            member_id: v("customer"),
            service: v("service"),
            date: v("date"),
            time: v("time"),
            duration: c.duration,
            staff: c.staffAssignment ? v("staff") : "원장",
            deposit: c.depositEnabled ? c.depositAmount : 0,
            depositPaid: !c.depositEnabled,
            status: "예정",
            note: "",
            photo: "",
          },
        ],
      }));
    }
    create(false);
    tell("등록했습니다.");
  }
  const appointmentRows = (rows: Appointment[]) =>
    rows.length ? (
      rows.map((a) => (
        <div className="list-row" key={a.id}>
          <div className="time">{a.time}</div>
          <div className="grow">
            <strong>
              {name(a.member_id)} · {a.service}
            </strong>
            <span>
              {a.date} · {a.duration}분
              {c.staffAssignment ? ` · 담당 ${a.staff}` : ""}
            </span>
          </div>
          <span className="status">{a.status}</span>
          {a.status === "예정" && (
            <>
              <button
                className="button small"
                onClick={() => patch(a.id, { status: "완료" })}
              >
                방문 완료
              </button>
              {c.noShowEnabled && (
                <button
                  className="button small"
                  onClick={() => patch(a.id, { status: "노쇼" })}
                >
                  노쇼
                </button>
              )}
            </>
          )}
        </div>
      ))
    ) : (
      <div className="empty-state">예약된 고객이 없습니다.</div>
    );
  return (
    <AppShell>
      <div className="workspace-top">
        <span>{c.name}</span>
        <span>데모 · 2026년 9월 9일</span>
      </div>
      <PageHeader
        title={titles[page]}
        description="예약부터 고객 기록, 다음 방문까지 한곳에서 관리하세요."
        action={
          page === "members" || (page === "appointments" && c.reservation) ? (
            <button className="button primary" onClick={() => create(true)}>
              + {page === "members" ? "고객 등록" : "예약 추가"}
            </button>
          ) : undefined
        }
      />
      {message && (
        <p role="status" className="hint-card">
          {message}
        </p>
      )}
      {!enabled ? (
        <div className="empty-state">
          이 기능은 꺼져 있습니다. <a href="/settings">설정에서 변경하기 →</a>
        </div>
      ) : (
        <>
          {page === "dashboard" && (
            <>
              <StatStrip
                items={[
                  {
                    label: "오늘 예약",
                    value: String(daily.length),
                    note: "방문 예정 포함",
                  },
                  {
                    label: "신규 고객",
                    value: String(
                      data.members.filter((m) => m.first === date).length,
                    ),
                    note: "오늘 첫 등록",
                  },
                  {
                    label: "결제 예정",
                    value: String(
                      c.depositEnabled
                        ? data.appointments.filter((a) => !a.depositPaid).length
                        : 0,
                    ),
                    note: "예약금 확인 필요",
                  },
                  {
                    label: "재방문 예정",
                    value: String(c.revisitEnabled ? revisits.length : 0),
                    note: `방문 후 ${c.revisitDays}일`,
                  },
                ]}
              />
              <div className="grid two">
                <section className="panel">
                  <div className="panel-head">
                    <h2>오늘 예약 현황</h2>
                    <a className="text-link" href="/appointments">
                      예약 일정 →
                    </a>
                  </div>
                  {appointmentRows(daily)}
                </section>
                <section className="panel">
                  <div className="panel-head">
                    <h2>운영 알림</h2>
                  </div>
                  {c.notifications &&
                    c.depositEnabled &&
                    data.appointments
                      .filter((a) => !a.depositPaid)
                      .map((a) => (
                        <a href="/payments" className="notice" key={a.id}>
                          <span className="dot warning" />
                          <div>
                            <strong>{name(a.member_id)} · 예약금 확인</strong>
                            <span>
                              {a.deposit.toLocaleString()}원 · {a.service}
                            </span>
                          </div>
                        </a>
                      ))}
                  {c.notifications &&
                    c.revisitEnabled &&
                    revisits.map(({ m }) => (
                      <a href="/revisits" className="notice" key={m.id}>
                        <span className="dot" />
                        <div>
                          <strong>{m.name} · 재방문 확인</strong>
                          <span>다음 방문을 준비할 시점이에요.</span>
                        </div>
                      </a>
                    ))}
                </section>
              </div>
              {c.lessonModule && (
                <div className="hint-card">
                  <strong>레슨도 함께 운영 중입니다.</strong>
                  <p>
                    <a href="/schedule">수업 일정</a> ·{" "}
                    <a href="/attendance">출석 관리</a> ·{" "}
                    <a href="/passes">이용권 관리</a>
                  </p>
                </div>
              )}
            </>
          )}
          {page === "members" && (
            <>
              <div className="toolbar">
                <input
                  className="input search"
                  aria-label="고객 검색"
                  placeholder="이름 또는 전화번호 검색"
                  value={query}
                  onChange={(e) => search(e.target.value)}
                />
              </div>
              <section className="panel">
                {data.members
                  .filter((m) => (m.name + m.phone).includes(query))
                  .map((m) => (
                    <details className="customer-detail" key={m.id}>
                      <summary>
                        <strong>{m.name}</strong>
                        <span>{m.phone}</span>
                        <span>기록 보기 ↓</span>
                      </summary>
                      <div className="modal-body">
                        <h3>예약 / 방문 이력</h3>
                        {appointmentRows(
                          data.appointments.filter((a) => a.member_id === m.id),
                        )}
                        <h3>결제 이력</h3>
                        {data.payments
                          .filter((p) => p.member_id === m.id)
                          .map((p) => (
                            <p className="muted" key={p.id}>
                              {p.date} · {p.amount.toLocaleString()}원
                            </p>
                          ))}
                        {c.treatmentHistory && (
                          <a className="text-link" href="/treatments">
                            시술 메모 확인 →
                          </a>
                        )}
                      </div>
                    </details>
                  ))}
                {!data.members.some((m) =>
                  (m.name + m.phone).includes(query),
                ) && <div className="empty-state">검색 결과가 없습니다.</div>}
              </section>
            </>
          )}
          {page === "appointments" && (
            <>
              <div className="toolbar">
                <label className="config-field">
                  <span>예약 날짜</span>
                  <input
                    type="date"
                    className="input"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                  />
                </label>
                <span className="muted">
                  기본 {c.duration}분 ·{" "}
                  {c.staffAssignment ? "담당자 지정" : "1:1 예약"}
                </span>
              </div>
              <section className="panel">{appointmentRows(daily)}</section>
            </>
          )}
          {page === "treatments" && (
            <section className="panel">
              {history.length ? (
                history.map((a) => (
                  <div className="treatment-entry" key={a.id}>
                    <div className="panel-head">
                      <h2>
                        {name(a.member_id)} · {a.service}
                      </h2>
                      <span className="muted">{a.date}</span>
                    </div>
                    <div className="modal-body">
                      <label className="config-field">
                        <span>시술 메모</span>
                        <textarea
                          className="input treatment-note"
                          value={a.note}
                          onChange={(e) =>
                            patch(a.id, { note: e.target.value })
                          }
                        />
                      </label>
                      {c.photoHistory && (
                        <label className="config-field">
                          <span>시술 사진 (데모 · 500KB 이하 이미지)</span>
                          <input
                            type="file"
                            accept="image/png,image/jpeg,image/webp"
                            onChange={(e) => {
                              const f = e.target.files?.[0];
                              if (!f) return;
                              if (
                                f.size > 500000 ||
                                ![
                                  "image/png",
                                  "image/jpeg",
                                  "image/webp",
                                ].includes(f.type)
                              ) {
                                tell(
                                  "500KB 이하 PNG, JPG, WebP 이미지를 선택해주세요.",
                                );
                                return;
                              }
                              const reader = new FileReader();
                              reader.onload = () =>
                                patch(a.id, { photo: String(reader.result) });
                              reader.readAsDataURL(f);
                            }}
                          />
                          {a.photo && (
                            <img
                              src={a.photo}
                              alt={`${name(a.member_id)} 시술 기록`}
                              width={160}
                            />
                          )}
                        </label>
                      )}
                      <p className="muted">
                        변경 사항은 이 브라우저에 저장됩니다.
                      </p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="empty-state">방문 완료된 시술이 없습니다.</div>
              )}
            </section>
          )}
          {page === "payments" && (
            <>
              <StatStrip
                items={[
                  {
                    label: "수납 합계",
                    value: data.payments
                      .reduce((n, p) => n + p.amount, 0)
                      .toLocaleString(),
                    note: "원 · 공유 결제 원장",
                  },
                  {
                    label: "예약금 확인 필요",
                    value: String(
                      c.depositEnabled
                        ? data.appointments.filter((a) => !a.depositPaid).length
                        : 0,
                    ),
                    note: "예약별 확인",
                  },
                ]}
              />
              <section className="panel">
                {c.depositEnabled ? (
                  data.appointments.map((a) => (
                    <div className="list-row" key={a.id}>
                      <div className="grow">
                        <strong>
                          {name(a.member_id)} · {a.service}
                        </strong>
                        <span>
                          {a.date} · 예약금 {a.deposit.toLocaleString()}원
                        </span>
                      </div>
                      <button
                        className="button small"
                        disabled={a.depositPaid}
                        onClick={() => confirm(a)}
                      >
                        {a.depositPaid ? "입금 확인됨" : "예약금 확인"}
                      </button>
                    </div>
                  ))
                ) : (
                  <div className="empty-state">예약금을 사용하지 않습니다.</div>
                )}
              </section>
              <h3>결제 내역</h3>
              <section className="panel">
                {data.payments.map((p) => (
                  <div className="list-row" key={p.id}>
                    <strong className="grow">{name(p.member_id)}</strong>
                    <span>{p.date}</span>
                    <strong>{p.amount.toLocaleString()}원</strong>
                  </div>
                ))}
              </section>
            </>
          )}
          {page === "revisits" && (
            <section className="panel">
              <div className="panel-head">
                <h2>방문 후 {c.revisitDays}일이 지난 고객</h2>
                <a className="text-link" href="/settings">
                  주기 변경 →
                </a>
              </div>
              {revisits.length ? (
                revisits.map(({ m, last }) => (
                  <div className="list-row" key={m.id}>
                    <div className="grow">
                      <strong>{m.name}</strong>
                      <span>
                        최근 방문 {last.date} · {last.service}
                      </span>
                    </div>
                    <a className="button small" href="/appointments?new=1">
                      다음 예약 등록
                    </a>
                  </div>
                ))
              ) : (
                <div className="empty-state">
                  현재 재방문 기준에 해당하는 고객이 없습니다.
                </div>
              )}
            </section>
          )}
        </>
      )}
      {creating && (
        <div className="modal-backdrop">
          <section
            className="modal"
            role="dialog"
            aria-modal="true"
            aria-label="등록"
          >
            <div className="panel-head">
              <h2>{page === "members" ? "고객 등록" : "예약 추가"}</h2>
              <button
                className="icon-button"
                aria-label="닫기"
                onClick={() => create(false)}
              >
                ×
              </button>
            </div>
            <form className="modal-body config-fields" onSubmit={save}>
              {page === "members" ? (
                <>
                  <label className="config-field">
                    <span>고객 이름</span>
                    <input className="input" name="name" required />
                  </label>
                  <label className="config-field">
                    <span>전화번호</span>
                    <input type="tel" className="input" name="phone" required />
                  </label>
                </>
              ) : (
                <>
                  <label className="config-field">
                    <span>고객</span>
                    <select className="input" name="customer" required>
                      {data.members.map((m) => (
                        <option key={m.id} value={m.id}>
                          {m.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="config-field">
                    <span>서비스 · 기본 {c.duration}분</span>
                    <select className="input" name="service">
                      {c.services
                        .split(",")
                        .map((s) => s.trim())
                        .filter(Boolean)
                        .map((s) => (
                          <option key={s}>{s}</option>
                        ))}
                    </select>
                  </label>
                  <label className="config-field">
                    <span>날짜</span>
                    <input
                      className="input"
                      type="date"
                      name="date"
                      defaultValue={date}
                      required
                    />
                  </label>
                  <label className="config-field">
                    <span>시간</span>
                    <input
                      className="input"
                      type="time"
                      name="time"
                      defaultValue="10:00"
                      required
                    />
                  </label>
                  {c.staffAssignment && (
                    <label className="config-field">
                      <span>담당자</span>
                      <input
                        className="input"
                        name="staff"
                        defaultValue="원장"
                        required
                      />
                    </label>
                  )}
                  {c.depositEnabled && (
                    <p className="muted">
                      예약금 {c.depositAmount.toLocaleString()}원
                    </p>
                  )}
                </>
              )}
              <button className="button primary">등록하기</button>
            </form>
          </section>
        </div>
      )}
    </AppShell>
  );
}
