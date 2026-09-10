"use client";
import Link from "next/link";
export function DemoEntry({ signup = false }: { signup?: boolean }) {
  return (
    <main className="entry-page">
      <Link className="wordmark" href="/">
        <span className="brand-mark">S</span>Studio OS
      </Link>
      <section className="entry-card">
        <span className="section-kicker">우리 사업장의 새로운 시작</span>
        <h1>
          {signup ? "고객에게 집중할 준비가 됐나요?" : "다시 만나 반가워요."}
        </h1>
        <p>
          지금은 가입 없이 체험할 수 있는 데모예요.
          <br />
          설정과 변경 사항은 이 브라우저에 저장됩니다.
        </p>
        <Link
          className="button primary"
          href={signup ? "/onboarding" : "/dashboard"}
        >
          {signup ? "우리 사업장 설정하기" : "데모 워크스페이스 입장"} →
        </Link>
        <Link className="button" href={signup ? "/dashboard" : "/onboarding"}>
          {signup ? "샘플 데이터로 바로 둘러보기" : "새 사업장 설정하기"}
        </Link>
        <small>실제 계정 생성과 결제는 진행되지 않습니다.</small>
      </section>
      <Link className="text-link" href="/">
        ← 서비스 소개로 돌아가기
      </Link>
    </main>
  );
}
