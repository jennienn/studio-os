"use client";
import {useEffect,useState,FormEvent} from "react";
import {api} from "@/auth/api";
import {useCommand} from "./useCommand";
import {CustomerPicker} from "./CustomerPicker";
import {PageResult} from "./CustomerPanel";
import {addDays,localInstant,localValue} from "./studioTime";
export type Booking={id:string;customerName:string;customerPhone:string;staffId:string;staffName:string;startAt:string;endAt:string;status:string;note:string|null};
type Block={id:string;scopeType:string;staffId:string|null;startAt:string;endAt:string;reason:string|null};
const statuses:Record<string,string>={PENDING:"대기",CONFIRMED:"확정",COMPLETED:"완료",CANCELLED:"취소",NO_SHOW:"노쇼"};
export function BookingPanel({studioId,category,timezone,role}:{studioId:string;category:string;timezone:string;role:string}){
  const base=`/studios/${studioId}`,command=useCommand(),canWrite=role!=="STAFF";
  const [date,setDate]=useState(()=>localValue(new Date(),timezone).slice(0,10));
  const [formDate,setFormDate]=useState(date);
  const [data,setData]=useState<PageResult<Booking>|null>(null),[blocks,setBlocks]=useState<PageResult<Block>|null>(null),[staff,setStaff]=useState<{id:string;name:string}[]>([]);
  const [page,setPage]=useState(0),[blockPage,setBlockPage]=useState(0),[revision,setRevision]=useState(0);
  const [customerId,setCustomerId]=useState(""),[staffId,setStaffId]=useState(""),[start,setStart]=useState(""),[end,setEnd]=useState(""),[note,setNote]=useState("");
  const [editing,setEditing]=useState<Booking|null>(null),[form,setForm]=useState<"booking"|"block"|null>(null),[scope,setScope]=useState("STUDIO");
  const [busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
  useEffect(()=>{if(!canWrite)return;let live=true;api<{id:string;name:string}[]>(base+"/booking-staff").then(r=>{if(live){setStaff(r);setStaffId(r[0]?.id??"");}}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,canWrite]);
  useEffect(()=>{let live=true;setData(null);setBlocks(null);
    try{const range=`from=${encodeURIComponent(localInstant(date+"T00:00",timezone))}&to=${encodeURIComponent(localInstant(addDays(date,1)+"T00:00",timezone))}`;
      api<PageResult<Booking>>(base+`/bookings?${range}&page=${page}`).then(r=>{if(live)setData(r);}).catch(e=>{if(live)setError(e.message);});
      if(canWrite)api<PageResult<Block>>(base+`/booking-blocks?${range}&page=${blockPage}`).then(r=>{if(live)setBlocks(r);}).catch(e=>{if(live)setError(e.message);});
    }catch(e){setError(e instanceof Error?e.message:"날짜 오류");}return()=>{live=false;};
  },[base,date,timezone,page,blockPage,revision,canWrite]);
  function selectDate(value:string){if(!value)return;setDate(value);setPage(0);setBlockPage(0);setEditing(null);setForm(null);setError("");setMessage("");}
  async function submit(e:FormEvent){e.preventDefault();setBusy(true);setError("");setMessage("");try{
    const startAt=localInstant(formDate+"T"+start,timezone),endAt=localInstant(formDate+"T"+end,timezone);
    if(form==="block")await command(base+"/booking-blocks",{scopeType:scope,staffId:scope==="STAFF"?staffId:null,startAt,endAt,reason:note});
    else if(editing)await command(base+"/bookings/"+editing.id,{staffId,startAt,endAt,note},"PUT");
    else await command(base+"/bookings",{customerId,staffId,bookingKind:category==="LESSON"?"LESSON_PRIVATE":"BEAUTY_SERVICE",startAt,endAt,note});
    setForm(null);setEditing(null);setDate(formDate);setPage(0);setBlockPage(0);setRevision(r=>r+1);setMessage("저장했습니다.");
  }catch(e){setError(e instanceof Error?e.message:"저장 실패");}finally{setBusy(false);}}
  async function action(path:string,method:"POST"|"DELETE"="POST"){setBusy(true);setError("");setMessage("");try{await command(base+path,{},method);setRevision(r=>r+1);setMessage("처리했습니다.");}catch(e){setError(e instanceof Error?e.message:"처리 실패");}finally{setBusy(false);}}
  function edit(b:Booking){setEditing(b);setFormDate(localValue(new Date(b.startAt),timezone).slice(0,10));setStaffId(b.staffId);setStart(localValue(new Date(b.startAt),timezone).slice(11));setEnd(localValue(new Date(b.endAt),timezone).slice(11));setNote(b.note??"");setForm("booking");}
  return <section className="operations-panel"><h2>예약 캘린더</h2><p>사업장 시간대: {timezone}</p><p>수동 1:1 일정과 상태만 기록합니다. 별도 상품 연결이나 자동 정산은 수행하지 않습니다.</p>
    <label>예약 날짜<input type="date" required value={date} onChange={e=>selectDate(e.target.value)}/></label>
    <div className="booking-week" aria-label="주간 날짜 선택">{Array.from({length:7},(_,i)=>addDays(date,i-3)).map(day=><button key={day} className={day===date?"button primary":"button"} aria-pressed={day===date} onClick={()=>selectDate(day)}>{day.slice(5)}</button>)}</div>
    {error && <p role="alert" className="auth-error">{error}</p>}{message && <p role="status">{message}</p>}
    {canWrite && <div className="configuration-actions"><button className="button primary" disabled={busy} onClick={()=>{setFormDate(date);setForm("booking");setEditing(null);setNote("");}}>예약 만들기</button><button className="button" disabled={busy} onClick={()=>{setFormDate(date);setForm("block");setEditing(null);setNote("");}}>시간 차단</button></div>}
    {form && canWrite && <form className="auth-form" onSubmit={submit}><h3>{form==="block"?"차단 설정":editing?"예약 변경":"수동 예약"}</h3>
      {form==="booking" && !editing && <CustomerPicker studioId={studioId} label={category==="LESSON"?"회원":"고객"} value={customerId} onChange={setCustomerId}/>}
      {editing && <p>{editing.customerName} · 고객 변경은 지원하지 않습니다.</p>}
      <label>일정 날짜<input type="date" required value={formDate} onChange={e=>setFormDate(e.target.value)}/></label>
      {form==="block" && <label>차단 범위<select aria-label="차단 범위" value={scope} onChange={e=>setScope(e.target.value)}><option value="STUDIO">사업장 전체</option><option value="STAFF">담당자</option></select></label>}
      {(form==="booking" || scope==="STAFF") && <label>담당자<select aria-label="담당자" required value={staffId} onChange={e=>setStaffId(e.target.value)}><option value="">선택해 주세요</option>{staff.map(s=><option key={s.id} value={s.id}>{s.name}</option>)}</select></label>}
      <label>시작 시간<input type="time" required value={start} onChange={e=>setStart(e.target.value)}/></label><label>종료 시간<input type="time" required value={end} onChange={e=>setEnd(e.target.value)}/></label>
      <label>{form==="block"?"차단 사유":"예약 메모"}<textarea maxLength={2000} value={note} onChange={e=>setNote(e.target.value)}/></label>
      <div className="configuration-actions"><button className="button primary" disabled={busy}>{form==="block"?"차단 저장":"예약 저장"}</button><button type="button" className="button" disabled={busy} onClick={()=>setForm(null)}>닫기</button></div>
    </form>}
    <h3>당일 예약</h3>{!data?<p role="status">예약을 불러오고 있습니다.</p>:<>
      {data.items.length===0?<p>예약이 없습니다.</p>:<div className="booking-list">{data.items.map(b=><article className="booking-item" key={b.id}>
        <h4>{b.customerName}</h4><p>{localValue(new Date(b.startAt),timezone).slice(11)}–{localValue(new Date(b.endAt),timezone).slice(11)} · {b.staffName}</p><p>{b.customerPhone} · <strong>{statuses[b.status]}</strong></p>
        {b.note && <p>{b.note}</p>}{canWrite && <div className="configuration-actions">
          {["PENDING","CONFIRMED"].includes(b.status) && <><button className="button" disabled={busy} onClick={()=>edit(b)}>변경</button><button className="button" disabled={busy} onClick={()=>void action(`/bookings/${b.id}/cancel`)}>예약 취소</button></>}
          {b.status==="CONFIRMED" && <><button className="button" disabled={busy} onClick={()=>void action(`/bookings/${b.id}/complete`)}>완료 처리</button><button className="button" disabled={busy} onClick={()=>void action(`/bookings/${b.id}/no-show`)}>노쇼 처리</button></>}
          {b.status==="PENDING" && <button className="button" disabled={busy} onClick={()=>void action(`/bookings/${b.id}/confirm`)}>예약 확정</button>}
        </div>}
      </article>)}</div>}
      <div className="configuration-actions"><button className="button" disabled={page===0} onClick={()=>setPage(page-1)}>예약 이전 페이지</button><span>{page+1} / {Math.max(1,data.totalPages)}</span><button className="button" disabled={page+1>=data.totalPages} onClick={()=>setPage(page+1)}>예약 다음 페이지</button></div>
    </>}
    {canWrite && <><h3>차단된 시간</h3>{blocks?.items.length===0 && <p>차단이 없습니다.</p>}{blocks?.items.map(b=><article className="booking-item" key={b.id}><p>{localValue(new Date(b.startAt),timezone)}–{localValue(new Date(b.endAt),timezone)} · {b.scopeType==="STUDIO"?"사업장 전체":staff.find(s=>s.id===b.staffId)?.name??"담당자"}</p><p>{b.reason}</p><button className="button" disabled={busy} onClick={()=>void action(`/booking-blocks/${b.id}`,"DELETE")}>차단 해제</button></article>)}
      <div className="configuration-actions"><button className="button" disabled={blockPage===0} onClick={()=>setBlockPage(blockPage-1)}>차단 이전 페이지</button><button className="button" disabled={!blocks || blockPage+1>=blocks.totalPages} onClick={()=>setBlockPage(blockPage+1)}>차단 다음 페이지</button></div></>}
  </section>;
}
