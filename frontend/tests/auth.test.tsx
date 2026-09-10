import { vi, describe, it, expect, beforeEach } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { AuthForm } from "@/auth/AuthForm";
import { Workspace } from "@/auth/Workspace";
import { api } from "@/auth/api";
import { StrictMode } from "react";
const { replace }=vi.hoisted(()=>({replace:vi.fn()}));
vi.mock("next/navigation",()=>({useRouter:()=>({replace})}));
beforeEach(()=> { vi.restoreAllMocks(); replace.mockReset(); window.history.replaceState(null,"","/login"); });
function response(body:unknown,status=200) { return new Response(JSON.stringify(body),{status,headers:{"Content-Type":"application/json"}}); }
describe("real operator auth",()=> {
  it("uses credentials and obtains a fresh CSRF token for a command",async()=> {
    const fetch=vi.spyOn(globalThis,"fetch").mockResolvedValueOnce(response({headerName:"X-CSRF-TOKEN",token:"csrf-test"})).mockResolvedValueOnce(response({message:"ok"}));
    await api("/auth/login",{email:"owner@example.test",password:"test-password"});
    expect(fetch).toHaveBeenNthCalledWith(2,"/api/v1/auth/login",expect.objectContaining({
      credentials:"include",method:"POST",headers:expect.objectContaining({"X-CSRF-TOKEN":"csrf-test"}),
    }));
  });
  it("shows real verification feedback for signup",async()=> {
    vi.spyOn(globalThis,"fetch").mockImplementation(async(url)=>String(url).endsWith("providers")
      ? response({google:false,kakao:false}) : String(url).endsWith("csrf")
        ? response({headerName:"X-CSRF-TOKEN",token:"csrf"}) : response({message:"sent"},202));
    render(<AuthForm mode="signup"/>);
    fireEvent.change(screen.getByLabelText("이름"),{target:{value:"운영자"}});
    fireEvent.change(screen.getByLabelText("이메일"),{target:{value:"owner@example.test"}});
    fireEvent.change(screen.getByLabelText("비밀번호"),{target:{value:"A long password!"}});
    fireEvent.click(screen.getByRole("button",{name:"계정 만들기"}));
    await waitFor(()=>expect(replace).toHaveBeenCalledWith("/verification-pending"));
  });
  it("redirects expired sessions without exposing demo state",async()=> {
    vi.spyOn(globalThis,"fetch").mockResolvedValue(response({code:"SESSION_REQUIRED",message:"다시 로그인해 주세요."},401));
    render(<Workspace/>);
    await waitFor(()=>expect(replace).toHaveBeenCalledWith("/login?expired=1"));
    expect(screen.queryByText("시술")).not.toBeInTheDocument();
    expect(screen.queryByText("출석")).not.toBeInTheDocument();
  });
  it("removes link tokens from the URL and requires explicit confirmation",async()=> {
    window.history.replaceState(null,"","/verify-email#token=secret");
    const fetch=vi.spyOn(globalThis,"fetch");
    render(<StrictMode><AuthForm mode="verify"/></StrictMode>);
    expect(window.location.hash).toBe("");
    expect(fetch).not.toHaveBeenCalled();
    expect(screen.getByRole("button",{name:"이메일 인증"})).toBeEnabled();
  });
});
