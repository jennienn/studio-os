import {vi,it,expect,afterEach} from "vitest";
import {render,screen,fireEvent,waitFor} from "@testing-library/react";
import {BookingPanel} from "@/operations/BookingPanel";
import {localInstant,localValue} from "@/operations/studioTime";
import {api} from "@/auth/api";
vi.mock("@/auth/api",async original=>({...await original<object>(),api:vi.fn()}));afterEach(()=>vi.clearAllMocks());
const booking={id:"b",customerName:"Customer",customerPhone:"01012345678",staffId:"staff",staffName:"Owner",startAt:"2030-01-01T01:00:00Z",endAt:"2030-01-01T02:00:00Z",status:"CONFIRMED",note:""};
function reads(items:unknown[]=[]){vi.mocked(api).mockImplementation(async(path)=>{
  if(path.endsWith("booking-staff"))return [{id:"staff",name:"Owner"}];
  if(path.includes("customers?"))return {items:[{id:"customer",name:"Customer",phone:"01012345678"}],totalPages:1};
  return {items:path.includes("/bookings?")?items:[],totalPages:1};
});}
it("uses Studio timezone and rejects nonexistent/ambiguous local times",()=>{
  expect(localInstant("2030-01-01T10:00","Asia/Seoul")).toBe("2030-01-01T01:00:00.000Z");
  expect(localValue(new Date("2030-01-01T01:00:00Z"),"Asia/Seoul")).toBe("2030-01-01T10:00");
  expect(()=>localInstant("2026-03-08T02:30","America/New_York")).toThrow();
  expect(()=>localInstant("2026-11-01T01:30","America/New_York")).toThrow();
});
it("creates an API-backed manual booking and displays a server conflict",async()=>{
  reads();render(<BookingPanel studioId="s" category="LESSON" timezone="Asia/Seoul" role="OWNER"/>);
  fireEvent.click(screen.getByRole("button",{name:"예약 만들기"}));await screen.findByRole("option",{name:"Customer · 01012345678"});
  fireEvent.change(screen.getByLabelText("회원 선택"),{target:{value:"customer"}});fireEvent.change(screen.getByLabelText("일정 날짜"),{target:{value:"2030-01-01"}});
  fireEvent.change(screen.getByLabelText("시작 시간"),{target:{value:"10:00"}});fireEvent.change(screen.getByLabelText("종료 시간"),{target:{value:"11:00"}});
  vi.mocked(api).mockRejectedValueOnce(new Error("담당자의 기존 예약과 시간이 겹칩니다."));fireEvent.click(screen.getByRole("button",{name:"예약 저장"}));
  await screen.findByText("담당자의 기존 예약과 시간이 겹칩니다.");
  expect(api).toHaveBeenLastCalledWith("/studios/s/bookings",expect.objectContaining({customerId:"customer",bookingKind:"LESSON_PRIVATE",startAt:"2030-01-01T01:00:00.000Z"}),"POST",expect.any(String));
});
it.each(["COMPLETED","CANCELLED","NO_SHOW"])("%s has no mutation buttons",async(status)=>{
  reads([{...booking,status}]);render(<BookingPanel studioId="s" category="BEAUTY" timezone="Asia/Seoul" role="OWNER"/>);await screen.findByText("Customer");
  expect(screen.queryByRole("button",{name:"예약 취소"})).not.toBeInTheDocument();expect(screen.queryByRole("button",{name:"완료 처리"})).not.toBeInTheDocument();
});
it("sends explicit cancellation and refreshes list",async()=>{
  reads([booking]);render(<BookingPanel studioId="s" category="LESSON" timezone="Asia/Seoul" role="OWNER"/>);await screen.findByText("Customer");
  fireEvent.click(screen.getByRole("button",{name:"예약 취소"}));await waitFor(()=>expect(api).toHaveBeenCalledWith("/studios/s/bookings/b/cancel",{},"POST",expect.any(String)));
});
it("staff sees no editing or block controls",async()=>{
  reads([booking]);render(<BookingPanel studioId="s" category="LESSON" timezone="Asia/Seoul" role="STAFF"/>);await screen.findByText("Customer");
  expect(screen.queryByRole("button",{name:"예약 만들기"})).not.toBeInTheDocument();expect(screen.queryByRole("button",{name:"시간 차단"})).not.toBeInTheDocument();
});
it("creates a Studio block without a staff reference",async()=>{
  reads();render(<BookingPanel studioId="s" category="BEAUTY" timezone="Asia/Seoul" role="OWNER"/>);fireEvent.click(screen.getByRole("button",{name:"시간 차단"}));
  fireEvent.change(screen.getByLabelText("일정 날짜"),{target:{value:"2030-01-01"}});fireEvent.change(screen.getByLabelText("시작 시간"),{target:{value:"10:00"}});fireEvent.change(screen.getByLabelText("종료 시간"),{target:{value:"11:00"}});
  fireEvent.click(screen.getByRole("button",{name:"차단 저장"}));await waitFor(()=>expect(api).toHaveBeenCalledWith("/studios/s/booking-blocks",expect.objectContaining({scopeType:"STUDIO",staffId:null}),"POST",expect.any(String)));
});
