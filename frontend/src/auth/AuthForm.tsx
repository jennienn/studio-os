"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useRef, useState } from "react";
import { api } from "./api";
type Mode = "login" | "signup" | "pending" | "verify" | "forgot" | "reset";
const titles: Record<Mode,string> = {
  login:"로그인", signup:"계정 만들기", pending:"이메일 인증을 확인해 주세요",
  verify:"이메일 인증", forgot:"비밀번호 찾기", reset:"새 비밀번호 설정",
};
export function AuthForm({ mode }: { mode: Mode }) {
  const router=useRouter();
  const [email,setEmail]=useState(""); const [password,setPassword]=useState(""); const [name,setName]=useState("");
  const [token,setToken]=useState(""); const [message,setMessage]=useState("");
  const [error,setError]=useState(""); const [busy,setBusy]=useState(false); const [done,setDone]=useState(false);
  const [providers,setProviders]=useState({google:false,kakao:false});
  const linkToken=useRef<string|null>(null);
  useEffect(() => {
    if (mode==="verify" || mode==="reset") {
      if (linkToken.current===null) linkToken.current=new URLSearchParams(window.location.hash.slice(1)).get("token") ?? "";
      const value=linkToken.current;
      setToken(value);
      window.history.replaceState(null,"",window.location.pathname);
      if (!value) setError("인증 링크가 없습니다. 새 링크를 요청해 주세요.");
    }
    if (mode==="login" || mode==="signup") {
      api<typeof providers>("/auth/providers").then(setProviders).catch(() => {});
      const query=new URLSearchParams(window.location.search);
      if (query.has("expired")) setMessage("로그인이 필요하거나 세션이 만료되었습니다. 다시 로그인해 주세요.");
      if (query.has("error")) setError(query.get("error")==="profile-required"
        ? "로그인 제공자의 이름 정보 제공에 동의한 후 다시 시도해 주세요." : "외부 로그인을 완료하지 못했습니다. 다시 시도해 주세요.");
    }
  },[mode]);
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(""); setMessage("");
    try {
      if (mode==="login") { await api("/auth/login",{email,password}); setPassword(""); router.replace("/app"); }
      if (mode==="signup") { await api("/auth/signup",{email,name,password}); setPassword(""); router.replace("/verification-pending"); }
      if (mode==="pending" || mode==="forgot") {
        const result=await api<{message:string}>(mode==="pending" ? "/auth/email-verification/request" : "/auth/password-reset/request",{email});
        setMessage(result.message);
      }
      if (mode==="verify" || mode==="reset") {
        const result=await api<{message:string}>(mode==="verify" ? "/auth/email-verification/confirm" : "/auth/password-reset/confirm",
          mode==="verify" ? {token} : {token,password});
        setToken(""); setPassword(""); setMessage(result.message); setDone(true);
      }
    } catch (e) { setError(e instanceof Error ? e.message : "요청을 처리하지 못했습니다."); }
    finally { setBusy(false); }
  }
  return <main className="entry-page"><section className="entry-card auth-card">
    <Link href="/" className="brand">Studio OS</Link>
    <h1>{titles[mode]}</h1>
    {mode==="signup" && <p>이메일 인증 후 사업장을 만들 수 있습니다.</p>}
    {mode==="pending" && <p>메일의 인증 링크를 열어 주세요. 메일이 오지 않았다면 이메일을 입력해 다시 요청할 수 있습니다.</p>}
    {message && <p role="status">{message}</p>}
    {error && <p role="alert" className="auth-error">{error}</p>}
    {!done && <form onSubmit={submit} className="auth-form">
      {mode==="signup" && <label>이름<input required maxLength={100} autoComplete="name" value={name} onChange={e=>setName(e.target.value)}/></label>}
      {["login","signup","pending","forgot"].includes(mode) && <label>이메일<input type="email" required maxLength={254} autoComplete="email" value={email} onChange={e=>setEmail(e.target.value)}/></label>}
      {["login","signup","reset"].includes(mode) && <label>비밀번호<input type="password" required minLength={mode==="login" ? undefined : 12}
        autoComplete={mode==="login" ? "current-password" : "new-password"} value={password} onChange={e=>setPassword(e.target.value)}/></label>}
      {(mode==="signup" || mode==="reset") && <small>12자 이상, UTF-8 72바이트 이하로 입력해 주세요.</small>}
      <button className="button primary" disabled={busy || (["verify","reset"].includes(mode) && !token)}>
        {busy ? "처리 중…" : mode==="pending" ? "인증 메일 다시 받기" : mode==="forgot" ? "재설정 링크 받기" : titles[mode]}
      </button>
    </form>}
    {(mode==="login" || mode==="signup") && <div className="auth-social">
      <button type="button" className="button" disabled={!providers.google} onClick={()=>window.location.assign("/oauth2/authorization/google")}>Google로 로그인</button>
      <button type="button" className="button kakao-button" disabled={!providers.kakao} onClick={()=>window.location.assign("/oauth2/authorization/kakao")}>카카오로 로그인</button>
      {(!providers.google || !providers.kakao) && <small>비활성화된 로그인 방식은 현재 준비 중입니다.</small>}
    </div>}
    <nav className="auth-links">
      {mode!=="login" && <Link href="/login">로그인으로 돌아가기</Link>}
      {mode==="login" && <><Link href="/signup">계정 만들기</Link><Link href="/forgot-password">비밀번호 찾기</Link><Link href="/verification-pending">이메일 인증 안내</Link></>}
      {mode==="verify" && <Link href="/verification-pending">인증 메일 다시 받기</Link>}
      {mode==="reset" && <Link href="/forgot-password">재설정 링크 다시 받기</Link>}
    </nav>
  </section></main>;
}
