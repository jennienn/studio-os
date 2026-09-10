import { Workspace } from "@/auth/Workspace";
export default async function PaymentPage({params}:{params:Promise<{id:string}>}){const {id}=await params;return <Workspace mode="payments" resourceId={id}/>;}
