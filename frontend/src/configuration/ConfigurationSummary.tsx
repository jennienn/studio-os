import { Draft, capabilityLabels, categoryLabels, typeLabels, weekdays, Capability } from "./model";
export function ConfigurationSummary({draft}:{draft:Draft}) {
  if (!draft.businessCategory) return null;
  return <section className="configuration-summary"><h2>설정 확인</h2><dl>
    <dt>사업장 유형</dt><dd>{categoryLabels[draft.businessCategory]}</dd>
    <dt>세부 업종</dt><dd>{typeLabels[draft.businessType]}</dd>
    <dt>사용 기능</dt><dd>{Object.entries(draft.capabilities).filter(([,on])=>on).map(([key])=>capabilityLabels[key as Capability]).join(", ") || "선택 없음"}</dd>
    <dt>영업시간</dt><dd>{draft.businessHours.map(h=><div key={h.weekday}>{weekdays[h.weekday-1]}: {h.closed ? "휴무" : h.openTime+"–"+h.closeTime}</div>)}</dd>
    <dt>예약 정책</dt><dd>{draft.bookingPolicy.slotIntervalMinutes}분 간격 · {draft.bookingPolicy.bookingWindowDays}일 전부터 · 취소 {draft.bookingPolicy.cancellationCutoffHours}시간 전</dd>
    {draft.businessCategory==="LESSON" && draft.lessonPolicy && <>
      <dt>이용권 알림</dt><dd>잔여 {draft.lessonPolicy.lowBalanceThreshold}회 이하 · 만료 {draft.lessonPolicy.expiryAlertDays}일 전</dd>
      <dt>기한 내 취소 시 이용권 복구</dt><dd>{draft.lessonPolicy.restoreOnTimelyCancellation ? "사용" : "사용 안 함"}</dd>
    </>}
    {draft.businessCategory==="BEAUTY" && draft.beautyPolicy && <>
      <dt>예약금</dt><dd>{draft.beautyPolicy.depositEnabled ? "사용" : "사용 안 함"}</dd>
      <dt>노쇼 상태 관리</dt><dd>{draft.beautyPolicy.noShowEnabled ? "사용" : "사용 안 함"}</dd>
    </>}
  </dl></section>;
}
