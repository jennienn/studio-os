"use client";
import {useEffect,useState} from "react";
import {api} from "@/auth/api";
import {useCommand} from "@/operations/useCommand";
import {CustomerPicker} from "@/operations/CustomerPicker";
import {localInstant,localValue,addDays} from "@/operations/studioTime";
import {CyclePicker} from "./CyclePicker";
import {ClassView,Occurrence} from "./types";
type Row={bookingId:string;customerName:string;customerPhone:string;bookingStatus:string;attendanceStatus:string|null;remaining:number|null;available:number|null;paymentNeeded:boolean};
export function AttendancePanel({studioId,timezone,role,attendanceEnabled}:{studioId:string;timezone:string;role:string;attendanceEnabled:boolean}){
 const base=`/studios/${studioId}/lesson`,command=useCommand(),manager=role!=="STAFF";
 const [classes,setClasses]=useState<ClassView[]>([]),[clazz,setClazz]=useState(""),[classPage,setClassPage]=useState(0),[month,setMonth]=useState(()=>localValue(new Date(),timezone).slice(0,7)),[day,setDay]=useState("");
 const [occurrences,setOccurrences]=useState<Occurrence[]>([]),[occurrence,setOccurrence]=useState(""),[occPage,setOccPage]=useState(0),[rows,setRows]=useState<Row[]>([]),[page,setPage]=useState(0),[revision,setRevision]=useState(0);
 const [customer,setCustomer]=useState(""),[cycle,setCycle]=useState(""),[marks,setMarks]=useState<Record<string,string>>({}),[busy,setBusy]=useState(false),[error,setError]=useState("");
 useEffect(()=>{let live=true;api<ClassView[]>(base+`/classes?page=${classPage}`).then(r=>{if(live)setClasses(r);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,classPage]);
 useEffect(()=>{let live=true;setOccurrences([]);if(!clazz)return;try{const first=month+"-01",next=addDays(first,32).slice(0,7)+"-01";api<Occurrence[]>(base+`/occurrences?classId=${clazz}&from=${encodeURIComponent(localInstant(first+"T00:00",timezone))}&to=${encodeURIComponent(localInstant(next+"T00:00",timezone))}&page=${occPage}&size=100`).then(r=>{if(live)setOccurrences(r);}).catch(e=>{if(live)setError(e.message);});}catch(e){setError(e instanceof Error?e.message:"날짜 오류");}return()=>{live=false;};},[base,clazz,month,timezone,occPage,revision]);
 useEffect(()=>{let live=true;setRows([]);setMarks({});if(occurrence)api<Row[]>(base+`/occurrences/${occurrence}/attendance?page=${page}`).then(r=>{if(live)setRows(r);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,occurrence,page,revision]);
 async function run(work:()=>Promise<unknown>){setBusy(true);setError("");try{await work();setRevision(r=>r+1);}catch(e){setError(e instanceof Error?e.message:"처리 실패");}finally{setBusy(false);}}
 const current=occurrences.find(o=>o.id===occurrence);const days=month?new Date(Number(month.slice(0,4)),Number(month.slice(5)),0).getDate():0;
 return <section className="operations-panel"><h2>그룹 일정 · 출석</h2>{error&&<p role="alert">{error}</p>}
 <label>반 선택<select aria-label="반 선택" value={clazz} onChange={e=>{setClazz(e.target.value);setOccurrence("");setDay("");setOccPage(0);}}><option value="">반을 선택해 주세요</option>{classes.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
 <button className="button" disabled={classPage===0} onClick={()=>setClassPage(p=>p-1)}>반 이전</button><button className="button" disabled={classes.length<20} onClick={()=>setClassPage(p=>p+1)}>반 다음</button>
 <label>수업 월<input type="month" value={month} onChange={e=>{setMonth(e.target.value);setOccurrence("");setDay("");setOccPage(0);}}/></label>
 <div className="lesson-month" aria-label="월간 수업 날짜">{Array.from({length:days},(_,i)=>`${month}-${String(i+1).padStart(2,"0")}`).map(d=><button className={d===day?"button primary":"button"} key={d} onClick={()=>{setDay(d);setOccurrence("");}}>{Number(d.slice(-2))}{occurrences.some(o=>localValue(new Date(o.startAt),timezone).startsWith(d))?" ·":""}</button>)}</div>
 <ul className="operations-list">{occurrences.filter(o=>!day||localValue(new Date(o.startAt),timezone).startsWith(day)).map(o=><li key={o.id}><span>{localValue(new Date(o.startAt),timezone).replace("T"," ")} · 예약 {o.bookedCount}/{o.capacitySnapshot} · {o.status==="SCHEDULED"?"예정":o.status==="COMPLETED"?"완료":"취소"}</span><button className="button" onClick={()=>{setOccurrence(o.id);setPage(0);}}>회차 선택</button></li>)}</ul>
 <button className="button" disabled={occPage===0} onClick={()=>setOccPage(p=>p-1)}>일정 이전</button><button className="button" disabled={occurrences.length<100} onClick={()=>setOccPage(p=>p+1)}>일정 다음</button>
 {current&&<><h3>{current.className} · {localValue(new Date(current.startAt),timezone).replace("T"," ")}</h3>
 {manager&&current.status==="SCHEDULED"&&<form className="auth-form" onSubmit={e=>{e.preventDefault();void run(()=>command(base+"/group-bookings",{customerId:customer,enrollmentCycleId:cycle,classOccurrenceId:occurrence}));}}>
 <CustomerPicker studioId={studioId} label="회원" value={customer} onChange={setCustomer}/><CyclePicker studioId={studioId} customerId={customer} kind="GROUP" classId={clazz} value={cycle} onChange={setCycle}/><button className="button primary" disabled={busy}>그룹 예약 추가</button></form>}
 <h3>참여 회원</h3>{rows.length===0&&<p>예약된 회원이 없습니다.</p>}
 {attendanceEnabled&&<label><input type="checkbox" checked={rows.some(r=>!r.attendanceStatus&&r.bookingStatus==="CONFIRMED")&&rows.filter(r=>!r.attendanceStatus&&r.bookingStatus==="CONFIRMED").every(r=>marks[r.bookingId]==="PRESENT")} onChange={e=>setMarks(Object.fromEntries(rows.filter(r=>!r.attendanceStatus&&r.bookingStatus==="CONFIRMED").map(r=>[r.bookingId,e.target.checked?"PRESENT":""])))}/>현재 페이지 전체 출석</label>}
 <ul className="operations-list">{rows.map(r=><li key={r.bookingId}><div>{r.customerName} · {r.customerPhone}<p>{r.remaining===null?"기간권":`잔여 ${r.remaining}회 · 예약 가능 ${r.available}회`}{r.paymentNeeded?" · 결제 필요":""}</p></div>
 {r.attendanceStatus?<strong>{{PRESENT:"출석",ABSENT:"결석",CANCELLED:"취소"}[r.attendanceStatus]}</strong>:attendanceEnabled?<div><label><input type="checkbox" aria-label={`${r.customerName} 출석`} checked={marks[r.bookingId]==="PRESENT"} disabled={r.bookingStatus!=="CONFIRMED"} onChange={e=>setMarks({...marks,[r.bookingId]:e.target.checked?"PRESENT":""})}/>출석</label><select aria-label={`${r.customerName} 결과`} value={marks[r.bookingId]??""} onChange={e=>setMarks({...marks,[r.bookingId]:e.target.value})}><option value="">미선택</option>{r.bookingStatus==="CONFIRMED"&&<><option value="PRESENT">출석</option><option value="ABSENT">결석</option></>}<option value="CANCELLED">취소</option></select></div>:<span>{r.bookingStatus}</span>}</li>)}</ul>
 <button className="button" disabled={page===0} onClick={()=>setPage(p=>p-1)}>회원 이전</button><button className="button" disabled={rows.length<20} onClick={()=>setPage(p=>p+1)}>회원 다음</button>
 {attendanceEnabled&&<button className="button primary" disabled={busy||!Object.values(marks).some(Boolean)} onClick={()=>void run(()=>command(base+`/occurrences/${occurrence}/attendance`,{entries:Object.entries(marks).filter(([,s])=>s).map(([bookingId,status])=>({bookingId,status}))},"PUT"))}>출석 일괄 저장</button>}
 {manager&&<button className="button" disabled={busy||current.bookedCount>0||current.status!=="SCHEDULED"} onClick={()=>void run(()=>command(base+`/occurrences/${occurrence}/complete`))}>회차 완료</button>}
 </>}
 </section>;
}
