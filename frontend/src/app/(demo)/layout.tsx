import Link from "next/link";
import { StudioProvider } from "@/demo/lib/store";

export default function DemoLayout({ children }: { children: React.ReactNode }) {
  if (process.env.NEXT_PUBLIC_ENABLE_DEMO !== "true") {
    return <main className="entry-page"><section className="entry-card">
      <h1>서비스를 준비하고 있어요.</h1>
      <p>계정과 운영 기능은 아직 제공되지 않습니다.</p>
      <Link className="button" href="/">서비스 소개로 돌아가기</Link>
    </section></main>;
  }
  return <StudioProvider><div role="note" className="demo-banner">
    UI 데모 · 실제 로그인·예약·결제가 아닙니다. 실제 고객 정보를 입력하지 마세요.
  </div>{children}</StudioProvider>;
}
