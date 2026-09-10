"use client";
import {useEffect,useState,FormEvent} from "react";
import {api} from "@/auth/api";
import {PageResult} from "@/operations/CustomerPanel";
import {Product,triggers} from "./types";
const empty={name:"",productType:"COUNT_BASED",totalCount:"",validityDays:"",validityStartRule:"FIRST_USE",price:"",deductionTrigger:"LESSON_COMPLETED",billingPeriod:"",active:true};
export function ProductPanel({studioId}:{studioId:string}){
 const base=`/studios/${studioId}/lesson/pass-products`;
 const [data,setData]=useState<PageResult<Product>|null>(null),[page,setPage]=useState(0),[revision,setRevision]=useState(0);
 const [draft,setDraft]=useState(empty),[id,setId]=useState<string|null>(null),[open,setOpen]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState("");
 useEffect(()=>{let live=true;api<PageResult<Product>>(base+`?page=${page}`).then(d=>{if(live)setData(d);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,page,revision]);
 function edit(p:Product){setId(p.id);setDraft({name:p.name,productType:p.productType,totalCount:p.totalCount?.toString()??"",validityDays:p.validityDays?.toString()??"",validityStartRule:p.validityStartRule,price:p.price,deductionTrigger:p.deductionTrigger??"",billingPeriod:p.billingPeriod??"",active:p.active});setOpen(true);}
 async function save(e:FormEvent){e.preventDefault();setBusy(true);setError("");try{await api(base+(id?`/${id}`:""),{...draft,totalCount:draft.productType==="COUNT_BASED"?Number(draft.totalCount):null,validityDays:draft.validityDays?Number(draft.validityDays):null,deductionTrigger:draft.productType==="COUNT_BASED"?draft.deductionTrigger:null,billingPeriod:draft.billingPeriod||null},id?"PUT":"POST");setOpen(false);setRevision(r=>r+1);}catch(e){setError(e instanceof Error?e.message:"저장 실패");}finally{setBusy(false);}}
 return <section className="operations-panel"><h2>이용권</h2><p>상품을 변경해도 기존 구매 이력과 차감 규칙은 유지됩니다.</p>{error&&<p role="alert">{error}</p>}
 <button className="button primary" onClick={()=>{setId(null);setDraft(empty);setOpen(true);}}>이용권 만들기</button>
 {open&&<form className="auth-form" onSubmit={save}><label>상품명<input required maxLength={100} value={draft.name} onChange={e=>setDraft({...draft,name:e.target.value})}/></label>
 <label>상품 유형<select value={draft.productType} onChange={e=>setDraft({...draft,productType:e.target.value,validityStartRule:e.target.value==="TIME_BASED"?"PURCHASE_DATE":"FIRST_USE",billingPeriod:""})}><option value="COUNT_BASED">회차권</option><option value="TIME_BASED">기간권</option></select></label>
 {draft.productType==="COUNT_BASED"&&<><label>구매 회차<input type="number" min={1} max={2147483647} required value={draft.totalCount} onChange={e=>setDraft({...draft,totalCount:e.target.value})}/></label>
 <label>차감 시점<select value={draft.deductionTrigger} onChange={e=>setDraft({...draft,deductionTrigger:e.target.value})}>{Object.entries(triggers).map(([v,t])=><option key={v} value={v}>{t}</option>)}</select></label>
 <label>유효기간 시작<select value={draft.validityStartRule} onChange={e=>setDraft({...draft,validityStartRule:e.target.value})}><option value="FIRST_USE">첫 차감일</option><option value="PURCHASE_DATE">구매일</option></select></label></>}
 {draft.productType==="TIME_BASED"&&<label>기간 방식<select value={draft.billingPeriod} onChange={e=>setDraft({...draft,billingPeriod:e.target.value,validityDays:""})}><option value="">일수</option><option value="MONTH">한 달</option></select></label>}
 {!draft.billingPeriod&&<label>유효 일수{draft.productType==="COUNT_BASED"?" (선택)":""}<input type="number" min={1} max={2147483647} required={draft.productType==="TIME_BASED"} value={draft.validityDays} onChange={e=>setDraft({...draft,validityDays:e.target.value})}/></label>}
 <label>가격 (원)<input inputMode="numeric" pattern="[0-9]+" maxLength={19} required value={draft.price} onChange={e=>setDraft({...draft,price:e.target.value})}/></label>
 <label><input type="checkbox" checked={draft.active} onChange={e=>setDraft({...draft,active:e.target.checked})}/>판매 활성</label>
 <button className="button primary" disabled={busy}>상품 저장</button><button className="button" type="button" disabled={busy} onClick={()=>setOpen(false)}>닫기</button></form>}
 {!data?<p role="status">이용권을 불러오고 있습니다.</p>:<><ul className="operations-list">{data.items.map(p=><li key={p.id}><span>{p.name} · {p.totalCount===null?"기간권":`${p.totalCount}회`} · {BigInt(p.price).toLocaleString()}원 · {p.active?"판매 중":"비활성"}</span><button className="button" onClick={()=>edit(p)}>상품 수정</button></li>)}</ul>{data.items.length===0&&<p>이용권이 없습니다.</p>}
 <button className="button" disabled={page===0} onClick={()=>setPage(page-1)}>이전 페이지</button><button className="button" disabled={page+1>=data.totalPages} onClick={()=>setPage(page+1)}>다음 페이지</button></>}
 </section>;
}
