"use client";
import {useEffect,useState} from "react";
import {api} from "@/auth/api";
import {PageResult} from "@/operations/CustomerPanel";
import {Cycle,Enrollment,triggers} from "./types";
export function CyclePicker({studioId,customerId,kind,classId,value,onChange}:{studioId:string;customerId:string;kind:string;classId?:string;value:string;onChange:(id:string)=>void}){
 const [cycles,setCycles]=useState<Cycle[]>([]),[page,setPage]=useState(0),[more,setMore]=useState(false),[error,setError]=useState("");
 useEffect(()=>{setPage(0);onChange("");},[customerId,kind,classId]); // Selection must not survive a different member/context.
 useEffect(()=>{let live=true;setCycles([]);if(!customerId)return;const base=`/studios/${studioId}/lesson`;
  api<PageResult<Enrollment>>(base+`/enrollments?customerId=${customerId}&page=${page}`).then(async r=>{if(live)setMore(page+1<r.totalPages);const lists=await Promise.all(r.items.filter(e=>e.status==="ACTIVE"&&e.kind===kind&&(kind!=="GROUP"||e.classId===classId)).map(e=>api<Cycle[]>(base+`/enrollments/${e.id}/cycles`)));if(live)setCycles(lists.flat().filter(c=>c.status==="ACTIVE"&&(c.available===null||c.available>0)));}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};
 },[studioId,customerId,kind,classId,page]);
 return <div>{error&&<p role="alert">{error}</p>}<label>사용할 이용권<select aria-label="사용할 이용권" required value={value} onChange={e=>onChange(e.target.value)}><option value="">활성 이용권 선택</option>{cycles.map(c=><option key={c.id} value={c.id}>{c.productName} · {c.available===null?"기간권":`예약 가능 ${c.available}회`} · {c.deductionTrigger?triggers[c.deductionTrigger]:"기간 내 이용"}</option>)}</select></label><p>선택한 수업 날짜의 유효기간과 최종 예약 가능 여부는 저장 시 확인합니다.</p><button className="button" type="button" disabled={page===0} onClick={()=>setPage(p=>p-1)}>수강 이전</button><button className="button" type="button" disabled={!more} onClick={()=>setPage(p=>p+1)}>수강 다음</button></div>;
}
