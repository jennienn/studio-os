export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message); }
}
async function decode<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new ApiError(response.status, error.code ?? "REQUEST_FAILED", error.message ?? "요청을 처리하지 못했습니다.");
  }
  return response.status === 204 ? undefined as T : response.json();
}
export async function api<T>(path: string, body?: unknown, method?: "POST"|"PUT"): Promise<T> {
  const headers: Record<string, string> = {};
  if (body !== undefined) {
    const token = await decode<{headerName: string; token: string}>(await fetch("/api/v1/auth/csrf", { credentials: "include", cache: "no-store" }));
    headers[token.headerName] = token.token;
    headers["Content-Type"] = "application/json";
  }
  return decode<T>(await fetch("/api/v1" + path, {
    method: body === undefined ? "GET" : method ?? "POST", headers,
    credentials: "include", cache: "no-store",
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  }));
}
