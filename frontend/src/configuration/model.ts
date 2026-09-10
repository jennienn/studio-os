export type Category = "LESSON" | "BEAUTY";
export type Capability = "PRIVATE_LESSON" | "GROUP_CLASS" | "ATTENDANCE" | "CUSTOMER_BOOKING" | "PASS_MANAGEMENT" | "DEPOSIT" | "REVISIT";
export type Hours = {weekday:number;openTime:string|null;closeTime:string|null;closed:boolean};
export type BookingPolicy = {slotIntervalMinutes:number;bookingWindowDays:number;cancellationCutoffHours:number};
export type LessonPolicy = {lowBalanceThreshold:number;expiryAlertDays:number;restoreOnTimelyCancellation:boolean};
export type BeautyPolicy = {depositEnabled:boolean;noShowEnabled:boolean};
export type Settings = {
  capabilities:Partial<Record<Capability,boolean>>; businessHours:Hours[];
  bookingPolicy:BookingPolicy; lessonPolicy:LessonPolicy|null; beautyPolicy:BeautyPolicy|null;
};
export type ConfigurationView = {
  studioId:string;status:"PRE_ONBOARDING"|"ACTIVE";version:number;
  businessCategory:Category|null;businessType:string|null;
  catalog:Partial<Record<Category,{businessTypes:string[];capabilities:Capability[]}>>;
  bookingDefaults:BookingPolicy;configuration:Settings|null;
  permissions:{completeOnboarding:boolean;editIdentity:boolean;editCapabilities:boolean;editPolicies:boolean;editDeposit:boolean};
};
export type Draft = {
  businessCategory:Category|null; businessType:string;
  capabilities:Partial<Record<Capability,boolean>>; businessHours:Hours[];
  bookingPolicy:BookingPolicy;
  lessonPolicy:{lowBalanceThreshold:number|"";expiryAlertDays:number|"";restoreOnTimelyCancellation:boolean|null}|null;
  beautyPolicy:{depositEnabled:boolean;noShowEnabled:boolean|null}|null;
};
export const categoryLabels:Record<Category,string>={LESSON:"레슨 / 스튜디오",BEAUTY:"뷰티 / 예약 서비스"};
// Labels are presentation only; subtype/capability options come from the server's hierarchical catalog.
export const typeLabels:Record<string,string>={DANCE:"댄스",PILATES:"필라테스",YOGA:"요가",PT:"PT",POLE:"폴댄스",VOCAL_MUSIC:"보컬/음악",OTHER_LESSON:"기타 레슨",
  NAIL:"네일",EYELASH:"속눈썹",HAIR_EXTENSION:"붙임머리",WAXING:"왁싱",HAIR:"1인 미용",OTHER_BEAUTY:"기타 뷰티"};
export const capabilityLabels:Record<Capability,string>={PRIVATE_LESSON:"개인 레슨",GROUP_CLASS:"그룹 수업",ATTENDANCE:"그룹 출석 관리",
  CUSTOMER_BOOKING:"고객 직접 예약",PASS_MANAGEMENT:"이용권 관리",DEPOSIT:"예약금 사용",REVISIT:"재방문 관리"};
export const weekdays=["월요일","화요일","수요일","목요일","금요일","토요일","일요일"];
export function initialDraft(view:ConfigurationView):Draft {
  if (view.configuration) return structuredClone({...view.configuration,businessCategory:view.businessCategory,businessType:view.businessType ?? ""});
  return {businessCategory:null,businessType:"",capabilities:{},
    businessHours:weekdays.map((_,i)=>({weekday:i+1,closed:true,openTime:null,closeTime:null})),
    bookingPolicy:{...view.bookingDefaults},lessonPolicy:null,beautyPolicy:null};
}
export function chooseCategory(draft:Draft,category:Category,view:ConfigurationView):Draft {
  return {...draft,businessCategory:category,businessType:"",
    capabilities:Object.fromEntries((view.catalog[category]?.capabilities ?? []).map(c=>[c,c==="PASS_MANAGEMENT"])),
    lessonPolicy:category==="LESSON" ? {lowBalanceThreshold:"",expiryAlertDays:"",restoreOnTimelyCancellation:null} : null,
    beautyPolicy:category==="BEAUTY" ? {depositEnabled:false,noShowEnabled:null} : null};
}
export function configurationRoute(status:string,category:string|null,target:Category|null):"/onboarding"|"/app"|null {
  if (status!=="ACTIVE") return "/onboarding";
  if (target!==null && category!==target) return "/app";
  return null;
}
