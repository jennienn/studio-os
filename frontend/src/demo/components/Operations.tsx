"use client";
import { ServiceOperations } from "./ServiceOperations";
import { useEffect, useState } from "react";
import { AppShell } from "./AppShell";
import { PageHeader } from "@/components/PageHeader";
import { StatStrip } from "@/components/StatStrip";
import { useStudio, owned, attend } from "@/demo/lib/store";
import { Member, Pass, Lesson } from "@/demo/types/studio";
export function EmptyState({ children }: { children: React.ReactNode }) {
  return <div className="empty-state">{children}</div>;
}
function Field({
  name,
  label,
  type = "text",
  value,
}: {
  name: string;
  label: string;
  type?: string;
  value?: string | number;
}) {
  return (
    <label className="config-field">
      <span>{label}</span>
      <input
        className="input"
        name={name}
        type={type}
        defaultValue={value}
        min={type === "number" ? 0 : undefined}
        required
      />
    </label>
  );
}
function Select({
  name,
  label,
  options,
  value,
}: {
  name: string;
  label: string;
  options: string[][];
  value?: string;
}) {
  return (
    <label className="config-field">
      <span>{label}</span>
      <select className="input" name={name} defaultValue={value}>
        {options.map(([v, t]) => (
          <option value={v} key={v}>
            {t || v}
          </option>
        ))}
      </select>
    </label>
  );
}
function LessonOperations({ page }: { page: string }) {
  const { data, setData } = useStudio();
  const { config: c, members, passes, lessons } = data;
  const [query, search] = useState("");
  const [filter, setFilter] = useState("전체");
  const [modal, open] = useState("");
  const [detailSnapshot, show] = useState<Member | null>(null);
  const detail = members.find((m) => m.id === detailSnapshot?.id) || null;
  const [notice, notify] = useState("");
  const [date, setDate] = useState("2026-09-09");
  const [month, setMonth] = useState("2026-09");
  const [group, setGroup] = useState("");
  const [selected, select] = useState<string[]>([]);
  useEffect(() => {
    if (new URLSearchParams(window.location.search).has("new"))
      open(
        page === "members" ? "member" : page === "passes" ? "pass" : "lesson",
      );
  }, [page]);
  const available = lessons.filter((l) => c.models.includes(l.model));
  const dayLessons = available.filter((l) => l.date === date);
  const groups = dayLessons.filter((l) => l.model === "그룹 레슨");
  const active = groups.find((l) => l.id === group) || groups[0];
  const remaining = (m: Member) => {
    const p = passes.find((p) => p.id === m.pass_id);
    return p?.sessions ? Math.max(0, p.sessions - m.used) : null;
  };
  const renew = (m: Member) =>
    !m.paid ||
    (remaining(m) !== null && remaining(m)! <= c.remaining) ||
    (new Date(m.expires).getTime() - new Date("2026-09-09").getTime()) /
      86400000 <=
      c.expiry;
  const hoursUntil = (l: Lesson) =>
    (new Date(l.date + "T" + l.time).getTime() -
      new Date("2026-09-09T09:00:00").getTime()) /
    3600000;
  function finish(l: Lesson) {
    setData((d) => ({
      ...d,
      lessons: d.lessons.map((x) =>
        x.id === l.id ? { ...x, completed: true } : x,
      ),
      members: d.members.map((m) => {
        const p = d.passes.find((p) => p.id === m.pass_id);
        return !l.completed &&
          l.member_ids.includes(m.id) &&
          p?.deduction === "수업 완료 시" &&
          m.used < p.sessions
          ? { ...m, used: m.used + 1 }
          : m;
      }),
    }));
    notify("수업을 완료했습니다.");
  }
  function save(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const v = (k: string) => String(f.get(k) || "");
    if (modal === "pass") {
      const p: Pass = {
        ...owned(),
        name: v("name").trim(),
        type: v("type"),
        sessions: ["기간권", "월정액"].includes(v("type"))
          ? 0
          : Number(v("sessions")),
        days: Number(v("days")),
        price: Number(v("price")),
        start: v("start"),
        deduction: v("deduction"),
      };
      if (
        !p.name ||
        p.days < 1 ||
        (!["기간권", "월정액"].includes(p.type) && p.sessions < 1)
      ) {
        notify("이름과 유효한 횟수·기간을 입력해주세요.");
        return;
      }
      setData((d) => ({ ...d, passes: [...d.passes, p] }));
    } else if (modal === "member") {
      if (!v("name").trim()) return;
      const p = passes.find((p) => p.id === v("pass_id"));
      const expiry = new Date("2026-09-09T12:00:00");
      expiry.setDate(expiry.getDate() + (p?.days || 30));
      setData((d) => ({
        ...d,
        members: [
          ...d.members,
          {
            ...owned(),
            name: v("name").trim(),
            phone: v("phone"),
            pass_id: v("pass_id"),
            used: 0,
            paid: false,
            first: "2026-09-09",
            expires: expiry.toISOString().slice(0, 10),
          },
        ],
      }));
    } else {
      if (!v("name").trim() || Number(v("capacity")) < 1) return;
      setData((d) => ({
        ...d,
        lessons: [
          ...d.lessons,
          {
            ...owned(),
            name: v("name").trim(),
            model: v("model"),
            date: v("date"),
            time: v("time"),
            capacity: Number(v("capacity")),
            member_ids: [],
            completed: false,
          },
        ],
      }));
    }
    open("");
    notify("등록했습니다.");
  }
  const list = (items: Lesson[]) =>
    items.length ? (
      items.map((l) => (
        <div className="list-row" key={l.id}>
          <div className="time">{l.time}</div>
          <div className="grow">
            <strong>{l.name}</strong>
            <span>
              {l.model} · {l.member_ids.length}/{l.capacity}명 ·{" "}
              {l.member_ids
                .map((id) => members.find((m) => m.id === id)?.name)
                .join(", ")}
            </span>
          </div>
          <a className="text-link" href="/reservations">
            예약 보기
          </a>
          <button
            className="button small"
            disabled={l.completed}
            onClick={() => finish(l)}
          >
            {l.completed ? "완료됨" : "수업 완료"}
          </button>
        </div>
      ))
    ) : (
      <EmptyState>등록된 수업이 없습니다.</EmptyState>
    );
  const titles: Record<string, string> = {
    dashboard: "오늘의 운영",
    members: "회원",
    passes: "이용권",
    schedule: "수업 일정",
    attendance: "출석",
    classes: "클래스",
    reservations: "예약",
    payments: "결제 / 재등록",
  };
  return (
    <AppShell>
      <div className="workspace-top">
        <span>{c.name}</span>
        <span>데모 워크스페이스 · 2026년 9월 9일</span>
      </div>
      <PageHeader
        title={titles[page]}
        description={
          page === "dashboard"
            ? "오늘도 수업에 집중할 수 있도록, 운영을 한눈에."
            : "회원과 수업의 흐름을 간편하게 관리하세요."
        }
        action={
          ["members", "passes", "schedule", "classes"].includes(page) ? (
            <button
              className="button primary"
              onClick={() =>
                open(
                  page === "members"
                    ? "member"
                    : page === "passes"
                      ? "pass"
                      : "lesson",
                )
              }
            >
              +{" "}
              {page === "members"
                ? "회원 등록"
                : page === "passes"
                  ? "이용권 만들기"
                  : "수업 만들기"}
            </button>
          ) : undefined
        }
      />
      {notice && (
        <div role="status" className="toast" onClick={() => notify("")}>
          {notice}{" "}
          <button aria-label="알림 닫기" className="icon-button">
            ×
          </button>
        </div>
      )}
      {page === "dashboard" && (
        <>
          <StatStrip
            items={[
              {
                label: "오늘 수업",
                value: String(dayLessons.length),
                note: "오늘 예정된 일정",
              },
              {
                label: "예약 회원",
                value: String(
                  dayLessons.reduce((n, l) => n + l.member_ids.length, 0),
                ),
                note: "오늘 예약 건수",
              },
              {
                label: "결제 확인 필요",
                value: String(members.filter((m) => !m.paid).length),
                note: "입금 확인을 기다려요",
              },
              {
                label: "이번 달 출석",
                value: String(
                  data.attendance.filter((a) =>
                    lessons
                      .find((l) => l.id === a.lesson_id)
                      ?.date.startsWith("2026-09"),
                  ).length,
                ),
                note: "처리한 출석 건수",
              },
            ]}
          />
          <div className="grid two">
            <section className="panel">
              <div className="panel-head">
                <h2>오늘 누가 오나요?</h2>
                <a className="text-link" href="/schedule">
                  전체 일정 ↗
                </a>
              </div>
              {list(dayLessons)}
            </section>
            <section className="panel">
              <div className="panel-head">
                <h2>놓치지 말아야 할 일</h2>
                <span className="status">운영 알림</span>
              </div>
              {c.notifications ? (
                members.filter(renew).map((m) => (
                  <a href="/payments" className="notice" key={m.id}>
                    <div className="dot warning" />
                    <div>
                      <strong>
                        {m.name} · {!m.paid ? "결제 확인 필요" : "재등록 확인"}
                      </strong>
                      <span>
                        잔여 {remaining(m) ?? "무제한"}회 · 만료 {m.expires}
                      </span>
                    </div>
                    <span>↗</span>
                  </a>
                ))
              ) : (
                <EmptyState>재등록 알림이 꺼져 있습니다.</EmptyState>
              )}
              <a className="notice" href="/attendance">
                <div className="dot" />
                <div>
                  <strong>출석을 확인해주세요</strong>
                  <span>예정된 수업의 출석을 처리할 수 있어요.</span>
                </div>
              </a>
            </section>
          </div>
          <div className="hint-card">
            <strong>우리 스튜디오의 운영 방식</strong>
            <p>
              {c.models.join(" · ")} /{" "}
              {c.reservation ? "예약 사용" : "예약 없이 이용"} /{" "}
              {c.attendanceBy} 출석 / 기본 {c.deduction} 차감{" "}
              <a href="/settings"> · 설정 변경 →</a>
            </p>
          </div>
        </>
      )}
      {page === "members" && (
        <>
          <div className="toolbar">
            <input
              aria-label="회원 검색"
              className="input search"
              placeholder="이름 또는 전화번호 검색"
              value={query}
              onChange={(e) => search(e.target.value)}
            />
            <div className="segmented">
              {["전체", "결제 필요", "이용 중"].map((x) => (
                <button
                  key={x}
                  className={filter === x ? "selected" : ""}
                  onClick={() => setFilter(x)}
                >
                  {x}
                </button>
              ))}
            </div>
          </div>
          <section className="panel table-panel">
            <div className="table-head">
              <span>회원</span>
              <span>이용권 / 상태</span>
              <span>잔여</span>
              <span>다음 수업</span>
              <span />
            </div>
            {members
              .filter(
                (m) =>
                  (m.name + m.phone).includes(query) &&
                  (filter === "전체" ||
                    (filter === "결제 필요" ? !m.paid : m.paid)),
              )
              .map((m) => (
                <div className="table-row" key={m.id}>
                  <div>
                    <strong>{m.name}</strong>
                    <span>{m.phone}</span>
                  </div>
                  <div>
                    {passes.find((p) => p.id === m.pass_id)?.name ||
                      "이용권 없음"}
                    <span className={!m.paid ? "warn-text" : ""}>
                      {m.paid ? "이용 중" : "결제 확인 필요"}
                    </span>
                  </div>
                  <strong>
                    {remaining(m) ?? "무제한"}
                    {remaining(m) !== null ? "회" : ""}
                  </strong>
                  <span>
                    {available
                      .filter(
                        (l) => l.member_ids.includes(m.id) && !l.completed,
                      )
                      .sort((a, b) =>
                        (a.date + a.time).localeCompare(b.date + b.time),
                      )[0]?.date || "예정 없음"}
                  </span>
                  <button
                    aria-label={`${m.name} 상세 보기`}
                    className="icon-button"
                    onClick={() => show(m)}
                  >
                    ›
                  </button>
                </div>
              ))}
            {!members.some(
              (m) =>
                (m.name + m.phone).includes(query) &&
                (filter === "전체" ||
                  (filter === "결제 필요" ? !m.paid : m.paid)),
            ) && <EmptyState>검색 결과가 없습니다.</EmptyState>}
          </section>
        </>
      )}
      {page === "passes" && (
        <section className="panel table-panel">
          <div className="table-head passes">
            <span>이용권 / 이용 회원</span>
            <span>유형</span>
            <span>횟수</span>
            <span>유효기간</span>
            <span>차감 / 시작 기준</span>
          </div>
          {passes.map((p) => (
            <div className="table-row passes" key={p.id}>
              <div>
                <strong>{p.name}</strong>
                <span>
                  {members.filter((m) => m.pass_id === p.id).length}명 ·{" "}
                  {p.price.toLocaleString()}원
                </span>
              </div>
              <span>{p.type}</span>
              <span>{p.sessions || "무제한"}</span>
              <span>{p.days}일</span>
              <div>
                {p.sessions ? p.deduction : "차감 없음"}
                <span>{p.start}부터</span>
              </div>
            </div>
          ))}
        </section>
      )}
      {page === "schedule" && (
        <>
          <section className="panel calendar-panel">
            <div className="calendar-top">
              <button
                aria-label="이전 달"
                onClick={() => {
                  const d = new Date(month + "-15");
                  d.setMonth(d.getMonth() - 1);
                  setMonth(d.toISOString().slice(0, 7));
                }}
              >
                ‹
              </button>
              <h2>{month.replace("-", "년 ")}월</h2>
              <button
                aria-label="다음 달"
                onClick={() => {
                  const d = new Date(month + "-15");
                  d.setMonth(d.getMonth() + 1);
                  setMonth(d.toISOString().slice(0, 7));
                }}
              >
                ›
              </button>
              <button
                className="text-link"
                style={{ width: 50, fontSize: 12 }}
                onClick={() => {
                  setMonth("2026-09");
                  setDate("2026-09-09");
                }}
              >
                오늘
              </button>
            </div>
            <div className="weekdays">
              {["일", "월", "화", "수", "목", "금", "토"].map((x) => (
                <span key={x}>{x}</span>
              ))}
            </div>
            <div className="calendar-grid">
              {Array.from(
                { length: new Date(month + "-01").getDay() },
                (_, i) => (
                  <div className="day empty" key={"e" + i} />
                ),
              )}
              {Array.from(
                {
                  length: new Date(
                    Number(month.slice(0, 4)),
                    Number(month.slice(5)),
                    0,
                  ).getDate(),
                },
                (_, i) => {
                  const key = `${month}-${String(i + 1).padStart(2, "0")}`;
                  const ls = available.filter((l) => l.date === key);
                  return (
                    <button
                      className={`day ${date === key ? "selected" : ""}`}
                      key={key}
                      onClick={() => setDate(key)}
                    >
                      <span className="day-num">{i + 1}</span>
                      {ls.length > 0 && (
                        <>
                          <span className="calendar-lesson">{ls[0].name}</span>
                          <span className="count">
                            예약{" "}
                            {ls.reduce((n, l) => n + l.member_ids.length, 0)}명
                            {ls.length > 1 ? ` · +${ls.length - 1}수업` : ""}
                          </span>
                        </>
                      )}
                    </button>
                  );
                },
              )}
            </div>
          </section>
          <section className="panel">
            <div className="panel-head">
              <h2>{date} 수업</h2>
              <span className="muted">{dayLessons.length}건</span>
            </div>
            {list(dayLessons)}
          </section>
        </>
      )}
      {page === "classes" && (
        <>
          <div className="toolbar">
            <span className="muted">
              사용 중인 수업 방식: {c.models.join(", ")}
            </span>
          </div>
          <section className="panel">{list(available)}</section>
        </>
      )}
      {page === "attendance" && (
        <>
          <div className="filters-row">
            <label className="config-field">
              <span>수업 날짜</span>
              <input
                className="input"
                type="date"
                value={date}
                onChange={(e) => {
                  setDate(e.target.value);
                  select([]);
                  setGroup("");
                }}
              />
            </label>
            <div className="segmented">
              {groups.map((l) => (
                <button
                  className={active?.id === l.id ? "selected" : ""}
                  key={l.id}
                  onClick={() => {
                    setGroup(l.id);
                    select([]);
                  }}
                >
                  {l.time} {l.name}
                </button>
              ))}
            </div>
          </div>
          {!c.models.includes("그룹 레슨") ? (
            <EmptyState>
              그룹 출석은 설정에서 그룹 레슨을 켜면 사용할 수 있어요.
            </EmptyState>
          ) : active ? (
            <section className="panel attendance-panel">
              <div className="panel-head">
                <h2>{active.name}</h2>
                <span className="muted">
                  {active.member_ids.length}명 · {c.attendanceBy}
                </span>
              </div>
              <label className="select-all">
                <input
                  aria-label="전체 선택"
                  type="checkbox"
                  checked={
                    selected.length > 0 &&
                    selected.length ===
                      active.member_ids.filter(
                        (id) =>
                          !data.attendance.some(
                            (a) =>
                              a.lesson_id === active.id && a.member_id === id,
                          ),
                      ).length
                  }
                  onChange={(e) =>
                    select(
                      e.target.checked
                        ? active.member_ids.filter(
                            (id) =>
                              !data.attendance.some(
                                (a) =>
                                  a.lesson_id === active.id &&
                                  a.member_id === id,
                              ),
                          )
                        : [],
                    )
                  }
                />
                전체 선택
              </label>
              {active.member_ids.map((id) => {
                const m = members.find((m) => m.id === id)!;
                const done = data.attendance.some(
                  (a) => a.lesson_id === active.id && a.member_id === id,
                );
                return (
                  <label className="attendance-row" key={id}>
                    <input
                      type="checkbox"
                      disabled={done}
                      checked={done || selected.includes(id)}
                      onChange={() =>
                        select(
                          selected.includes(id)
                            ? selected.filter((x) => x !== id)
                            : [...selected, id],
                        )
                      }
                    />
                    <div className="grow">
                      <strong>{m.name}</strong>
                      <span>{m.phone}</span>
                    </div>
                    <span>
                      {done
                        ? "✓ 출석 완료"
                        : `잔여 ${remaining(m) ?? "무제한"}회`}
                    </span>
                  </label>
                );
              })}
              <div className="action-footer">
                <span>{selected.length}명 선택</span>
                <button
                  className="button primary"
                  disabled={!selected.length}
                  onClick={() => {
                    setData((d) => attend(d, active.id, selected));
                    select([]);
                    notify(
                      "출석을 처리했습니다. 이용권 정책에 따라 회차를 반영했습니다.",
                    );
                  }}
                >
                  출석 처리
                </button>
              </div>
            </section>
          ) : (
            <EmptyState>이 날짜에 그룹 수업이 없습니다.</EmptyState>
          )}
        </>
      )}
      {page === "reservations" && (
        <>
          {!c.reservation ? (
            <EmptyState>
              예약 기능이 꺼져 있습니다. 설정에서 변경할 수 있어요.
            </EmptyState>
          ) : (
            <>
              <p className="muted">
                데모 기준 9/9 09:00 · 예약 {c.opens}일 전 오픈 · {c.closes}시간
                전 마감 · 취소 {c.cancellation}시간 전까지
              </p>
              <section className="panel">
                {available
                  .filter((l) => !l.completed)
                  .map((l) => (
                    <div key={l.id} className="reservation-block">
                      <div className="panel-head">
                        <div>
                          <h2>{l.name}</h2>
                          <p>
                            {l.date} {l.time} · {l.member_ids.length}/
                            {l.capacity}명
                          </p>
                        </div>
                      </div>
                      {l.member_ids.map((id) => (
                        <div className="list-row" key={id}>
                          <strong className="grow">
                            {members.find((m) => m.id === id)?.name}
                          </strong>
                          <button
                            className="button small"
                            onClick={() => {
                              setData((d) => ({
                                ...d,
                                lessons: d.lessons.map((x) =>
                                  x.id === l.id
                                    ? {
                                        ...x,
                                        member_ids: x.member_ids.filter(
                                          (m) => m !== id,
                                        ),
                                      }
                                    : x,
                                ),
                                members: d.members.map((m) => {
                                  const p = d.passes.find(
                                    (p) => p.id === m.pass_id,
                                  );
                                  const late = hoursUntil(l) < c.cancellation;
                                  const refund =
                                    p?.deduction === "예약 시" &&
                                    (!late || c.late === "회차 유지");
                                  const charge =
                                    late &&
                                    c.late === "회차 차감" &&
                                    p?.deduction !== "예약 시" &&
                                    p &&
                                    p.sessions > m.used;
                                  return m.id === id
                                    ? {
                                        ...m,
                                        used: refund
                                          ? Math.max(0, m.used - 1)
                                          : charge
                                            ? m.used + 1
                                            : m.used,
                                      }
                                    : m;
                                }),
                              }));
                              notify("예약을 취소했습니다.");
                            }}
                          >
                            예약 취소
                          </button>
                        </div>
                      ))}
                      <form
                        className="list-row"
                        onSubmit={(e) => {
                          e.preventDefault();
                          const id = String(
                            new FormData(e.currentTarget).get("member"),
                          );
                          const m = members.find((m) => m.id === id);
                          if (
                            !m ||
                            l.member_ids.includes(id) ||
                            l.member_ids.length >= l.capacity
                          )
                            return;
                          if (
                            hoursUntil(l) < c.closes ||
                            hoursUntil(l) > c.opens * 24
                          ) {
                            notify(
                              "예약 가능한 시간이 아닙니다. 데모 기준 시각은 9월 9일 오전 9시입니다.",
                            );
                            return;
                          }
                          if (remaining(m) === 0) {
                            notify("남은 회차가 없습니다.");
                            return;
                          }
                          setData((d) => ({
                            ...d,
                            lessons: d.lessons.map((x) =>
                              x.id === l.id
                                ? { ...x, member_ids: [...x.member_ids, id] }
                                : x,
                            ),
                            members: d.members.map((m) => {
                              const p = d.passes.find(
                                (p) => p.id === m.pass_id,
                              );
                              return m.id === id &&
                                p?.deduction === "예약 시" &&
                                p.sessions
                                ? { ...m, used: m.used + 1 }
                                : m;
                            }),
                          }));
                          notify("예약했습니다.");
                        }}
                      >
                        <select
                          aria-label="예약 회원"
                          className="input grow"
                          name="member"
                        >
                          {members
                            .filter((m) => !l.member_ids.includes(m.id))
                            .map((m) => (
                              <option key={m.id} value={m.id}>
                                {m.name}
                              </option>
                            ))}
                        </select>
                        <button
                          className="button small"
                          disabled={
                            l.member_ids.length >= l.capacity ||
                            l.member_ids.length === members.length
                          }
                        >
                          {l.member_ids.length >= l.capacity
                            ? "정원 마감"
                            : "예약 추가"}
                        </button>
                      </form>
                    </div>
                  ))}
              </section>
            </>
          )}
        </>
      )}
      {page === "payments" && (
        <>
          <StatStrip
            items={[
              {
                label: "확인 필요",
                value: String(members.filter((m) => !m.paid).length),
                note: "미확인 결제",
              },
              {
                label: "재등록 대상",
                value: String(members.filter(renew).length),
                note: `잔여 ${c.remaining}회 / 만료 ${c.expiry}일`,
              },
              {
                label: "수납 기록",
                value: String(data.payments.length),
                note: "데모에서 확인한 결제",
              },
              {
                label: "수납 합계",
                value: data.payments
                  .reduce((n, p) => n + p.amount, 0)
                  .toLocaleString(),
                note: "원",
              },
            ]}
          />
          <section className="panel">
            {members.filter(renew).map((m) => (
              <div className="list-row" key={m.id}>
                <div className="grow">
                  <strong>
                    {m.name} · {passes.find((p) => p.id === m.pass_id)?.name}
                  </strong>
                  <span>
                    잔여 {remaining(m) ?? "무제한"}회 · 만료 {m.expires}
                  </span>
                </div>
                <span className="status">
                  {m.paid ? "재등록 검토" : "결제 확인 필요"}
                </span>
                {m.paid && (
                  <button
                    className="button small"
                    onClick={() => {
                      const p = passes.find((p) => p.id === m.pass_id);
                      if (!p) return;
                      const expires = new Date("2026-09-09T12:00:00");
                      expires.setDate(expires.getDate() + p.days);
                      setData((d) => ({
                        ...d,
                        renewals: [
                          ...d.renewals,
                          {
                            ...owned(),
                            member_id: m.id,
                            pass_id: p.id,
                            start: m.first,
                            end: m.expires,
                            used: m.used,
                            total: p.sessions,
                          },
                        ],
                        members: d.members.map((x) =>
                          x.id === m.id
                            ? {
                                ...x,
                                used: 0,
                                paid: false,
                                first: "2026-09-09",
                                expires: expires.toISOString().slice(0, 10),
                              }
                            : x,
                        ),
                      }));
                      notify(
                        "이전 이용 주기를 보관하고 새 주기를 시작했습니다. 결제를 확인해주세요.",
                      );
                    }}
                  >
                    재등록 시작
                  </button>
                )}
                {!m.paid && (
                  <button
                    className="button small"
                    onClick={() => {
                      setData((d) => ({
                        ...d,
                        members: d.members.map((x) =>
                          x.id === m.id ? { ...x, paid: true } : x,
                        ),
                        payments: [
                          ...d.payments,
                          {
                            ...owned(),
                            member_id: m.id,
                            date: "2026-09-09",
                            amount:
                              passes.find((p) => p.id === m.pass_id)?.price ||
                              0,
                          },
                        ],
                      }));
                      notify("결제 확인을 기록했습니다.");
                    }}
                  >
                    입금 확인
                  </button>
                )}
              </div>
            ))}
            {!members.some(renew) && (
              <EmptyState>확인이 필요한 회원이 없습니다.</EmptyState>
            )}
          </section>
          <h3>최근 결제 내역</h3>
          <section className="panel">
            {data.payments.map((p) => (
              <div className="list-row" key={p.id}>
                <strong className="grow">
                  {members.find((m) => m.id === p.member_id)?.name}
                </strong>
                <span>{p.date}</span>
                <strong>{p.amount.toLocaleString()}원</strong>
              </div>
            ))}
            {!data.payments.length && (
              <EmptyState>아직 기록된 결제가 없습니다.</EmptyState>
            )}
          </section>
        </>
      )}
      {detail && (
        <div className="modal-backdrop" onClick={() => show(null)}>
          <section
            role="dialog"
            aria-modal="true"
            aria-label="회원 상세"
            className="modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="panel-head">
              <div>
                <h2>{detail.name}</h2>
                <p>
                  {detail.phone} · {detail.paid ? "이용 중" : "결제 확인 필요"}
                </p>
              </div>
              <button
                className="icon-button"
                aria-label="닫기"
                onClick={() => show(null)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
              <StatStrip
                items={[
                  {
                    label: "잔여",
                    value: String(remaining(detail) ?? "무제한"),
                  },
                  { label: "사용", value: String(detail.used) },
                  {
                    label: "전체",
                    value: String(
                      passes.find((p) => p.id === detail.pass_id)?.sessions ||
                        "무제한",
                    ),
                  },
                ]}
              />
              <p className="muted">
                첫 수업 {detail.first} · 만료 {detail.expires}
              </p>
              <h3>예정된 수업</h3>
              {list(
                available.filter(
                  (l) => l.member_ids.includes(detail.id) && !l.completed,
                ),
              )}
              <h3>최근 출석</h3>
              {data.attendance
                .filter((a) => a.member_id === detail.id)
                .map((a) => (
                  <p key={a.id} className="muted">
                    {lessons.find((l) => l.id === a.lesson_id)?.date} ·{" "}
                    {lessons.find((l) => l.id === a.lesson_id)?.name} · 출석
                  </p>
                ))}
              {!data.attendance.some((a) => a.member_id === detail.id) && (
                <EmptyState>출석 기록이 없습니다.</EmptyState>
              )}
              <h3>결제 / 등록 주기</h3>
              {data.renewals
                .filter((r) => r.member_id === detail.id)
                .map((r) => (
                  <p className="muted" key={r.id}>
                    이전 주기 · {r.start} ~ {r.end} · {r.used}/
                    {r.total || "무제한"}회 사용
                  </p>
                ))}
              <p className="muted">
                현재 이용권 · {detail.first} ~ {detail.expires}
              </p>
              {data.payments
                .filter((p) => p.member_id === detail.id)
                .map((p) => (
                  <p key={p.id}>
                    {p.date} · {p.amount.toLocaleString()}원
                  </p>
                ))}
            </div>
          </section>
        </div>
      )}
      {modal && (
        <div className="modal-backdrop">
          <section
            role="dialog"
            aria-modal="true"
            aria-label="새 항목 등록"
            className="modal"
          >
            <div className="panel-head">
              <h2>
                {modal === "pass"
                  ? "이용권 만들기"
                  : modal === "member"
                    ? "회원 등록"
                    : "수업 만들기"}
              </h2>
              <button
                className="icon-button"
                aria-label="닫기"
                onClick={() => open("")}
              >
                ×
              </button>
            </div>
            <form className="modal-body" onSubmit={save}>
              <div className="form-grid">
                <Field
                  name="name"
                  label={modal === "member" ? "회원 이름" : "이름"}
                />
                {modal === "pass" && (
                  <>
                    <Select
                      name="type"
                      label="유형"
                      options={c.passTypes.map((x) => [x])}
                    />
                    <Field
                      name="sessions"
                      type="number"
                      label="횟수 (기간권·월정액은 무제한)"
                      value={10}
                    />
                    <Field
                      name="days"
                      type="number"
                      label="유효기간 (일)"
                      value={60}
                    />
                    <Select
                      name="start"
                      label="유효기간 시작"
                      options={["결제일", "첫 수업일"].map((x) => [x])}
                    />
                    <Select
                      name="deduction"
                      label="회차 차감"
                      value={c.deduction}
                      options={["예약 시", "출석 시", "수업 완료 시"].map(
                        (x) => [x],
                      )}
                    />
                    <label className="config-field">
                      <span>가격 (선택)</span>
                      <input
                        className="input"
                        name="price"
                        type="number"
                        min={0}
                      />
                    </label>
                  </>
                )}
                {modal === "member" && (
                  <>
                    <Field name="phone" type="tel" label="전화번호" />
                    <Select
                      name="pass_id"
                      label="이용권"
                      options={passes.map((p) => [p.id, p.name])}
                    />
                  </>
                )}
                {modal === "lesson" && (
                  <>
                    <Select
                      name="model"
                      label="수업 방식"
                      options={c.models.map((x) => [x])}
                    />
                    <Field name="date" type="date" label="날짜" value={date} />
                    <Field name="time" type="time" label="시간" value="10:00" />
                    <Field
                      name="capacity"
                      type="number"
                      label="정원"
                      value={1}
                    />
                  </>
                )}
              </div>
              <div className="onboarding-actions">
                <button
                  type="button"
                  className="button"
                  onClick={() => open("")}
                >
                  취소
                </button>
                <button className="button primary">등록하기</button>
              </div>
            </form>
          </section>
        </div>
      )}
    </AppShell>
  );
}

export function Operations({ page }: { page: string }) {
  const { data, ready } = useStudio();
  if (!ready)
    return <div className="empty-state">사업장 설정을 불러오는 중입니다.</div>;
  const c = data.config;
  if (
    c.appointmentModule &&
    ["dashboard", "appointments", "treatments", "revisits"].includes(page)
  )
    return <ServiceOperations page={page} />;
  if (!c.lessonModule && ["members", "payments"].includes(page))
    return <ServiceOperations page={page} />;
  if (
    (!c.lessonModule &&
      ["schedule", "classes", "attendance", "passes", "reservations"].includes(
        page,
      )) ||
    (!c.attendanceEnabled && page === "attendance") ||
    (!c.appointmentModule &&
      ["appointments", "treatments", "revisits"].includes(page))
  )
    return (
      <AppShell>
        <div className="empty-state">
          이 기능은 꺼져 있습니다. <a href="/settings">설정에서 변경하기 →</a>
        </div>
      </AppShell>
    );
  return <LessonOperations page={page} />;
}
