"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect,useState,FormEvent } from "react";
import { api } from "@/auth/api";
import { PageResult } from "./CustomerPanel";
import { CustomerPicker } from "./CustomerPicker";
import { useCommand } from "./useCommand";
type Payment={id:string;customerName:string;amount:string;method:string;status:string;paidAt:string|null;createdAt:string;refund:{amount:string;reason:string;refundedAt:string}|null};
export const paymentStatus:Record<string,string>={PENDING:"미결제",PAID:"수납 완료",REFUNDED:"환불 완료",CANCELLED:"취소"};
const methods:Record<string,string>={CARD:"카드",CASH:"현금",TRANSFER:"계좌이체",OTHER:"기타"};
export function PaymentPanel({studioId,category,role,id}:{studioId:string;category:string;role:string;id?:string}){
  const base=`/studios/${studioId}/payments`,router=useRouter(),command=useCommand();
  const [data,setData]=useState<PageResult<Payment>|null>(null),[payment,setPayment]=useState<Payment|null>(null);
  const [customerId,setCustomerId]=useState(""),[amount,setAmount]=useState(""),[method,setMethod]=useState("CARD"),[status,setStatus]=useState("PAID"),[filter,setFilter]=useState("ALL"),[page,setPage]=useState(0);
  const [reason,setReason]=useState(""),[busy,setBusy]=useState(false),[error,setError]=useState(""),[message,setMessage]=useState("");
  useEffect(()=>{let live=true;setPayment(null);setData(null);setError("");if(id==="new")return;
    api<Payment|PageResult<Payment>>(id?`${base}/${id}`:`${base}?status=${filter}&page=${page}`).then(r=>{if(live){if("items" in r)setData(r);else setPayment(r);}}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};
  },[base,id,filter,page]);
  async function create(e:FormEvent){e.preventDefault();setBusy(true);setError("");try{const p=await command<Payment>(base,{customerId,amount,method,status,currency:"KRW",referenceType:"OTHER",referenceId:null,paidAt:null});router.push(`/app/payments/${p.id}`);}catch(e){setError(e instanceof Error?e.message:"저장 실패");}finally{setBusy(false);}}
  async function action(kind:string){setBusy(true);setError("");setMessage("");try{setPayment(await command<Payment>(`${base}/${id}/${kind}`,kind==="refund"?{reason}:{}));setMessage("처리했습니다.");}catch(e){setError(e instanceof Error?e.message:"처리 실패");}finally{setBusy(false);}}
  return <section className="operations-panel"><h2>결제 {id==="new"?"기록":id?"상세":"내역"}</h2><p>수동 결제 기록입니다. 실제 카드 승인이나 계좌 이체를 실행하지 않습니다.</p>
    {error && <p role="alert" className="auth-error">{error}</p>}{message && <p role="status">{message}</p>}
    {id && <Link href="/app/payments">결제 목록</Link>}
    {id==="new" ? <form className="auth-form" onSubmit={create}><CustomerPicker studioId={studioId} label={category==="LESSON"?"회원":"고객"} value={customerId} onChange={setCustomerId}/>
      <label>금액 (원)<input required inputMode="numeric" pattern="[0-9]+" maxLength={19} value={amount} onChange={e=>setAmount(e.target.value)}/></label>
      <label>결제 수단<select aria-label="결제 수단" value={method} onChange={e=>setMethod(e.target.value)}>{Object.entries(methods).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
      <label>기록 상태<select aria-label="기록 상태" value={status} onChange={e=>setStatus(e.target.value)}><option value="PAID">수납 완료</option><option value="PENDING">미결제</option></select></label>
      <p>수납 완료 기록의 수납 시각은 저장 시점입니다.</p><button className="button primary" disabled={busy}>결제 기록 저장</button></form> : id ? payment && <>
      <dl className="operations-detail"><dt>{category==="LESSON"?"회원":"고객"}</dt><dd>{payment.customerName}</dd><dt>금액</dt><dd>{BigInt(payment.amount).toLocaleString()}원</dd><dt>수단</dt><dd>{methods[payment.method]}</dd><dt>상태</dt><dd>{paymentStatus[payment.status]}</dd><dt>수납 시각</dt><dd>{payment.paidAt?new Date(payment.paidAt).toISOString():"미수납"}</dd></dl>
      {payment.status==="PENDING" && <div className="configuration-actions"><button className="button primary" disabled={busy} onClick={()=>void action("confirm")}>수납 확인</button><button className="button" disabled={busy} onClick={()=>void action("cancel")}>미결제 취소</button></div>}
      {payment.status==="PAID" && role==="OWNER" && <form className="auth-form" onSubmit={e=>{e.preventDefault();void action("refund");}}><label>환불 사유<textarea required maxLength={2000} value={reason} onChange={e=>setReason(e.target.value)}/></label><button className="button" disabled={busy}>전액 환불 기록</button></form>}
      {payment.refund && <section><h3>환불 이력</h3><p>{BigInt(payment.refund.amount).toLocaleString()}원 · {payment.refund.reason}</p><p>{payment.refund.refundedAt}</p></section>}
    </> : <><Link className="button primary" href="/app/payments/new">결제 기록</Link><label>결제 상태<select aria-label="결제 상태" value={filter} onChange={e=>{setFilter(e.target.value);setPage(0);}}><option value="ALL">전체</option>{Object.entries(paymentStatus).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
      {!data?<p role="status">내역을 불러오고 있습니다.</p>:<>{data.items.length===0?<p>결제 기록이 없습니다.</p>:<ul className="operations-list">{data.items.map(p=><li key={p.id}><Link href={`/app/payments/${p.id}`}>{p.customerName} · {BigInt(p.amount).toLocaleString()}원</Link><span>{paymentStatus[p.status]}</span></li>)}</ul>}
      <div className="configuration-actions"><button className="button" disabled={page===0} onClick={()=>setPage(page-1)}>이전 페이지</button><span>{page+1} / {Math.max(1,data.totalPages)}</span><button className="button" disabled={page+1>=data.totalPages} onClick={()=>setPage(page+1)}>다음 페이지</button></div></>}
    </>}
  </section>;
}
