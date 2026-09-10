"use client";
import { useState } from "react";
import { AppShell } from "@/demo/components/AppShell";
import { PageHeader } from "@/components/PageHeader";
import { configValid, stepLabels } from "@/demo/lib/config";
import { ConfigFields } from "@/demo/components/ConfigFields";
import { useStudio } from "@/demo/lib/store";
export default function Page() {
  const { data, setData, ready } = useStudio();
  return (
    <AppShell>
      {ready && (
        <Settings
          data={data}
          save={(c) => setData((d) => ({ ...d, config: c }))}
        />
      )}
    </AppShell>
  );
}
function Settings({
  data,
  save,
}: {
  data: ReturnType<typeof useStudio>["data"];
  save: (c: typeof data.config) => void;
}) {
  const [c, set] = useState(data.config);
  const steps = stepLabels(c);
  const [saved, done] = useState(false);
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        save(c);
        done(true);
      }}
    >
      <PageHeader
        title="사업장 설정"
        description="사업장이 운영되는 방식을 직접 정하세요."
        action={
          <button className="button primary" disabled={!configValid(c)}>
            설정 저장
          </button>
        }
      />
      {saved && <p role="status">설정을 저장했습니다.</p>}
      <div className="settings-layout">
        {steps.slice(0, 6).map((s, i) => (
          <section className="panel settings-section" key={s}>
            <div>
              <h2>{s}</h2>
              <p>변경한 설정은 저장 후 적용됩니다.</p>
            </div>
            <ConfigFields step={i + 1} c={c} set={set} />
          </section>
        ))}
        <section className="panel settings-section">
          <h2>직원 / 역할</h2>
          <div>
            <strong>데모 운영자 · 관리자</strong>
            <p className="muted">
              직원 초대와 권한 적용은 정식 서비스에서 제공 예정입니다.
            </p>
          </div>
        </section>
      </div>
    </form>
  );
}
