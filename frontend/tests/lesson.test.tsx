import {vi,it,expect,afterEach} from "vitest";
import {render,screen,fireEvent,waitFor} from "@testing-library/react";
import {api} from "@/auth/api";
import {ProductPanel} from "@/lesson/ProductPanel";
import {EnrollmentPanel} from "@/lesson/EnrollmentPanel";
import {CyclePicker} from "@/lesson/CyclePicker";
import {ClassPanel} from "@/lesson/ClassPanel";
import {AttendancePanel} from "@/lesson/AttendancePanel";
import {ConfigurationPanel} from "@/configuration/ConfigurationPanel";
const router=vi.hoisted(()=>({replace:vi.fn(),push:vi.fn()}));vi.mock("next/navigation",()=>({useRouter:()=>router}));
vi.mock("@/auth/api",async original=>({...await original<object>(),api:vi.fn()}));afterEach(()=>vi.clearAllMocks());
const product={id:"p",name:"Flexible",productType:"COUNT_BASED",totalCount:7,validityDays:30,validityStartRule:"FIRST_USE",price:"10000",deductionTrigger:"LESSON_COMPLETED",billingPeriod:null,active:true};
const cycle={id:"c",enrollmentId:"e",productName:"Historical",productType:"COUNT_BASED",purchasedCount:7,purchasePrice:"10000",deductionTrigger:"LESSON_COMPLETED",status:"ACTIVE",balance:6,reserved:1,available:5,paymentDate:"2030-01-01",startDate:null,validEndDate:null,paymentId:"pay"};
it("creates arbitrary counts and edits product without fixed presets",async()=>{
 vi.mocked(api).mockImplementation(async(path,body)=>body?{...product,...body as object}:{items:[product],totalPages:1});render(<ProductPanel studioId="s"/>);
 fireEvent.click(screen.getByRole("button",{name:"이용권 만들기"}));fireEvent.change(screen.getByLabelText("상품명"),{target:{value:"15 sessions"}});fireEvent.change(screen.getByLabelText("구매 회차"),{target:{value:"15"}});fireEvent.change(screen.getByLabelText("가격 (원)"),{target:{value:"12345"}});fireEvent.click(screen.getByRole("button",{name:"상품 저장"}));
 await waitFor(()=>expect(api).toHaveBeenCalledWith("/studios/s/lesson/pass-products",expect.objectContaining({totalCount:15,price:"12345"}),"POST"));
 fireEvent.click(await screen.findByRole("button",{name:"상품 수정"}));fireEvent.click(screen.getByLabelText("판매 활성"));fireEvent.click(screen.getByRole("button",{name:"상품 저장"}));await waitFor(()=>expect(api).toHaveBeenCalledWith("/studios/s/lesson/pass-products/p",expect.objectContaining({active:false}),"PUT"));
});
function enrollmentReads(){vi.mocked(api).mockImplementation(async(path,body)=>{
 if(body)return path.endsWith("renew")?cycle:{id:"e"};if(path.includes("pass-products"))return {items:[product],totalPages:1};if(path.endsWith("/cycles"))return [cycle,{...cycle,id:"old",status:"EXPIRED",productName:"Previous"}];return {items:[{id:"e",kind:"PRIVATE",status:"ACTIVE"}],totalPages:1};
});}
it("adds enrollment and records renewal through separate explicit payment confirmation",async()=>{
 enrollmentReads();render(<EnrollmentPanel studioId="s" customerId="customer" role="OWNER" groupEnabled={false}/>);
 fireEvent.click(screen.getByRole("button",{name:"수강 추가"}));await screen.findByText("Historical · 이용 중");
 expect(api).toHaveBeenCalledWith("/studios/s/lesson/enrollments",expect.objectContaining({customerId:"customer",kind:"PRIVATE"}),"POST",expect.any(String));
 fireEvent.change(screen.getByLabelText("구매할 이용권"),{target:{value:"p"}});fireEvent.click(screen.getByRole("button",{name:"수납 확인 · 이용 시작"}));await waitFor(()=>expect(api).toHaveBeenCalledWith("/studios/s/lesson/enrollments/e/renew",{passProductId:"p",method:"CASH"},"POST",expect.any(String)));
});
it("renders cycle snapshots and ledger-derived available balance",async()=>{
 enrollmentReads();render(<EnrollmentPanel studioId="s" customerId="customer" role="MANAGER" groupEnabled={false}/>);await screen.findByRole("option",{name:/개인 · 진행/});fireEvent.change(screen.getByLabelText("수강 이력"),{target:{value:"e"}});
 await screen.findByText("Previous · 만료");expect(screen.getAllByText("구매 7회 · 잔여 6회 · 예약 중 1회 · 예약 가능 5회")).toHaveLength(2);expect(screen.queryByLabelText("회차 조정")).not.toBeInTheDocument();
});
it("private pass picker excludes scheduled and wrong-kind enrollments",async()=>{
 vi.mocked(api).mockImplementation(async path=>path.includes("enrollments?")?{items:[{id:"e",kind:"PRIVATE",status:"ACTIVE"},{id:"g",kind:"GROUP",status:"ACTIVE"}],totalPages:1}:[cycle,{...cycle,id:"next",status:"SCHEDULED",productName:"Next"}]);render(<CyclePicker studioId="s" customerId="customer" kind="PRIVATE" value="" onChange={vi.fn()}/>);
 await screen.findByRole("option",{name:/Historical · 예약 가능 5회/});expect(screen.queryByRole("option",{name:/Next/})).not.toBeInTheDocument();expect(api).not.toHaveBeenCalledWith(expect.stringContaining("/g/cycles"));
});
it("creates a class and saves a recurring schedule",async()=>{
 vi.mocked(api).mockImplementation(async(path,body)=>body?{id:"class"}:[]);render(<ClassPanel studioId="s"/>);fireEvent.change(screen.getByLabelText("반 이름"),{target:{value:"Evening"}});fireEvent.change(screen.getByLabelText("정원"),{target:{value:"12"}});fireEvent.click(screen.getByRole("button",{name:"반 저장"}));await screen.findByRole("heading",{name:"반복 일정"});
 fireEvent.change(screen.getByLabelText("수업 시작"),{target:{value:"18:00"}});fireEvent.change(screen.getByLabelText("수업 시간 (분)"),{target:{value:"50"}});fireEvent.click(screen.getByRole("button",{name:"반복 일정 저장"}));await waitFor(()=>expect(api).toHaveBeenCalledWith("/studios/s/lesson/classes/class/schedules",expect.objectContaining({durationMinutes:50}),"POST",expect.any(String)));
});
it("bulk attendance submits selected members with explicit PRESENT",async()=>{
 vi.mocked(api).mockImplementation(async(path,body)=>{
 if(body)return {count:1};if(path.includes("/classes?"))return [{id:"class",name:"Group"}];if(path.includes("/attendance?"))return [{bookingId:"b",customerName:"Member",customerPhone:"010",bookingStatus:"CONFIRMED",attendanceStatus:null,remaining:7,available:6}];return [{id:"o",classId:"class",className:"Group",startAt:"2030-01-01T01:00:00Z",endAt:"2030-01-01T02:00:00Z",capacitySnapshot:10,bookedCount:1,status:"SCHEDULED"}];
 });render(<AttendancePanel studioId="s" timezone="Asia/Seoul" role="STAFF" attendanceEnabled/>);await screen.findByRole("option",{name:"Group"});fireEvent.change(screen.getByLabelText("반 선택"),{target:{value:"class"}});fireEvent.click(await screen.findByRole("button",{name:"회차 선택"}));await screen.findByText(/잔여 7회/);fireEvent.click(screen.getByLabelText("현재 페이지 전체 출석"));fireEvent.click(screen.getByRole("button",{name:"출석 일괄 저장"}));await waitFor(()=>expect(api).toHaveBeenCalledWith("/studios/s/lesson/occurrences/o/attendance",{entries:[{bookingId:"b",status:"PRESENT"}]},"PUT",expect.any(String)));
 expect(screen.queryByRole("button",{name:"그룹 예약 추가"})).not.toBeInTheDocument();
});
function config(category:string){return {studioId:"s",status:"ACTIVE",businessCategory:category,businessType:category==="LESSON"?"DANCE":"NAIL",configuration:{capabilities:{PRIVATE_LESSON:true,GROUP_CLASS:true,ATTENDANCE:true},businessHours:[],bookingPolicy:{},lessonPolicy:{},beautyPolicy:null},permissions:{editPolicies:true},catalog:{},bookingDefaults:{}};}
it("lesson navigation exposes products and attendance",async()=>{vi.mocked(api).mockResolvedValue(config("LESSON"));render(<ConfigurationPanel studioId="s" mode="bookings" onComplete={vi.fn()}/>);expect(await screen.findByRole("link",{name:"이용권"})).toBeInTheDocument();expect(screen.getByRole("link",{name:"출석"})).toBeInTheDocument();});
it("beauty direct lesson route redirects without rendering lesson children",async()=>{vi.mocked(api).mockResolvedValue(config("BEAUTY"));render(<ConfigurationPanel studioId="s" mode="lesson-products" target="LESSON" onComplete={vi.fn()}><p>Secret lesson content</p></ConfigurationPanel>);await waitFor(()=>expect(router.replace).toHaveBeenCalledWith("/app"));expect(screen.queryByText("Secret lesson content")).not.toBeInTheDocument();});
