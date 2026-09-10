"use client";
import { ConfigurationView, Draft, Capability, capabilityLabels, typeLabels, weekdays } from "./model";
type Props={view:ConfigurationView;draft:Draft;set:(draft:Draft)=>void;section:"type"|"capabilities"|"hours"|"policies";onboarding?:boolean};
export function ConfigurationFields({view,draft,set,section,onboarding=false}:Props) {
  const category=draft.businessCategory;
  if (!category) return null;
  const branch=view.catalog[category];
  const canIdentity=onboarding || view.permissions.editIdentity;
  const canCapabilities=onboarding || view.permissions.editCapabilities;
  const canPolicies=onboarding || view.permissions.editPolicies;
  function flag(key:Capability,enabled:boolean) {
    const next={...draft,capabilities:{...draft.capabilities,[key]:enabled}};
    if (key==="DEPOSIT" && next.beautyPolicy) next.beautyPolicy={...next.beautyPolicy,depositEnabled:enabled};
    set(next);
  }
  function booleanSelect(label:string,value:boolean|null,onChange:(value:boolean)=>void,disabled=false) {
    return <label>{label}<select aria-label={label} required value={value===null ? "" : String(value)} disabled={disabled} onChange={e=>onChange(e.target.value==="true")}>
      <option value="" disabled>선택해 주세요</option><option value="true">사용</option><option value="false">사용 안 함</option>
    </select></label>;
  }
  return <div className="configuration-fields">
    {section==="type" && <fieldset disabled={!canIdentity}><legend>세부 업종</legend>
      <div className="select-card-grid two">{branch?.businessTypes.map(type=><button key={type} type="button" aria-pressed={draft.businessType===type}
        className={"select-card"+(draft.businessType===type ? " selected" : "")} onClick={()=>set({...draft,businessType:type})}>{typeLabels[type]}</button>)}</div>
    </fieldset>}
    {section==="capabilities" && <fieldset disabled={!canCapabilities}><legend>{category==="LESSON" ? "레슨 운영 방식" : "예약 서비스 운영 방식"}</legend>
      {branch?.capabilities.map(capability=><label className="check-line" key={capability}><input type="checkbox"
        disabled={capability==="PASS_MANAGEMENT" || (capability==="DEPOSIT" && !onboarding && !view.permissions.editDeposit)}
        checked={draft.capabilities[capability]===true} onChange={e=>flag(capability,e.target.checked)}/>{capabilityLabels[capability]}</label>)}
      {category==="LESSON" && <p>이용권 관리는 필수입니다. 개인 레슨과 그룹 수업 중 하나 이상을 선택해 주세요. 출석 관리는 그룹 수업을 사용할 때 선택할 수 있습니다.</p>}
      <p className="muted">사용할 기능의 설정만 저장합니다. 실제 업무 기능은 아직 제공되지 않습니다.</p>
    </fieldset>}
    {section==="hours" && <fieldset disabled={!canPolicies}><legend>영업시간</legend>
      <p>사업장 시간대 기준입니다. 영업하는 요일의 휴무를 해제하고 시간을 입력해 주세요.</p>
      {draft.businessHours.map((day,index)=><div className="hours-row" key={day.weekday}>
        <strong>{weekdays[day.weekday-1]}</strong>
        <label className="check-line"><input type="checkbox" checked={day.closed} aria-label={weekdays[day.weekday-1]+" 휴무"}
          onChange={e=>set({...draft,businessHours:draft.businessHours.map((h,i)=>i===index ? {...h,closed:e.target.checked,openTime:null,closeTime:null} : h)})}/>휴무</label>
        {!day.closed && <div className="hours-times">
          <label>시작<input type="time" required step={60} aria-label={weekdays[day.weekday-1]+" 시작"} value={day.openTime ?? ""}
            onChange={e=>set({...draft,businessHours:draft.businessHours.map((h,i)=>i===index ? {...h,openTime:e.target.value} : h)})}/></label>
          <label>종료<input type="time" required step={60} aria-label={weekdays[day.weekday-1]+" 종료"} value={day.closeTime ?? ""}
            onChange={e=>set({...draft,businessHours:draft.businessHours.map((h,i)=>i===index ? {...h,closeTime:e.target.value} : h)})}/></label>
        </div>}
      </div>)}
    </fieldset>}
    {section==="policies" && <fieldset disabled={!canPolicies}><legend>예약 정책</legend>
      <label>예약 시간 간격 (분)<input type="number" required min={1} max={1440} value={draft.bookingPolicy.slotIntervalMinutes}
        onChange={e=>set({...draft,bookingPolicy:{...draft.bookingPolicy,slotIntervalMinutes:Number(e.target.value)}})}/></label>
      <label>예약 가능 기간 (일)<input type="number" required min={1} max={2147483647} value={draft.bookingPolicy.bookingWindowDays}
        onChange={e=>set({...draft,bookingPolicy:{...draft.bookingPolicy,bookingWindowDays:Number(e.target.value)}})}/></label>
      <label>취소 마감 (시간 전)<input type="number" required min={0} max={2147483647} value={draft.bookingPolicy.cancellationCutoffHours}
        onChange={e=>set({...draft,bookingPolicy:{...draft.bookingPolicy,cancellationCutoffHours:Number(e.target.value)}})}/></label>
    </fieldset>}
    {section==="policies" && category==="LESSON" && draft.lessonPolicy && <fieldset disabled={!canPolicies}><legend>레슨 알림·취소 정책</legend>
      <label>잔여 이용권 알림 기준 (회 이하)<input type="number" required min={0} max={2147483647} value={draft.lessonPolicy.lowBalanceThreshold}
        onChange={e=>set({...draft,lessonPolicy:{...draft.lessonPolicy!,lowBalanceThreshold:e.target.value==="" ? "" : Number(e.target.value)}})}/></label>
      <label>만료 알림 기준 (일 전)<input type="number" required min={0} max={2147483647} value={draft.lessonPolicy.expiryAlertDays}
        onChange={e=>set({...draft,lessonPolicy:{...draft.lessonPolicy!,expiryAlertDays:e.target.value==="" ? "" : Number(e.target.value)}})}/></label>
      {booleanSelect("기한 내 취소 시 이용권 복구",draft.lessonPolicy.restoreOnTimelyCancellation,
        value=>set({...draft,lessonPolicy:{...draft.lessonPolicy!,restoreOnTimelyCancellation:value}}))}
    </fieldset>}
    {section==="policies" && category==="BEAUTY" && draft.beautyPolicy && <fieldset disabled={!canPolicies}><legend>뷰티 운영 정책</legend>
      <p>예약금: {draft.beautyPolicy.depositEnabled ? "사용" : "사용 안 함"}</p>
      {booleanSelect("노쇼 상태 관리",draft.beautyPolicy.noShowEnabled,value=>set({...draft,beautyPolicy:{...draft.beautyPolicy!,noShowEnabled:value}}))}
    </fieldset>}
  </div>;
}
