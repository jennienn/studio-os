"use client";
import Link from "next/link";
import {useEffect,useState,FormEvent} from "react";
import {api} from "@/auth/api";
import {useCommand} from "@/operations/useCommand";
import {CustomerPicker} from "@/operations/CustomerPicker";
import {PageResult} from "@/operations/CustomerPanel";
import {Product,Enrollment,Cycle,ClassView,triggers,cycleStatus} from "./types";
export function EnrollmentPanel({studioId,customerId:fixedCustomer,role,groupEnabled}:{studioId:string;customerId?:string;role:string;groupEnabled:boolean}){
 const base=`/studios/${studioId}/lesson`,command=useCommand();
 const [customer,setCustomer]=useState(fixedCustomer??""),[items,setItems]=useState<Enrollment[]>([]),[products,setProducts]=useState<Product[]>([]),[classes,setClasses]=useState<ClassView[]>([]);
 const [kind,setKind]=useState("PRIVATE"),[classId,setClassId]=useState(""),[product,setProduct]=useState(""),[selected,setSelected]=useState(""),[cycles,setCycles]=useState<Cycle[]>([]),[method,setMethod]=useState("CASH");
 const [page,setPage]=useState(0),[productPage,setProductPage]=useState(0),[classPage,setClassPage]=useState(0),[revision,setRevision]=useState(0),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
 const [ledger,setLedger]=useState<{eventType:string;amount:number;reason:string;createdAt:string}[]>([]),[ledgerCycle,setLedgerCycle]=useState(""),[ledgerPage,setLedgerPage]=useState(0),[amount,setAmount]=useState(""),[reason,setReason]=useState("");
 useEffect(()=>{let live=true;if(!customer){setItems([]);return;}api<PageResult<Enrollment>>(base+`/enrollments?customerId=${customer}&page=${page}`).then(r=>{if(live)setItems(r.items);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,customer,page,revision]);
 useEffect(()=>{let live=true;api<PageResult<Product>>(base+`/pass-products?active=true&page=${productPage}`).then(r=>{if(live)setProducts(r.items);}).catch(e=>{if(live)setError(e.message);});if(groupEnabled)api<ClassView[]>(base+`/classes?page=${classPage}`).then(r=>{if(live)setClasses(r.filter(c=>c.active));}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,groupEnabled,productPage,classPage]);
 useEffect(()=>{let live=true;setCycles([]);if(selected)api<Cycle[]>(base+`/enrollments/${selected}/cycles`).then(r=>{if(live)setCycles(r);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,selected,revision]);
 useEffect(()=>{let live=true;setLedger([]);if(ledgerCycle)api<typeof ledger>(base+`/cycles/${ledgerCycle}/ledger?page=${ledgerPage}`).then(r=>{if(live)setLedger(r);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[base,ledgerCycle,ledgerPage,revision]);
 async function run(work:()=>Promise<void>){setBusy(true);setError("");setMessage("");try{await work();setRevision(r=>r+1);setMessage("처리했습니다.");}catch(e){setError(e instanceof Error?e.message:"처리 실패");}finally{setBusy(false);}}
 async function create(e:FormEvent){e.preventDefault();await run(async()=>{const r=await command<Enrollment>(base+"/enrollments",{customerId:customer,kind,classId:kind==="GROUP"?classId:null});setSelected(r.id);});}
 const selectedEnrollment=items.find(e=>e.id===selected);
 const compatibleProducts=products.filter(p=>p.productType==="TIME_BASED"||(selectedEnrollment?.kind==="PRIVATE"
  ?p.deductionTrigger!=="ATTENDANCE_PRESENT"
  :selectedEnrollment?.kind==="GROUP"?p.deductionTrigger!=="LESSON_COMPLETED":true));
 return <section className="operations-panel"><h2>수강 · 결제 / 재등록</h2>{error&&<p role="alert">{error}</p>}{message&&<p role="status">{message}</p>}
 {!fixedCustomer&&<CustomerPicker studioId={studioId} label="회원" value={customer} onChange={v=>{setCustomer(v);setSelected("");setProduct("");setPage(0);}}/>}
 <form className="auth-form" onSubmit={create}><label>수강 형태<select aria-label="수강 형태" value={kind} onChange={e=>setKind(e.target.value)}><option value="PRIVATE">개인 레슨</option>{groupEnabled&&<option value="GROUP">그룹 수업</option>}</select></label>
 {kind==="GROUP"&&<><label>등록할 반<select aria-label="등록할 반" required value={classId} onChange={e=>setClassId(e.target.value)}><option value="">반 선택</option>{classes.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label><div><button type="button" disabled={classPage===0} onClick={()=>setClassPage(p=>p-1)}>반 이전</button><button type="button" disabled={classes.length<20} onClick={()=>setClassPage(p=>p+1)}>반 다음</button></div></>}
 <button className="button" disabled={busy||!customer}>수강 추가</button></form>
 <label>수강 이력<select aria-label="수강 이력" value={selected} onChange={e=>{setSelected(e.target.value);setProduct("");}}><option value="">수강 선택</option>{items.map((e,i)=><option key={e.id} value={e.id}>{e.kind==="PRIVATE"?"개인":"그룹"} · {e.status==="ACTIVE"?"진행 중":"종료"} · {page*20+i+1}</option>)}</select></label>
 <button className="button" disabled={page===0} onClick={()=>setPage(p=>p-1)}>수강 이전</button><button className="button" disabled={items.length<20} onClick={()=>setPage(p=>p+1)}>수강 다음</button>
 {selected&&<><form className="auth-form" onSubmit={e=>{e.preventDefault();void run(async()=>{await command(base+`/enrollments/${selected}/renew`,{passProductId:product,method});});}}><h3>이용권 결제 확인</h3><p>실제 수납을 확인한 뒤 저장합니다. 기존 이력을 유지하며 잔여 회차는 자동 이월하지 않습니다.</p>
 <label>구매할 이용권<select aria-label="구매할 이용권" required value={product} onChange={e=>setProduct(e.target.value)}><option value="">이용권 선택</option>{compatibleProducts.map(p=><option key={p.id} value={p.id}>{p.name} · {BigInt(p.price).toLocaleString()}원</option>)}</select></label>
 <div><button type="button" disabled={productPage===0} onClick={()=>setProductPage(p=>p-1)}>상품 이전</button><button type="button" disabled={products.length<20} onClick={()=>setProductPage(p=>p+1)}>상품 다음</button></div>
 <label>수납 방법<select aria-label="수납 방법" value={method} onChange={e=>setMethod(e.target.value)}><option value="CASH">현금</option><option value="CARD">카드</option><option value="TRANSFER">계좌이체</option><option value="OTHER">기타</option></select></label>
 <button className="button primary" disabled={busy}>수납 확인 · 이용 시작</button></form>
 <button className="button" disabled={busy} onClick={()=>void run(async()=>{await command(base+`/enrollments/${selected}/end`);})}>수강 종료</button>
 <h3>구매·이용 이력</h3>{cycles.length===0&&<p>결제 확인된 이용권이 없습니다.</p>}{cycles.map(c=><article className="booking-item" key={c.id}><h4>{c.productName} · {cycleStatus[c.status]}</h4><p>구매일 {c.paymentDate} · {BigInt(c.purchasePrice).toLocaleString()}원</p><p>이용기간 {c.startDate??"첫 차감 전"} ~ {c.validEndDate??"미설정"}</p>
 <p>{c.balance===null?"기간권":`구매 ${c.purchasedCount}회 · 잔여 ${c.balance}회 · 예약 중 ${c.reserved}회 · 예약 가능 ${c.available}회`}</p><p>{c.deductionTrigger?`${triggers[c.deductionTrigger]} 시 차감`:"기간 내 이용"}</p>
 <Link href={`/app/payments/${c.paymentId}`}>결제 상세</Link><button className="button" onClick={()=>{setLedgerCycle(c.id);setLedgerPage(0);}}>사용 이력 보기</button></article>)}
 {ledgerCycle&&<section><h3>회차 변동 이력</h3><ul className="operations-list">{ledger.map((l,i)=><li key={i}>{l.amount>0?"+":""}{l.amount} · {({PURCHASE:"구매",BOOKING_DEDUCTION:"예약 확정",ATTENDANCE:"출석",LESSON_COMPLETED:"수업 완료",CANCEL_RESTORE:"취소 복구",MANUAL_ADJUSTMENT:"수동 조정"} as Record<string,string>)[l.eventType]} · {l.reason} · {l.createdAt}</li>)}</ul>
 <button className="button" disabled={ledgerPage===0} onClick={()=>setLedgerPage(p=>p-1)}>이력 이전</button><button className="button" disabled={ledger.length<20} onClick={()=>setLedgerPage(p=>p+1)}>이력 다음</button>
 {role==="OWNER"&&<form className="auth-form" onSubmit={e=>{e.preventDefault();void run(async()=>{await command(base+`/cycles/${ledgerCycle}/adjustments`,{amount:Number(amount),reason});});}}><label>회차 조정<input type="number" min={-2147483648} max={2147483647} required value={amount} onChange={e=>setAmount(e.target.value)}/></label><label>조정 사유<textarea required maxLength={2000} value={reason} onChange={e=>setReason(e.target.value)}/></label><button className="button" disabled={busy}>조정 기록</button></form>}</section>}
 </>}
 </section>;
}
