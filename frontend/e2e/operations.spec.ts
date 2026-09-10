import {expect,test,Page} from "@playwright/test";
import {mailLink} from "./mail";
async function setup(page:Page,category:string,suffix:string){
  async function post(path:string,data:unknown){const csrf=await (await page.request.get('/api/v1/auth/csrf')).json();const response=await page.request.post('/api/v1'+path,{data,headers:{[csrf.headerName]:csrf.token}});expect(response.ok(),path).toBe(true);return response.status()===204?null:response.json();}
  const email=`ops-${suffix}@example.test`,password="Operational safe password!";
  await post('/auth/signup',{name:"운영자",email,password});const link=new URL(await mailLink(email,"VERIFY"));
  await post('/auth/email-verification/confirm',{token:new URLSearchParams(link.hash.slice(1)).get('token')});await post('/auth/login',{email,password});
  const studio=await post('/studios',{name:"운영 테스트",slug:`ops-${suffix}`,timezone:"Asia/Seoul"});
  await post(`/studios/${studio.id}/onboarding/complete`,{version:0,businessCategory:category,businessType:category==="LESSON"?"DANCE":"NAIL",
    capabilities:category==="LESSON"?{PRIVATE_LESSON:true,GROUP_CLASS:false,ATTENDANCE:false,PASS_MANAGEMENT:true,CUSTOMER_BOOKING:false}:{CUSTOMER_BOOKING:false,DEPOSIT:false,REVISIT:false},
    businessHours:Array.from({length:7},(_,i)=>({weekday:i+1,closed:false,openTime:"08:00",closeTime:"22:00"})),bookingPolicy:{slotIntervalMinutes:30,bookingWindowDays:30,cancellationCutoffHours:12},
    lessonPolicy:category==="LESSON"?{lowBalanceThreshold:2,expiryAlertDays:7,restoreOnTimelyCancellation:true}:null,beautyPolicy:category==="BEAUTY"?{depositEnabled:false,noShowEnabled:true}:null});
}
for(const category of ["LESSON","BEAUTY"]){
  test(`${category} customer payment refund booking conflict and block flow`,async({page},info)=>{
    test.setTimeout(90000);
    await setup(page,category,`${category.toLowerCase()}-${info.project.name}-${Date.now()}`);
    const label=category==="LESSON"?"회원":"고객";
    await page.goto('/app/customers');await page.getByRole('link',{name:label+' 등록',exact:true}).click();
    await page.getByLabel('이름',{exact:true}).fill('운영 고객');await page.getByLabel('전화번호',{exact:true}).fill('010-1234-5678');await page.getByLabel('운영자 메모',{exact:true}).fill('내부 메모');
    await page.getByRole('button',{name:label+' 저장',exact:true}).click();await expect(page).toHaveURL(/\/app\/customers\/[0-9a-f-]+$/);const customerUrl=page.url();
    await page.getByLabel('이름',{exact:true}).fill('수정 고객');await page.getByRole('button',{name:label+' 저장',exact:true}).click();await expect(page.getByText('저장했습니다.',{exact:true})).toBeVisible();
    await page.getByRole('link',{name:'결제',exact:true}).click();await page.getByRole('link',{name:'결제 기록',exact:true}).click();
    await expect(page.getByRole('option',{name:/수정 고객/})).toHaveCount(1);await page.getByLabel(label+' 선택',{exact:true}).selectOption({label:'수정 고객 · 010-1234-5678'});
    await page.getByLabel('금액 (원)',{exact:true}).fill('45000');await page.getByRole('button',{name:'결제 기록 저장',exact:true}).click();
    await expect(page.getByText('수납 완료',{exact:true})).toBeVisible();await page.getByLabel('환불 사유',{exact:true}).fill('요청에 따른 전액 환불');await page.getByRole('button',{name:'전액 환불 기록',exact:true}).click();
    await expect(page.getByText('환불 완료',{exact:true})).toBeVisible();await page.reload();await expect(page.getByText('환불 이력',{exact:true})).toBeVisible();
    await page.getByRole('link',{name:'예약',exact:true}).click();
    const tomorrow=new Date(Date.now()+2*86400000).toISOString().slice(0,10);await page.getByLabel('예약 날짜',{exact:true}).fill(tomorrow);
    async function book(start:string,end:string){await page.getByRole('button',{name:'예약 만들기',exact:true}).click();await expect(page.getByRole('option',{name:/수정 고객/})).toHaveCount(1);
      await page.getByLabel(label+' 선택',{exact:true}).selectOption({label:'수정 고객 · 010-1234-5678'});await page.getByLabel('시작 시간',{exact:true}).fill(start);await page.getByLabel('종료 시간',{exact:true}).fill(end);await page.getByRole('button',{name:'예약 저장',exact:true}).click();}
    await book('10:00','11:00');await expect(page.locator('.booking-item').filter({hasText:'수정 고객'}).getByText('확정',{exact:true})).toBeVisible();
    await book('10:00','11:00');await expect(page.getByText('담당자의 기존 예약과 시간이 겹칩니다.',{exact:true})).toBeVisible();await page.getByRole('button',{name:'닫기',exact:true}).click();
    await page.getByRole('button',{name:'예약 취소',exact:true}).click();await expect(page.locator('.booking-item').getByText('취소',{exact:true})).toBeVisible();
    await page.getByRole('button',{name:'시간 차단',exact:true}).click();await page.getByLabel('시작 시간',{exact:true}).fill('12:00');await page.getByLabel('종료 시간',{exact:true}).fill('13:00');await page.getByLabel('차단 사유',{exact:true}).fill('휴게 시간');await page.getByRole('button',{name:'차단 저장',exact:true}).click();
    await expect(page.getByText('휴게 시간',{exact:true})).toBeVisible();await book('12:00','13:00');await expect(page.getByText('예약이 차단된 시간입니다.',{exact:true})).toBeVisible();await page.getByRole('button',{name:'닫기',exact:true}).click();
    await page.getByRole('button',{name:'차단 해제',exact:true}).click();await expect(page.getByText('차단이 없습니다.',{exact:true})).toBeVisible();
    await book('12:00','13:00');await page.getByRole('button',{name:'변경',exact:true}).click();
    await page.getByLabel('시작 시간',{exact:true}).fill('13:00');await page.getByLabel('종료 시간',{exact:true}).fill('14:00');await page.getByRole('button',{name:'예약 저장',exact:true}).click();
    await expect(page.getByText('13:00–14:00 · 운영자',{exact:true})).toBeVisible();
    await page.getByRole('button',{name:'완료 처리',exact:true}).click();await expect(page.locator('.booking-item').getByText('완료',{exact:true})).toBeVisible();
    await book('14:00','15:00');await page.getByRole('button',{name:'노쇼 처리',exact:true}).click();await expect(page.locator('.booking-item').getByText('노쇼',{exact:true})).toBeVisible();
    await page.goto(customerUrl);await page.getByRole('button',{name:label+' 보관',exact:true}).click();await expect(page.getByText('보관했습니다. 기존 이력은 유지됩니다.',{exact:true})).toBeVisible();
    await page.getByRole('link',{name:label+' 목록',exact:true}).click();await page.getByLabel('이름·전화번호 검색',{exact:true}).fill('01012345678');await page.getByRole('button',{name:'검색',exact:true}).click();await expect(page.getByText('검색 결과가 없습니다.',{exact:true})).toBeVisible();
    await page.getByRole('combobox',{name:'상태',exact:true}).selectOption('ARCHIVED');await expect(page.getByRole('link',{name:'수정 고객',exact:true})).toBeVisible();
    expect(await page.evaluate(()=>localStorage.length)).toBe(0);expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  });
}
