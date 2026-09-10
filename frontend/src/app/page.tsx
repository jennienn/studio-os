import Link from "next/link";
import { ProductPreview } from "@/components/ProductPreview";
import { Icon } from "@/components/icons";
const features = [
  [
    "users",
    "고객의 지금을, 한눈에.",
    "연락처부터 예약, 상담 메모까지. 고객마다 흩어져 있던 정보를 한곳에 모으세요.",
  ],
  [
    "ticket",
    "결제 확인을 놓치지 않게.",
    "이용권 결제부터 예약금까지. 사업장의 운영 방식에 맞춰 확인하세요.",
  ],
  [
    "calendar",
    "오늘 누가 오는지 바로.",
    "다가오는 예약과 담당자를 확인하고, 고객을 맞이할 시간을 준비하세요.",
  ],
  [
    "check",
    "방문 기록은 간편하게.",
    "수업 출석이나 시술 이력처럼, 다음 방문에 필요한 기록을 모으세요.",
  ],
  [
    "home",
    "다음 방문까지 자연스럽게.",
    "재등록 기준이나 재방문 주기에 맞춰, 다시 만날 고객을 확인하세요.",
  ],
  [
    "settings",
    "운영 방식은 우리답게.",
    "개인·그룹 레슨부터 1:1 시술까지. 필요한 기능과 예약 정책을 선택하세요.",
  ],
];
export default function Page() {
  return (
    <div className="landing">
      <header className="landing-nav">
        <Link href="/" className="wordmark">
          <span className="brand-mark">S</span>Studio OS
          <span className="beta">베타</span>
        </Link>
        <nav>
          <a href="#features">기능</a>
          <a href="#studios">대상 업종</a>
          <a href="#pricing">요금</a>
        </nav>
        <div className="nav-actions">
          <Link href="/login">로그인</Link>
          <Link href="/signup" className="button primary">
            무료로 시작하기 <span>↗</span>
          </Link>
        </div>
      </header>
      <main>
        <section className="hero">
          <div className="hero-badge">
            <span />
            예약 기반 소규모 사업장을 위한 맞춤형 운영 SaaS
          </div>
          <h1>
            고객에게 집중하세요.
            <br />
            <span>운영은 한 곳에서.</span>
          </h1>
          <p>
            예약부터 고객 관리, 결제, 재방문까지.
            <br />
            사업장의 운영 방식에 맞춰 필요한 기능만 구성하세요.
          </p>
          <p className="hero-support">
            댄스·필라테스·PT부터 네일·속눈썹·뷰티샵까지
            <br />내 사업장 방식에 맞게 가볍게 시작하세요.
          </p>
          <div className="hero-actions">
            <Link className="button primary" href="/signup">
              무료로 시작하기 <span>→</span>
            </Link>
            <Link className="button" href="/dashboard">
              데모 둘러보기 <span>↗</span>
            </Link>
          </div>
          <span className="hero-note">
            가입 없이 데모 체험 · 복잡한 설치 없이 시작
          </span>
          <ProductPreview />
        </section>
        <section className="industry-strip" id="studios">
          <p>이런 사업장의 운영을 함께 준비하고 있어요.</p>
          <div className="industry-groups">
            <div>
              <h3>레슨 · 스튜디오</h3>
              <p>댄스 · 필라테스 · 요가 · PT · 폴댄스 · 보컬 / 개인레슨</p>
            </div>
            <div>
              <h3>뷰티 · 예약 서비스</h3>
              <p>
                네일 · 속눈썹 · 붙임머리 · 왁싱 · 1인 미용 · 기타 예약형 서비스
              </p>
            </div>
          </div>
          <small>
            초기 지원을 준비 중인 대상 업종입니다. 세부 기능은 단계적으로
            제공됩니다.
          </small>
        </section>
        <section className="landing-section mode-section">
          <span className="section-kicker">
            하나의 제품, 우리만의 운영 방식
          </span>
          <h2>내 사업장에 맞게 달라지는 관리 화면</h2>
          <p className="section-description">
            똑같은 기능을 모든 사업장에 강요하지 않습니다.
            <br />
            처음 설정한 운영 방식에 따라 필요한 기능과 화면을 구성합니다.
          </p>
          <div className="mode-columns">
            <div>
              <h3>레슨 · 스튜디오</h3>
              <p>수업 일정 · 출석 · 회차권 · 그룹 수업 · 재등록</p>
            </div>
            <div>
              <h3>뷰티 · 예약샵</h3>
              <p>예약 일정 · 시술 이력 · 예약금 · 재방문 · 노쇼 관리</p>
            </div>
          </div>
          <p className="section-description">
            고객 · 예약 · 결제 · 알림 등 핵심 운영 데이터는
            <br />
            하나의 시스템에서 관리됩니다.
          </p>
        </section>
        <section className="landing-section" id="features">
          <span className="section-kicker">매일의 운영을 위한 기능</span>
          <h2>
            여러 도구를 오가던 일상,
            <br />
            이제 하나의 흐름으로.
          </h2>
          <p className="section-description">
            고객 · 예약 · 결제 · 담당자, 작은 사업장에 꼭 필요한 것부터.
          </p>
          <div className="feature-grid">
            {features.map(([icon, title, description], i) => (
              <article key={title}>
                <div className="feature-top">
                  <span className="feature-icon">
                    <Icon name={icon} />
                  </span>
                  <span>0{i + 1}</span>
                </div>
                <h3>{title}</h3>
                <p>{description}</p>
              </article>
            ))}
          </div>
        </section>
        <section className="configuration-section">
          <div>
            <span className="section-kicker">사업장에 맞추는 시작</span>
            <h2>우리 방식대로 운영할 수 있어요.</h2>
            <p>
              업종 하나로 운영 방식을 정하지 않아요.
              <br />
              어떤 서비스를 제공하고, 어떻게 예약을 받고,
              <br />
              어떤 기록을 남길지 직접 선택하세요.
            </p>
            <Link className="text-link" href="/onboarding">
              우리 사업장 설정하기 →
            </Link>
          </div>
          <div className="configuration-preview">
            <span className="eyebrow">나만의 운영 설정</span>
            <h3>어떤 기능이 필요한가요?</h3>
            <div className="config-demo-options">
              <span>✓ 예약 관리</span>
              <span>✓ 고객 기록</span>
              <span>알림</span>
            </div>
            <div className="config-demo-row">
              <span>예약 마감</span>
              <strong>방문 2시간 전</strong>
            </div>
            <div className="config-demo-row">
              <span>담당자 배정</span>
              <strong>예약별 지정</strong>
            </div>
            <div className="config-demo-row">
              <span>재방문 알림</span>
              <strong>운영 주기에 맞춰</strong>
            </div>
            <p>설정은 언제든 변경할 수 있어요.</p>
          </div>
        </section>
        <section className="landing-section pricing-section" id="pricing">
          <div>
            <span className="section-kicker">부담 없는 첫 시작</span>
            <h2>
              먼저, 우리 사업장에
              <br />
              맞는지 경험해보세요.
            </h2>
            <p className="section-description">
              작은 팀이 편하게 쓰는 운영 도구.
              <br />
              지금은 모든 데모 기능을 무료로 둘러볼 수 있어요.
            </p>
          </div>
          <div className="pricing-card">
            <span>프로토타입 체험</span>
            <h3>
              무료 <small>/ 데모</small>
            </h3>
            <p>
              ✓ 고객·예약·결제 관리
              <br />✓ 맞춤 온보딩과 운영 설정
              <br />✓ 레슨·뷰티 모듈 체험
            </p>
            <Link className="button primary" href="/dashboard">
              데모 시작하기 →
            </Link>
            <small>정식 서비스 요금은 출시 시 안내할 예정입니다.</small>
          </div>
        </section>
        <section className="final-cta">
          <span className="section-kicker">
            운영은 간결하게, 고객에게 더 가까이.
          </span>
          <h2>우리 사업장에 맞게 시작하기</h2>
          <Link className="button primary" href="/signup">
            무료로 시작하기 →
          </Link>
        </section>
      </main>
      <footer className="landing-footer">
        <Link href="/" className="wordmark">
          <span className="brand-mark">S</span>Studio OS
        </Link>
        <span>작은 사업장의 매일을 함께.</span>
        <small>© 2026 Studio OS · 데모 프로토타입</small>
      </footer>
    </div>
  );
}
