"use client";
import { useRef } from "react";
import { api } from "@/auth/api";
export function useCommand(){
  const attempts=useRef(new Map<string,string>());
  return async function command<T>(path:string,body:unknown={},method:"POST"|"PUT"|"DELETE"="POST"):Promise<T>{
    const signature=JSON.stringify([path,method,body]);
    const key=attempts.current.get(signature)??crypto.randomUUID();attempts.current.set(signature,key);
    const result=await api<T>(path,body,method,key);attempts.current.delete(signature);return result;
  };
}
