import { expect } from "@playwright/test";
import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
export async function mailLink(email:string,kind:string) {
  const directory=path.resolve("../backend/.local-mail/e2e");
  let url="";
  await expect.poll(async()=> {
    for (const file of await readdir(directory).catch(()=>[])) {
      const item=JSON.parse(await readFile(path.join(directory,file),"utf8"));
      if (item.to===email && item.kind===kind) url=item.url;
    }
    return url;
  }).not.toBe("");
  return url;
}
