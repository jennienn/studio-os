"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Icon } from "@/components/icons";
import { useStudio } from "@/demo/lib/store";
export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { data } = useStudio();
  const c = data.config;
  const visible = [
    ["/dashboard", "홈", "home"],
    ...(c.appointmentModule && c.reservation
      ? [["/appointments", "예약 일정", "calendar"]]
      : []),
    ...(c.lessonModule
      ? [
          ["/schedule", "수업 일정", "calendar"],
          ...(c.attendanceEnabled ? [["/attendance", "출석", "check"]] : []),
        ]
      : []),
    ["/members", c.lessonModule ? "회원" : "고객", "users"],
    ...(c.lessonModule
      ? [
          ["/passes", "이용권", "ticket"],
          ["/classes", "클래스", "calendar"],
          ...(c.reservation
            ? [["/reservations", "수업 예약", "calendar"]]
            : []),
        ]
      : []),
    ...(c.appointmentModule && c.treatmentHistory
      ? [["/treatments", "시술 이력", "check"]]
      : []),
    ["/payments", c.lessonModule ? "결제 / 재등록" : "결제", "ticket"],
    ...(c.appointmentModule && c.revisitEnabled
      ? [["/revisits", "재방문", "home"]]
      : []),
    ["/settings", "설정", "settings"],
  ];
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">S</div>
          <div>
            <strong>Studio OS</strong>
            <span>사업장 운영 워크스페이스</span>
          </div>
        </div>

        <nav className="nav">
          {visible.map(([href, label, icon]) => {
            const active = pathname === href;
            return (
              <Link
                className={`nav-link${active ? " active" : ""}`}
                href={href}
                key={href}
              >
                <Icon name={icon} />
                <span>{label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="sidebar-bottom">
          <div className="studio-switcher">
            <span className="eyebrow">현재 사업장</span>
            <strong>{data.config.name}</strong>
            <span>
              {data.config.business} · {c.lessonModule ? "회원" : "고객"}{" "}
              {data.members.length}명
            </span>
            <a className="text-link" href="/">
              서비스 소개 ↗
            </a>
          </div>
        </div>
      </aside>

      <main className="main">{children}</main>
    </div>
  );
}
