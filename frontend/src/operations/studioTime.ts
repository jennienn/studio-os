function parts(date:Date,zone:string){return Object.fromEntries(new Intl.DateTimeFormat("en-CA",{timeZone:zone,year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit",hourCycle:"h23"}).formatToParts(date).map(p=>[p.type,p.value]));}
export function localValue(date:Date,zone:string){const p=parts(date,zone);return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`;}
export function localInstant(value:string,zone:string):string {
  const target=Date.parse(value+":00Z");if(!Number.isFinite(target))throw new Error("날짜와 시간을 입력해 주세요.");
  let guess=target;
  for(let i=0;i<4;i++){const wall=Date.parse(localValue(new Date(guess),zone)+":00Z");guess+=target-wall;}
  if(localValue(new Date(guess),zone)!==value)throw new Error("사업장 시간대에 존재하지 않는 시각입니다.");
  if([-3600000,3600000,-1800000,1800000].some(offset=>localValue(new Date(guess+offset),zone)===value))throw new Error("시간대 전환으로 중복되는 시각입니다. 다른 시간을 선택해 주세요.");
  return new Date(guess).toISOString();
}
export function addDays(date:string,days:number){const value=new Date(date+"T12:00:00Z");value.setUTCDate(value.getUTCDate()+days);return value.toISOString().slice(0,10);}
