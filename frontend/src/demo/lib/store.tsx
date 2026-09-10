"use client";
import { createContext, useContext, useEffect, useState } from "react";
import { Data } from "@/demo/types/studio";
import { normalizeDemoConfig } from "./config";
import { initial, studio_id } from "@/demo/mock/data";
const Context = createContext<{
  data: Data;
  setData: React.Dispatch<React.SetStateAction<Data>>;
  ready: boolean;
}>({ data: initial, setData: () => {}, ready: false });
export function StudioProvider({ children }: { children: React.ReactNode }) {
  const [data, setData] = useState(initial);
  const [ready, setReady] = useState(false);
  useEffect(() => {
    try {
      const saved = localStorage.getItem("studio-os-demo-v2");
      if (saved) {
        const parsed = JSON.parse(saved);
        if (parsed.config && Array.isArray(parsed.lessons))
          setData({
            ...initial,
            ...parsed,
            config: normalizeDemoConfig({ ...initial.config, ...parsed.config }),
            appointments: parsed.appointments || initial.appointments,
            renewals: parsed.renewals || [],
          });
      }
    } catch {}
    setReady(true);
  }, []);
  useEffect(() => {
    if (ready) localStorage.setItem("studio-os-demo-v2", JSON.stringify(data));
  }, [data, ready]);
  return (
    <Context.Provider value={{ data, setData, ready }}>
      {children}
    </Context.Provider>
  );
}
export const useStudio = () => useContext(Context);
export const owned = () => ({ id: crypto.randomUUID(), studio_id });
export function attend(data: Data, lessonId: string, ids: string[]): Data {
  const lesson = data.lessons.find((l) => l.id === lessonId);
  if (!lesson) return data;
  const fresh = ids.filter(
    (id) =>
      lesson.member_ids.includes(id) &&
      !data.attendance.some(
        (a) => a.lesson_id === lessonId && a.member_id === id,
      ),
  );
  return {
    ...data,
    attendance: [
      ...data.attendance,
      ...fresh.map((member_id) => ({
        ...owned(),
        lesson_id: lessonId,
        member_id,
      })),
    ],
    members: data.members.map((m) => {
      const p = data.passes.find((p) => p.id === m.pass_id);
      return fresh.includes(m.id) &&
        p?.deduction === "출석 시" &&
        p.sessions > m.used
        ? { ...m, used: m.used + 1 }
        : m;
    }),
  };
}
