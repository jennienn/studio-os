import { expect, test } from "@playwright/test";
import { mailLink } from "./mail";

for(const category of ["LESSON","BEAUTY"] as const) {
  test(`${category} persistent onboarding, settings and route boundaries`,async({page},info)=>{
    const suffix=`${category.toLowerCase()}-${info.project.name}-${Date.now()}`;
    const email=`${suffix}@example.test`,password="Configuration password!";
    await page.goto("/signup");
    await page.getByLabel("이름",{exact:true}).fill("설정 운영자");
    await page.getByLabel("이메일",{exact:true}).fill(email);
    await page.getByLabel("비밀번호",{exact:true}).fill(password);
    await page.getByRole("button",{name:"계정 만들기",exact:true}).click();
    await expect(page).toHaveURL(/verification-pending/);
    await page.goto(await mailLink(email,"VERIFY"));
    await page.getByRole("button",{name:"이메일 인증",exact:true}).click();
    await expect(page.getByRole("status")).toContainText("인증이 완료");
    await page.getByRole("link",{name:"로그인으로 돌아가기"}).click();
    await page.getByLabel("이메일",{exact:true}).fill(email);
    await page.getByLabel("비밀번호",{exact:true}).fill(password);
    await page.getByRole("button",{name:"로그인",exact:true}).click();
    await expect(page.getByRole("heading",{name:"내 사업장"})).toBeVisible();
    await page.getByLabel("사업장 이름",{exact:true}).fill(`${category} 테스트`);
    await page.getByLabel("사업장 주소",{exact:true}).fill(suffix);
    await page.getByLabel("시간대",{exact:true}).fill("Asia/Seoul");
    await page.getByRole("button",{name:"사업장 만들기",exact:true}).click();
    await expect(page).toHaveURL(/\/onboarding$/);
    await page.getByRole("button",{name:category==="LESSON"?"레슨 / 스튜디오":"뷰티 / 예약 서비스",exact:true}).click();
    await page.getByRole("button",{name:"다음",exact:true}).click();
    await expect(page.getByRole("button",{name:category==="LESSON"?"네일":"댄스",exact:true})).toHaveCount(0);
    if(category==="BEAUTY") await expect(page.getByRole("button",{name:"필라테스",exact:true})).toHaveCount(0);
    await page.getByRole("button",{name:category==="LESSON"?"댄스":"네일",exact:true}).click();
    await page.getByRole("button",{name:"다음",exact:true}).click();
    if(category==="LESSON") {
      await expect(page.getByRole("checkbox",{name:"예약금 사용",exact:true})).toHaveCount(0);
      await expect(page.getByRole("checkbox",{name:"이용권 관리",exact:true})).toBeChecked();
      await expect(page.getByRole("checkbox",{name:"이용권 관리",exact:true})).toBeDisabled();
      await page.getByRole("checkbox",{name:"개인 레슨",exact:true}).check();
    } else {
      await expect(page.getByRole("checkbox",{name:"그룹 출석 관리",exact:true})).toHaveCount(0);
      await page.getByRole("checkbox",{name:"예약금 사용",exact:true}).check();
    }
    await page.getByRole("checkbox",{name:"고객 직접 예약",exact:true}).check();
    await page.getByRole("button",{name:"다음",exact:true}).click();
    await page.getByRole("checkbox",{name:"월요일 휴무",exact:true}).uncheck();
    await page.getByLabel("월요일 시작",{exact:true}).fill("09:00");
    await page.getByLabel("월요일 종료",{exact:true}).fill("18:00");
    await page.getByRole("button",{name:"다음",exact:true}).click();
    await expect(page.getByLabel("예약 시간 간격 (분)",{exact:true})).toHaveValue("30");
    if(category==="LESSON") {
      await page.getByLabel("잔여 이용권 알림 기준 (회 이하)",{exact:true}).fill("3");
      await page.getByLabel("만료 알림 기준 (일 전)",{exact:true}).fill("7");
      await page.getByLabel("기한 내 취소 시 이용권 복구",{exact:true}).selectOption("true");
      await expect(page.getByLabel("노쇼 상태 관리",{exact:true})).toHaveCount(0);
    } else {
      await page.getByLabel("노쇼 상태 관리",{exact:true}).selectOption("true");
      await expect(page.getByLabel("기한 내 취소 시 이용권 복구",{exact:true})).toHaveCount(0);
    }
    await page.getByRole("button",{name:"다음",exact:true}).click();
    await expect(page.getByRole("heading",{name:"설정 확인",exact:true})).toBeVisible();
    if(category==="BEAUTY") await expect(page.locator(".configuration-summary")).not.toContainText(/회차|이용권|출석/);
    await page.getByRole("button",{name:"온보딩 완료",exact:true}).click();
    await expect(page).toHaveURL(/\/app$/);
    await expect(page.getByTestId("active-category")).toContainText("ACTIVE");
    await page.reload();
    await expect(page.getByTestId("active-category")).toContainText(category==="LESSON"?"레슨":"뷰티");
    await expect(page.getByRole("navigation",{name:"사업장 메뉴"}).getByRole("link")).toHaveCount(5);
    await page.getByRole("link",{name:"설정",exact:true}).click();
    await expect(page.getByRole("button",{name:category==="LESSON"?"뷰티 / 예약 서비스":"레슨 / 스튜디오",exact:true})).toHaveCount(0);
    await page.getByRole("button",{name:category==="LESSON"?"필라테스":"속눈썹",exact:true}).click();
    await page.getByLabel("예약 가능 기간 (일)",{exact:true}).fill("45");
    await page.getByRole("button",{name:"설정 저장",exact:true}).click();
    await expect(page.getByRole("status")).toContainText("설정을 저장했습니다");
    await page.reload();
    await expect(page.getByLabel("예약 가능 기간 (일)",{exact:true})).toHaveValue("45");
    await expect(page.getByRole("button",{name:category==="LESSON"?"필라테스":"속눈썹",exact:true})).toHaveAttribute("aria-pressed","true");
    await page.goto(category==="LESSON"?"/app/beauty/treatments":"/app/lesson/attendance");
    await expect(page).toHaveURL(/\/app$/);
    await page.goto("/onboarding");
    await expect(page).toHaveURL(/\/app$/);
    expect(await page.evaluate(()=>localStorage.length)).toBe(0);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  });
}
