import { vi,it,expect,afterEach } from "vitest";
import { render,screen,fireEvent,waitFor } from "@testing-library/react";
import { CustomerPanel } from "@/operations/CustomerPanel";
import { api } from "@/auth/api";
const router=vi.hoisted(()=>({replace:vi.fn(),push:vi.fn()}));
vi.mock("next/navigation",()=>({useRouter:()=>router}));vi.mock("@/auth/api",async original=>({...await original<object>(),api:vi.fn()}));
afterEach(()=>vi.clearAllMocks());
const customer={id:"one",name:"First",phone:"01012345678",normalizedPhone:"01012345678",memo:"Memo",status:"ACTIVE"};
it.each([['LESSON','회원'],['BEAUTY','고객']])("uses %s terminology",async(category,label)=>{
  vi.mocked(api).mockResolvedValue({items:[customer],page:0,size:20,totalElements:1,totalPages:1});
  render(<CustomerPanel studioId="s" category={category}/>);
  expect(await screen.findByRole("heading",{name:label+" 관리"})).toBeInTheDocument();
  expect(await screen.findByText("First")).toBeInTheDocument();
});
it("creates through the API and opens detail",async()=>{
  vi.mocked(api).mockResolvedValue(customer);render(<CustomerPanel studioId="s" category="LESSON" id="new"/>);
  fireEvent.change(screen.getByLabelText("이름"),{target:{value:"First"}});fireEvent.change(screen.getByLabelText("전화번호"),{target:{value:"01012345678"}});
  fireEvent.click(screen.getByRole("button",{name:"회원 저장"}));
  await waitFor(()=>expect(api).toHaveBeenCalledWith('/studios/s/customers',expect.objectContaining({name:"First"}),"POST"));
  await waitFor(()=>expect(router.push).toHaveBeenCalledWith("/app/customers/one"));
});
it("edits and archives while preserving the detail",async()=>{
  vi.mocked(api).mockResolvedValueOnce(customer).mockResolvedValueOnce({...customer,name:"Edited"}).mockResolvedValueOnce({...customer,status:"ARCHIVED"});
  render(<CustomerPanel studioId="s" category="BEAUTY" id="one"/>);
  await screen.findByDisplayValue("First");fireEvent.change(screen.getByLabelText("이름"),{target:{value:"Edited"}});fireEvent.click(screen.getByRole("button",{name:"고객 저장"}));
  await screen.findByText("저장했습니다.");fireEvent.click(screen.getByRole("button",{name:"고객 보관"}));
  await screen.findByText("보관했습니다. 기존 이력은 유지됩니다.");expect(screen.queryByRole("button",{name:"고객 보관"})).not.toBeInTheDocument();
});
it("search resets pagination and preserves the archive filter",async()=>{
  vi.mocked(api).mockResolvedValue({items:[customer],page:0,size:20,totalElements:21,totalPages:2});
  render(<CustomerPanel studioId="s" category="LESSON"/>);await screen.findByText("First");fireEvent.click(screen.getByRole("button",{name:"다음 페이지"}));
  await waitFor(()=>expect(api).toHaveBeenLastCalledWith(expect.stringContaining("page=1")));
  fireEvent.change(screen.getByLabelText("이름·전화번호 검색"),{target:{value:"010-1234"}});fireEvent.click(screen.getByRole("button",{name:"검색"}));
  await waitFor(()=>expect(api).toHaveBeenLastCalledWith(expect.stringContaining("search=010-1234&status=ACTIVE&page=0")));
});
