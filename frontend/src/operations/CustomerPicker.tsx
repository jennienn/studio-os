"use client";
import { useEffect,useState } from "react";
import { api } from "@/auth/api";
import { Customer,PageResult } from "./CustomerPanel";
export function CustomerPicker({studioId,label,value,onChange}:{studioId:string;label:string;value:string;onChange:(id:string)=>void}){
  const [search,setSearch]=useState(""),[page,setPage]=useState(0),[data,setData]=useState<PageResult<Customer>|null>(null),[error,setError]=useState("");
  const [selectedName,setSelectedName]=useState("");
  useEffect(()=>{let live=true;setError("");api<PageResult<Customer>>(`/studios/${studioId}/customers?search=${encodeURIComponent(search)}&page=${page}&size=20`).then(r=>{if(live)setData(r);}).catch(e=>{if(live)setError(e.message);});return()=>{live=false;};},[studioId,search,page]);
  return <fieldset><legend>{label} 선택</legend><label>{label} 검색<input value={search} maxLength={100} onChange={e=>{setSearch(e.target.value);setPage(0);}}/></label>
    {error && <p role="alert">{error}</p>}
    <label>{label} 선택<select aria-label={label+" 선택"} required value={value} onChange={e=>{onChange(e.target.value);setSelectedName(e.target.selectedOptions[0]?.text??"");}}>
      <option value="">선택해 주세요</option>{value && !data?.items.some(c=>c.id===value) && <option value={value}>{selectedName}</option>}
      {data?.items.map(c=><option key={c.id} value={c.id}>{c.name} · {c.phone}</option>)}
    </select></label>
    <div className="configuration-actions"><button type="button" className="button" disabled={page===0} onClick={()=>setPage(page-1)}>검색 이전</button><button type="button" className="button" disabled={!data || page+1>=data.totalPages} onClick={()=>setPage(page+1)}>검색 다음</button></div>
  </fieldset>;
}
