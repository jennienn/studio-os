"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, FormEvent, ReactNode } from "react";
import { api, ApiError } from "@/auth/api";
import { Category, ConfigurationView, Draft, initialDraft, chooseCategory, configurationRoute, categoryLabels } from "./model";
import { ConfigurationFields } from "./ConfigurationFields";
import { ConfigurationSummary } from "./ConfigurationSummary";

export type WorkspaceMode="home"|"onboarding"|"settings"|"guard"|"customers"|"payments"|"bookings";
export function ConfigurationPanel({studioId,mode,target=null,onComplete,children}:{studioId:string;mode:WorkspaceMode;target?:Category|null;onComplete:()=>Promise<void>;children?:ReactNode}) {
  const router=useRouter();
  const [view,setView]=useState<ConfigurationView|null>(null);
  const [draft,setDraft]=useState<Draft|null>(null);
  const [step,setStep]=useState(0),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
  const fail=useCallback((e:unknown)=>{
    if(e instanceof ApiError && e.status===401) { setView(null);setDraft(null);router.replace("/login?expired=1"); }
    else { if(e instanceof ApiError && e.status===403) {setView(null);setDraft(null);} setError(e instanceof Error?e.message:"설정을 불러오지 못했습니다."); }
  },[router]);
  const load=useCallback(async()=>{
    setError("");setMessage("");
    try { const result=await api<ConfigurationView>(`/studios/${studioId}/configuration`);setView(result);setDraft(initialDraft(result)); }
    catch(e){fail(e);}
  },[studioId,fail]);
  useEffect(()=>{void load();},[load]);
  const redirect=view ? configurationRoute(view.status,view.businessCategory,target):null;
  useEffect(()=>{
    if(!view) return;
    if(redirect && !(redirect==="/onboarding" && mode==="onboarding")) router.replace(redirect);
    else if(view.status==="ACTIVE" && (mode==="onboarding" || mode==="guard")) router.replace("/app");
  },[view,redirect,mode,router]);
  function validateStep():string {
    if(!draft?.businessCategory) return "사업장 유형을 선택해 주세요.";
    if(step===1 && !draft.businessType) return "세부 업종을 선택해 주세요.";
    if(step===2 && draft.businessCategory==="LESSON") {
      if(!draft.capabilities.PRIVATE_LESSON && !draft.capabilities.GROUP_CLASS) return "개인 레슨 또는 그룹 수업을 선택해 주세요.";
      if(draft.capabilities.ATTENDANCE && !draft.capabilities.GROUP_CLASS) return "출석 관리에는 그룹 수업이 필요합니다.";
    }
    if(step===3 && draft.businessHours.some(h=>!h.closed && (!h.openTime || !h.closeTime || h.openTime>=h.closeTime))) return "영업일의 시작 시간은 종료 시간보다 빨라야 합니다.";
    return "";
  }
  async function submit(event:FormEvent) {
    event.preventDefault();setError("");setMessage("");
    if(!view || !draft || busy) return;
    if(mode==="onboarding" && step<5) {const problem=validateStep();if(problem){setError(problem);return;}setStep(step+1);return;}
    setBusy(true);
    try {
      const result=await api<ConfigurationView>(`/studios/${studioId}/${mode==="onboarding" ? "onboarding/complete" : "configuration"}`,
        {...draft,version:view.version},mode==="onboarding" ? "POST" : "PUT");
      setView(result);setDraft(initialDraft(result));setMessage("설정을 저장했습니다.");
      await onComplete();
      if(mode==="onboarding") router.replace("/app");
    } catch(e){fail(e);} finally{setBusy(false);}
  }
  if(!view || !draft) return <section>{error ? <p role="alert">{error}</p> : <p role="status">설정을 불러오고 있습니다.</p>}<button className="button" onClick={()=>void load()}>다시 불러오기</button></section>;
  if((redirect && !(redirect==="/onboarding" && mode==="onboarding")) || mode==="guard" || (mode==="onboarding" && view.status==="ACTIVE")) return <p role="status">사업장 화면으로 이동합니다.</p>;
  if(mode==="onboarding" && !view.permissions.completeOnboarding) return <p>소유자가 사업장 온보딩을 완료해야 합니다.</p>;
  const sections=["type","capabilities","hours","policies"] as const;
  return <section className="configuration-panel">
    {view.status==="ACTIVE" && <>
      <p className="configuration-category" data-testid="active-category">{categoryLabels[view.businessCategory!]} · ACTIVE</p>
      <nav aria-label="사업장 메뉴"><Link href="/app" aria-current={mode==="home"?"page":undefined}>홈</Link>
        {view.permissions.editPolicies && <Link href="/app/customers" aria-current={mode==="customers"?"page":undefined}>{view.businessCategory==="LESSON"?"회원":"고객"}</Link>}
        {view.permissions.editPolicies && <Link href="/app/payments" aria-current={mode==="payments"?"page":undefined}>결제</Link>}
        <Link href="/app/bookings" aria-current={mode==="bookings"?"page":undefined}>예약</Link>
        <Link href="/app/settings" aria-current={mode==="settings"?"page":undefined}>설정</Link></nav>
    </>}
    {mode==="bookings" ? children : mode==="customers" || mode==="payments" ? (view.permissions.editPolicies?children:<p role="alert">이 화면에 접근할 권한이 없습니다.</p>) : mode==="home" ? <><h2>사업장 설정 완료</h2><p>위 메뉴에서 운영 업무를 시작하거나 설정을 확인할 수 있습니다.</p><ConfigurationSummary draft={draft}/></> :
      <form onSubmit={submit} className="configuration-form">
        <h2>{mode==="onboarding" ? "사업장 시작하기" : "사업장 설정"}</h2>
        {mode==="onboarding" ? <>
          <p className="muted" role="status">{step+1} / 6 단계 · {['사업장 유형','세부 업종','사용 기능','영업시간','운영 정책','설정 확인'][step]}</p>
          {step===0 && <fieldset><legend>어떤 형태의 사업장을 운영하시나요?</legend><div className="select-card-grid two">
            {(["LESSON","BEAUTY"] as const).map(category=><button className={"select-card"+(draft.businessCategory===category?" selected":"")} type="button" key={category}
              aria-pressed={draft.businessCategory===category} onClick={()=>setDraft(chooseCategory(draft,category,view))}>{categoryLabels[category]}</button>)}
          </div></fieldset>}
          {step>=1 && step<=4 && <ConfigurationFields view={view} draft={draft} set={setDraft} section={sections[step-1]} onboarding/>}
          {step===5 && <ConfigurationSummary draft={draft}/>}
        </> : <>
          <p>사업장 유형은 변경할 수 없습니다. 세부 업종과 사용 기능은 소유자만 변경할 수 있습니다.</p>
          {!view.permissions.editPolicies && <p>현재 역할은 설정을 조회할 수 있습니다.</p>}
          {sections.map(section=><ConfigurationFields key={section} view={view} draft={draft} set={setDraft} section={section}/>)}
        </>}
        {error && <p role="alert" className="auth-error">{error}</p>}
        {message && <p role="status">{message}</p>}
        <div className="configuration-actions">
          {mode==="onboarding" && step>0 && <button type="button" className="button" disabled={busy} onClick={()=>{setError("");setStep(step-1);}}>이전</button>}
          {(mode==="onboarding" || view.permissions.editPolicies) && <button className="button primary" disabled={busy}>{busy?"저장 중…":mode==="onboarding"?(step===5?"온보딩 완료":"다음"):"설정 저장"}</button>}
          {mode==="settings" && <button type="button" className="button" disabled={busy} onClick={()=>void load()}>최신 설정 다시 불러오기</button>}
        </div>
      </form>}
  </section>;
}
