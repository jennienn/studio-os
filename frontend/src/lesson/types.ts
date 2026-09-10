export type Product={id:string;name:string;productType:string;totalCount:number|null;validityDays:number|null;validityStartRule:string;price:string;deductionTrigger:string|null;billingPeriod:string|null;active:boolean};
export type Enrollment={id:string;customerId:string;kind:string;classId:string|null;status:string};
export type Cycle={id:string;enrollmentId:string;productName:string;productType:string;purchasedCount:number|null;validityDays:number|null;purchasePrice:string;deductionTrigger:string|null;startDate:string|null;validEndDate:string|null;paymentDate:string;status:string;balance:number|null;reserved:number|null;available:number|null;paymentId:string};
export type ClassView={id:string;name:string;capacity:number;instructorStaffId:string|null;active:boolean};
export type Occurrence={id:string;classId:string;className:string;startAt:string;endAt:string;capacitySnapshot:number;status:string;bookedCount:number};
export const triggers:Record<string,string>={BOOKING_CONFIRMED:"예약 확정",LESSON_COMPLETED:"수업 완료",ATTENDANCE_PRESENT:"출석"};
export const cycleStatus:Record<string,string>={ACTIVE:"이용 중",SCHEDULED:"다음 이용 대기",COMPLETED:"사용 완료",EXPIRED:"만료",CANCELLED:"종료"};
