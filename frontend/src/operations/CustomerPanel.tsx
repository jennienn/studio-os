"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState, FormEvent } from "react";
import { api, ApiError } from "@/auth/api";
export type Customer={id:string;name:string;phone:string;normalizedPhone:string;memo:string;status:"ACTIVE"|"ARCHIVED"};
export type PageResult<T>={items:T[];page:number;size:number;totalElements:number;totalPages:number};
export function CustomerPanel({studioId,category,id}:{studioId:string;category:string;id?:string}) {
  const router=useRouter(),label=category==="LESSON"?"회원":"고객";
  const base=`/studios/${studioId}/customers`;
  const [data,setData]=useState<PageResult<Customer>|null>(null),[customer,setCustomer]=useState<Customer|null>(null);
  const [name,setName]=useState(""),[phone,setPhone]=useState(""),[memo,setMemo]=useState("");
  const [input,setInput]=useState(""),[search,setSearch]=useState(""),[status,setStatus]=useState("ACTIVE"),[page,setPage]=useState(0);
  const [error,setError]=useState(""),[message,setMessage]=useState(""),[busy,setBusy]=useState(false),[revision,setRevision]=useState(0);
  useEffect(()=>{
    let live=true;setData(null);setCustomer(null);setError("");setMessage("");
    if(id==="new"){setName("");setPhone("");setMemo("");return;}
    const path=id?`${base}/${id}`:`${base}?search=${encodeURIComponent(search)}&status=${status}&page=${page}&size=20`;
    api<Customer|PageResult<Customer>>(path).then(result=>{
      if(!live)return;
      if("items" in result)setData(result);else{setCustomer(result);setName(result.name);setPhone(result.phone);setMemo(result.memo);}
    }).catch(e=>{if(live){setError(e.message);if(e instanceof ApiError && e.status===401)router.replace("/login?expired=1");}});
    return()=>{live=false;};
  },[base,id,search,status,page,revision,router]);
  async function save(e:FormEvent){
    e.preventDefault();setBusy(true);setError("");setMessage("");
    try {const saved=await api<Customer>(id==="new"?base:`${base}/${id}`,{name,phone,memo},id==="new"?"POST":"PUT");
      setCustomer(saved);setMessage("저장했습니다.");if(id==="new")router.push(`/app/customers/${saved.id}`);
    }catch(e){setError(e instanceof Error?e.message:"저장하지 못했습니다.");}finally{setBusy(false);}
  }
  async function archive(){
    setBusy(true);setError("");setMessage("");
    try{setCustomer(await api<Customer>(`${base}/${id}/archive`,{}));setMessage("보관했습니다. 기존 이력은 유지됩니다.");}
    catch(e){setError(e instanceof Error?e.message:"보관하지 못했습니다.");}finally{setBusy(false);}
  }
  return <section className="operations-panel"><h2>{label} {id==="new"?"등록":id?"상세":"관리"}</h2>
    {error && <p role="alert" className="auth-error">{error}</p>}{message && <p role="status">{message}</p>}
    {id ? <><Link href="/app/customers">{label} 목록</Link>
      {id!=="new" && !customer ? <p role="status">정보를 불러오고 있습니다.</p> : <form className="auth-form" onSubmit={save}>
        {customer?.status==="ARCHIVED" && <p>보관된 {label}입니다. 신규 예약·결제에 사용할 수 없습니다.</p>}
        <label>이름<input required maxLength={100} value={name} onChange={e=>setName(e.target.value)}/></label>
        <label>전화번호<input type="tel" required maxLength={32} value={phone} onChange={e=>setPhone(e.target.value)}/></label>
        <label>운영자 메모<textarea maxLength={2000} value={memo} onChange={e=>setMemo(e.target.value)}/></label>
        <small>메모는 내부 운영용입니다.</small>
        <div className="configuration-actions"><button className="button primary" disabled={busy}>{label} 저장</button>
          {customer?.status==="ACTIVE" && <button type="button" className="button" disabled={busy} onClick={archive}>{label} 보관</button>}</div>
      </form>}</> : <>
      <Link className="button primary" href="/app/customers/new">{label} 등록</Link>
      <form className="operations-filters" onSubmit={e=>{e.preventDefault();setPage(0);setSearch(input);setRevision(r=>r+1);}}>
        <label>이름·전화번호 검색<input value={input} maxLength={100} onChange={e=>setInput(e.target.value)}/></label>
        <button className="button">검색</button>
        <label>상태<select value={status} onChange={e=>{setStatus(e.target.value);setPage(0);}}><option value="ACTIVE">활성</option><option value="ARCHIVED">보관</option><option value="ALL">전체</option></select></label>
      </form>
      {!data ? <p role="status">목록을 불러오고 있습니다.</p> : <>
        {data.items.length===0?<p>검색 결과가 없습니다.</p>:<ul className="operations-list">{data.items.map(c=><li key={c.id}><Link href={`/app/customers/${c.id}`}>{c.name}</Link><span>{c.phone}</span><span>{c.status==="ACTIVE"?"활성":"보관"}</span></li>)}</ul>}
        <div className="configuration-actions"><button className="button" disabled={page===0} onClick={()=>setPage(page-1)}>이전 페이지</button><span>{page+1} / {Math.max(1,data.totalPages)}</span>
          <button className="button" disabled={page+1>=data.totalPages} onClick={()=>setPage(page+1)}>다음 페이지</button></div>
      </>}
    </>}
  </section>;
}
