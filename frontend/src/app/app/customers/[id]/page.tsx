import { Workspace } from "@/auth/Workspace";
export default async function CustomerPage({params}:{params:Promise<{id:string}>}) {
    const {id}=await params;return <Workspace mode="customers" resourceId={id}/>;
}
