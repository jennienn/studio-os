import { Workspace } from "@/auth/Workspace";
export default async function UnavailablePage({params}:{params:Promise<{path:string[]}>}) {
  const {path}=await params;
  return <Workspace mode="guard" target={path[0]==="lesson"?"LESSON":path[0]==="beauty"?"BEAUTY":null}/>;
}
