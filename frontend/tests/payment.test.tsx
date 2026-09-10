import { vi,it,expect,afterEach } from "vitest";
import {render,screen,fireEvent,waitFor} from "@testing-library/react";
import { PaymentPanel } from "@/operations/PaymentPanel";
import {api} from "@/auth/api";
const router=vi.hoisted(()=>({replace:vi.fn(),push:vi.fn()}));vi.mock("next/navigation",()=>({useRouter:()=>router}));
vi.mock("@/auth/api",async original=>({...await original<object>(),api:vi.fn()}));afterEach(()=>vi.clearAllMocks());
const payment={id:"p",customerName:"Customer",amount:"10000",method:"CARD",status:"PAID",paidAt:"2026-01-01T00:00:00Z",refund:null};
it("creates a manual payment using exact decimal strings and an idempotency key",async()=>{
  vi.mocked(api).mockResolvedValueOnce({items:[{id:"c",name:"Customer",phone:"01012345678"}],totalPages:1}).mockResolvedValueOnce(payment);
  render(<PaymentPanel studioId="s" category="LESSON" role="OWNER" id="new"/>);
  await screen.findByRole("option",{name:"Customer · 01012345678"});fireEvent.change(screen.getByLabelText("회원 선택"),{target:{value:"c"}});
  fireEvent.change(screen.getByLabelText("금액 (원)"),{target:{value:"10000"}});fireEvent.click(screen.getByRole("button",{name:"결제 기록 저장"}));
  await waitFor(()=>expect(api).toHaveBeenLastCalledWith("/studios/s/payments",expect.objectContaining({amount:"10000",customerId:"c"}),"POST",expect.any(String)));
});
it("records a full refund and renders history instead of another refund button",async()=>{
  vi.mocked(api).mockResolvedValueOnce(payment).mockResolvedValueOnce({...payment,status:"REFUNDED",refund:{amount:"10000",reason:"Requested",refundedAt:"2026-01-02T00:00:00Z"}});
  render(<PaymentPanel studioId="s" category="BEAUTY" role="OWNER" id="p"/>);await screen.findByText("수납 완료");
  fireEvent.change(screen.getByLabelText("환불 사유"),{target:{value:"Requested"}});fireEvent.click(screen.getByRole("button",{name:"전액 환불 기록"}));
  await screen.findByText("환불 완료");expect(screen.queryByRole("button",{name:"전액 환불 기록"})).not.toBeInTheDocument();expect(screen.getByText("환불 이력")).toBeInTheDocument();
});
it("manager cannot refund and a paid payment cannot be cancelled",async()=>{
  vi.mocked(api).mockResolvedValue(payment);render(<PaymentPanel studioId="s" category="LESSON" role="MANAGER" id="p"/>);
  await screen.findByText("수납 완료");expect(screen.queryByRole("button",{name:"전액 환불 기록"})).not.toBeInTheDocument();expect(screen.queryByRole("button",{name:"미결제 취소"})).not.toBeInTheDocument();
});
