import { describe, it, expect, vi, afterEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { chooseCategory, initialDraft, configurationRoute, ConfigurationView } from "@/configuration/model";
import { ConfigurationSummary } from "@/configuration/ConfigurationSummary";
import { ConfigurationFields } from "@/configuration/ConfigurationFields";
import { ConfigurationPanel } from "@/configuration/ConfigurationPanel";
const router=vi.hoisted(()=>({replace:vi.fn()}));
vi.mock("next/navigation",()=>({useRouter:()=>router}));
afterEach(()=>vi.restoreAllMocks());

const view:ConfigurationView={
  studioId:"studio-a",status:"PRE_ONBOARDING",version:0,businessCategory:null,businessType:null,
  catalog:{LESSON:{businessTypes:["DANCE","PILATES"],capabilities:["PRIVATE_LESSON","GROUP_CLASS","ATTENDANCE","CUSTOMER_BOOKING","PASS_MANAGEMENT"]},
    BEAUTY:{businessTypes:["NAIL","EYELASH"],capabilities:["CUSTOMER_BOOKING","DEPOSIT","REVISIT"]}},
  bookingDefaults:{slotIntervalMinutes:30,bookingWindowDays:30,cancellationCutoffHours:12},configuration:null,
  permissions:{completeOnboarding:true,editIdentity:true,editCapabilities:true,editPolicies:true,editDeposit:true},
};
describe("category-safe configuration",()=>{
  it("starts with category cards then branches without creating a draft on the server",async()=>{
    const fetch=vi.spyOn(globalThis,"fetch").mockResolvedValue(new Response(JSON.stringify(view),{status:200}));
    render(<ConfigurationPanel studioId={view.studioId} mode="onboarding" onComplete={async()=>{}}/>);
    fireEvent.click(await screen.findByRole("button",{name:"뷰티 / 예약 서비스"}));
    fireEvent.click(screen.getByRole("button",{name:"다음"}));
    expect(screen.getByRole("button",{name:"네일"})).toBeInTheDocument();
    expect(screen.queryByRole("button",{name:"댄스"})).not.toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch.mock.calls[0][1]?.method).toBe("GET");
  });
  it("clears incompatible draft values when the category changes",()=>{
    const lesson=chooseCategory(initialDraft(view),"LESSON",view);
    lesson.businessType="DANCE";
    lesson.capabilities.ATTENDANCE=true;
    const beauty=chooseCategory(lesson,"BEAUTY",view);
    expect(beauty.businessType).toBe("");
    expect(beauty.lessonPolicy).toBeNull();
    expect(beauty.capabilities).not.toHaveProperty("ATTENDANCE");
    expect(beauty.beautyPolicy).not.toBeNull();
    expect(lesson.businessType).toBe("DANCE");
  });
  it.each(["LESSON","BEAUTY"] as const)("renders only server-approved %s subtype options",category=>{
    const draft=chooseCategory(initialDraft(view),category,view);
    render(<ConfigurationFields view={view} draft={draft} set={()=>{}} section="type" onboarding/>);
    expect(screen.queryByRole("button",{name:"네일"})!==null).toBe(category==="BEAUTY");
    expect(screen.queryByRole("button",{name:"댄스"})!==null).toBe(category==="LESSON");
    expect(screen.queryByRole("button",{name:"필라테스"})!==null).toBe(category==="LESSON");
  });
  it("keeps lesson terminology out of the beauty summary",()=>{
    const draft=chooseCategory(initialDraft(view),"BEAUTY",view);
    draft.businessType="NAIL";
    const {container}=render(<ConfigurationSummary draft={draft}/>);
    expect(container).toHaveTextContent("예약금");
    expect(container).not.toHaveTextContent(/회차|이용권|출석|레슨/);
  });
  it("protects pre-onboarding and incompatible routes",()=>{
    expect(configurationRoute("PRE_ONBOARDING",null,null)).toBe("/onboarding");
    expect(configurationRoute("ACTIVE","BEAUTY","LESSON")).toBe("/app");
    expect(configurationRoute("ACTIVE","LESSON","BEAUTY")).toBe("/app");
    expect(configurationRoute("ACTIVE","LESSON","LESSON")).toBeNull();
    expect(configurationRoute("ACTIVE","BEAUTY",null)).toBeNull();
  });
  it.each(["LESSON","BEAUTY"] as const)("renders only %s policy questions",category=>{
    const draft=chooseCategory(initialDraft(view),category,view);
    render(<ConfigurationFields view={view} draft={draft} set={()=>{}} section="policies" onboarding/>);
    expect(screen.queryByLabelText("기한 내 취소 시 이용권 복구")!==null).toBe(category==="LESSON");
    expect(screen.queryByLabelText("노쇼 상태 관리")!==null).toBe(category==="BEAUTY");
  });
  it("manager cannot edit subtype or deposit capabilities",()=>{
    const manager={...view,permissions:{completeOnboarding:false,editIdentity:false,editCapabilities:false,editPolicies:true,editDeposit:false}};
    const draft=chooseCategory(initialDraft(view),"BEAUTY",view);
    render(<><ConfigurationFields view={manager} draft={draft} set={()=>{}} section="type"/>
      <ConfigurationFields view={manager} draft={draft} set={()=>{}} section="capabilities"/>
      <ConfigurationFields view={manager} draft={draft} set={()=>{}} section="policies"/></>);
    expect(screen.getByRole("button",{name:"네일"})).toBeDisabled();
    expect(screen.getByRole("checkbox",{name:"예약금 사용"})).toBeDisabled();
    expect(screen.getByLabelText("노쇼 상태 관리")).toBeEnabled();
  });
  it("staff operational settings are read-only",()=>{
    const staff={...view,permissions:{completeOnboarding:false,editIdentity:false,editCapabilities:false,editPolicies:false,editDeposit:false}};
    const draft=chooseCategory(initialDraft(view),"LESSON",view);
    render(<ConfigurationFields view={staff} draft={draft} set={()=>{}} section="policies"/>);
    expect(screen.getByLabelText("예약 시간 간격 (분)")).toBeDisabled();
    expect(screen.getByLabelText("기한 내 취소 시 이용권 복구")).toBeDisabled();
  });
});
