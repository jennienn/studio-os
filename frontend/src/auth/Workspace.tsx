"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState, FormEvent } from "react";
import { api, ApiError } from "./api";
import { ConfigurationPanel, WorkspaceMode } from "@/configuration/ConfigurationPanel";
import { Category } from "@/configuration/model";
import { CustomerPanel } from "@/operations/CustomerPanel";
import { PaymentPanel } from "@/operations/PaymentPanel";
import { BookingPanel } from "@/operations/BookingPanel";
import {ProductPanel} from "@/lesson/ProductPanel";
import {EnrollmentPanel} from "@/lesson/EnrollmentPanel";
import {ClassPanel} from "@/lesson/ClassPanel";
import {AttendancePanel} from "@/lesson/AttendancePanel";
type Studio={id:string;name:string;slug:string;timezone:string;businessCategory:string;status:string;role:"OWNER"|"MANAGER"|"STAFF"};
type Me={id:string;email:string|null;name:string;studios:Studio[];activeStudioId:string|null};
export function Workspace({mode="home",target=null,resourceId}:{mode?:WorkspaceMode;target?:Category|null;resourceId?:string}) {
  const router=useRouter();
  const [me,setMe]=useState<Me|null>(null); const [error,setError]=useState(""); const [busy,setBusy]=useState(false);
  const [name,setName]=useState(""); const [slug,setSlug]=useState(""); const [timezone,setTimezone]=useState("");
  const failed=useCallback((e:unknown) => {
    if (e instanceof ApiError && e.status===401) { setMe(null); router.replace("/login?expired=1"); }
    else setError(e instanceof Error ? e.message : "연결을 확인해 주세요.");
  },[router]);
  const load=useCallback(async () => { try { setMe(await api<Me>("/auth/me")); } catch(e) { failed(e); } },[failed]);
  useEffect(()=> {
    setTimezone(Intl.DateTimeFormat().resolvedOptions().timeZone);
    void load();
    const focus=()=>{ void load(); };
    window.addEventListener("focus",focus); return ()=>window.removeEventListener("focus",focus);
  },[load]);
  async function create(event:FormEvent) {
    event.preventDefault(); setBusy(true); setError("");
    try { await api("/studios",{name,slug,timezone}); setName(""); setSlug(""); await load(); } catch(e) { failed(e); }
    finally { setBusy(false); }
  }
  async function activate(id:string) {
    if (!id) return;
    setBusy(true); setError("");
    try { await api("/studios/"+id+"/activate",{}); await load(); } catch(e) { failed(e); } finally { setBusy(false); }
  }
  async function logout() {
    setBusy(true);
    try { await api("/auth/logout",{}); setMe(null); router.replace("/login"); } catch(e) { failed(e); setBusy(false); }
  }
  const active=me?.studios.find(s=>s.id===me.activeStudioId);
  return <main className="workspace-page">
    <header className="workspace-header"><Link className="brand" href="/">Studio OS</Link>
      {me && <div><span>{me.name}</span><button className="button" disabled={busy} onClick={logout}>로그아웃</button></div>}
    </header>
    {error && <p role="alert" className="auth-error">{error}</p>}
    {!me ? <section><p role="status">로그인 상태를 확인하고 있습니다.</p>{error && <button className="button" onClick={()=>void load()}>다시 시도</button>}</section> :
    <section className="workspace-content">
      <h1>내 사업장</h1>
      {me.studios.length>1 && <label>현재 사업장<select value={me.activeStudioId ?? ""} disabled={busy} onChange={e=>void activate(e.target.value)}>
        <option value="">사업장을 선택해 주세요</option>{me.studios.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}
      </select></label>}
      {active && <section className="workspace-studio"><h2>{active.name}</h2>
        <dl><dt>사업장 주소</dt><dd>{active.slug}</dd><dt>시간대</dt><dd>{active.timezone}</dd>
          <dt>내 역할</dt><dd>{active.role==="OWNER" ? "소유자" : active.role==="MANAGER" ? "매니저" : "스태프"}</dd></dl></section>}
      {active && !busy && <ConfigurationPanel key={active.id} studioId={active.id} mode={mode} target={target} onComplete={load}>{view=><>
        {mode==="customers" && <CustomerPanel key={active.id+resourceId} studioId={active.id} category={active.businessCategory} id={resourceId}/>}
        {mode==="customers" && resourceId && resourceId!=="new" && active.businessCategory==="LESSON" && active.role!=="STAFF" && <EnrollmentPanel key={active.id+resourceId+"lesson"} studioId={active.id} customerId={resourceId} role={active.role} groupEnabled={!!view.configuration?.capabilities.GROUP_CLASS}/>}
        {mode==="lesson-products" && <ProductPanel studioId={active.id}/>}
        {mode==="lesson-enrollments" && <EnrollmentPanel studioId={active.id} role={active.role} groupEnabled={!!view.configuration?.capabilities.GROUP_CLASS}/>}
        {mode==="lesson-classes" && <ClassPanel studioId={active.id}/>}
        {mode==="lesson-attendance" && <AttendancePanel studioId={active.id} timezone={active.timezone} role={active.role} attendanceEnabled={!!view.configuration?.capabilities.ATTENDANCE}/>}
        {mode==="payments" && <PaymentPanel key={active.id+resourceId} studioId={active.id} category={active.businessCategory} role={active.role} id={resourceId}/>}
        {mode==="bookings" && <BookingPanel key={active.id} studioId={active.id} category={active.businessCategory} timezone={active.timezone} role={active.role} privateEnabled={!!view.configuration?.capabilities.PRIVATE_LESSON} attendanceEnabled={!!view.configuration?.capabilities.ATTENDANCE}/>}
      </>}</ConfigurationPanel>}
      <details open={me.studios.length===0}><summary>{me.studios.length===0 ? "첫 사업장 만들기" : "새 사업장 만들기"}</summary>
        <form className="auth-form" onSubmit={create}>
          <label>사업장 이름<input required maxLength={100} value={name} onChange={e=>setName(e.target.value)}/></label>
          <label>사업장 주소<input required minLength={3} maxLength={63} pattern="[A-Za-z0-9]+(-[A-Za-z0-9]+)*" value={slug} onChange={e=>setSlug(e.target.value)}/></label>
          <small>영문·숫자·하이픈 3~63자. 이미 사용 중인 주소는 사용할 수 없습니다.</small>
          <label>시간대<input required maxLength={64} value={timezone} onChange={e=>setTimezone(e.target.value)} placeholder="Asia/Seoul"/></label>
          <button className="button primary" disabled={busy}>{busy ? "처리 중…" : "사업장 만들기"}</button>
        </form>
      </details>
    </section>}
  </main>;
}
