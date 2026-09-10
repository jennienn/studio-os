"use client";
import {useEffect,useState,FormEvent} from "react";
import {api} from "@/auth/api";
import {useCommand} from "@/operations/useCommand";
import {ClassView} from "./types";
type Schedule={id:string;weekday:number;startTime:string;durationMinutes:number;active:boolean};
export function ClassPanel({studioId}:{studioId:string}){
 const base=`/studios/${studioId}`,command=useCommand();const [classes,setClasses]=useState<ClassView[]>([]),[staff,setStaff]=useState<{id:string;name:string}[]>([]),[schedules,setSchedules]=useState<Schedule[]>([]);
 const [page,setPage]=useState(0),[revision,setRevision]=useState(0),[id,setId]=useState(""),[name,setName]=useState(""),[capacity,setCapacity]=useState(""),[instructor,setInstructor]=useState(""),[active,setActive]=useState(true);
 const [scheduleId,setScheduleId]=useState(""),[weekday,setWeekday]=useState("1"),[time,setTime]=useState(""),[duration,setDuration]=useState(""),[scheduleActive,setScheduleActive]=useState(true),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
 useEffect(()=>{let live=true;api<ClassView[]>(base+`/lesson/classes?page=${page}`).then(r=>{if(live)setClasses(r);}).catch(e=>{if(live)setError(e.message);});api<typeof staff>(base+"/booking-staff").then(r=>{if(live)setStaff(r);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,page,revision]);
 useEffect(()=>{let live=true;setSchedules([]);if(id)api<Schedule[]>(base+`/lesson/classes/${id}/schedules`).then(r=>{if(live)setSchedules(r);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,id,revision]);
 async function run(work:()=>Promise<void>){setBusy(true);setError("");setMessage("");try{await work();setRevision(r=>r+1);setMessage("저장했습니다.");}catch(e){setError(e instanceof Error?e.message:"저장 실패");}finally{setBusy(false);}}
 function choose(c:ClassView){setId(c.id);setName(c.name);setCapacity(String(c.capacity));setInstructor(c.instructorStaffId??"");setActive(c.active);setScheduleId("");}
 return <section className="operations-panel"><h2>그룹 수업</h2>{error&&<p role="alert">{error}</p>}{message&&<p role="status">{message}</p>}
 <ul className="operations-list">{classes.map(c=><li key={c.id}><span>{c.name} · 정원 {c.capacity} · {c.active?"운영 중":"비활성"}</span><button className="button" onClick={()=>choose(c)}>반 선택 · 수정</button></li>)}</ul>
 <button className="button" disabled={page===0} onClick={()=>setPage(p=>p-1)}>반 이전</button><button className="button" disabled={classes.length<20} onClick={()=>setPage(p=>p+1)}>반 다음</button>
 <button className="button" onClick={()=>{setId("");setName("");setCapacity("");setInstructor("");setActive(true);}}>새 반</button>
 <form className="auth-form" onSubmit={e=>{e.preventDefault();void run(async()=>{const c=await command<ClassView>(base+"/lesson/classes"+(id?`/${id}`:""),{name,capacity:Number(capacity),instructorStaffId:instructor||null,active},id?"PUT":"POST");setId(c.id);});}}>
 <label>반 이름<input required maxLength={100} value={name} onChange={e=>setName(e.target.value)}/></label><label>정원<input type="number" min={1} max={2147483647} required value={capacity} onChange={e=>setCapacity(e.target.value)}/></label>
 <label>담당 강사<select aria-label="담당 강사" value={instructor} onChange={e=>setInstructor(e.target.value)}><option value="">미지정</option>{staff.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
 <label><input type="checkbox" checked={active} onChange={e=>setActive(e.target.checked)}/>반 활성</label><button className="button primary" disabled={busy}>반 저장</button></form>
 {id&&<><h3>반복 일정</h3><p>예약 이력이 있는 수업 회차는 일정 변경 시에도 유지됩니다. 정원·강사 변경은 새로 생성되는 회차부터 적용됩니다.</p>
 <ul className="operations-list">{schedules.map(s=><li key={s.id}>{["월","화","수","목","금","토","일"][s.weekday-1]} {s.startTime} · {s.durationMinutes}분 · {s.active?"활성":"비활성"}<button className="button" onClick={()=>{setScheduleId(s.id);setWeekday(String(s.weekday));setTime(s.startTime.slice(0,5));setDuration(String(s.durationMinutes));setScheduleActive(s.active);}}>일정 수정</button></li>)}</ul>
 <button className="button" onClick={()=>{setScheduleId("");setScheduleActive(true);}}>새 반복 일정</button>
 <form className="auth-form" onSubmit={e=>{e.preventDefault();void run(async()=>{await command(base+`/lesson/classes/${id}/schedules`+(scheduleId?`/${scheduleId}`:""),{weekday:Number(weekday),startTime:time,durationMinutes:Number(duration),active:scheduleActive},scheduleId?"PUT":"POST");});}}>
 <label>수업 요일<select aria-label="수업 요일" value={weekday} onChange={e=>setWeekday(e.target.value)}>{["월","화","수","목","금","토","일"].map((v,i)=><option key={v} value={i+1}>{v}</option>)}</select></label>
 <label>수업 시작<input type="time" required value={time} onChange={e=>setTime(e.target.value)}/></label><label>수업 시간 (분)<input type="number" min={1} max={1439} required value={duration} onChange={e=>setDuration(e.target.value)}/></label>
 <label><input type="checkbox" checked={scheduleActive} onChange={e=>setScheduleActive(e.target.checked)}/>일정 활성</label><button className="button primary" disabled={busy}>반복 일정 저장</button></form></>}
 </section>;
}
