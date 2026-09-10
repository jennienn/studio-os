export type Owned = {
  id: string;
  studio_id: string;
};
export type Config = {
  mode: "lesson" | "beauty";
  lessonModule: boolean;
  appointmentModule: boolean;
  attendanceEnabled: boolean;
  staffAssignment: boolean;
  services: string;
  duration: number;
  depositEnabled: boolean;
  depositAmount: number;
  treatmentHistory: boolean;
  photoHistory: boolean;
  revisitEnabled: boolean;
  revisitDays: number;
  noShowEnabled: boolean;
  name: string;
  business: string;
  memberSize: string;
  staffSize: string;
  models: string[];
  passTypes: string[];
  reservation: boolean;
  opens: number;
  closes: number;
  cancellation: number;
  late: string;
  attendanceBy: string;
  deduction: string;
  remaining: number;
  expiry: number;
  notifications: boolean;
  role: string;
};
export type Pass = Owned & {
  name: string;
  type: string;
  sessions: number;
  days: number;
  start: string;
  deduction: string;
  price: number;
};
export type Member = Owned & {
  name: string;
  phone: string;
  pass_id: string;
  used: number;
  paid: boolean;
  first: string;
  expires: string;
};
export type Lesson = Owned & {
  name: string;
  model: string;
  date: string;
  time: string;
  capacity: number;
  member_ids: string[];
  completed: boolean;
};
export type Attendance = Owned & {
  lesson_id: string;
  member_id: string;
};
export type Payment = Owned & {
  member_id: string;
  amount: number;
  date: string;
};
export type Renewal = Owned & {
  member_id: string;
  pass_id: string;
  start: string;
  end: string;
  used: number;
  total: number;
};
export type Appointment = Owned & {
  member_id: string;
  service: string;
  date: string;
  time: string;
  duration: number;
  staff: string;
  deposit: number;
  depositPaid: boolean;
  status: "예정" | "완료" | "노쇼";
  note: string;
  photo: string;
};
export type Data = {
  appointments: Appointment[];
  config: Config;
  passes: Pass[];
  members: Member[];
  lessons: Lesson[];
  attendance: Attendance[];
  payments: Payment[];
  renewals: Renewal[];
};
