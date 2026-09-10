/** @type {import('next').NextConfig} */
const backend = process.env.BACKEND_BASE_URL || "http://127.0.0.1:8080";
const nextConfig = {
  async rewrites() {
    return ["/api/v1/:path*", "/oauth2/:path*", "/login/oauth2/:path*"].map(source => ({
      source, destination: backend + source,
    }));
  },
  async headers() {
    return ["/api/v1/:path*", "/login", "/signup", "/app", "/verify-email", "/reset-password"].map(source => ({
      source, headers: [{ key: "Cache-Control", value: "no-store" }, { key: "Referrer-Policy", value: "no-referrer" }],
    }));
  },
};
module.exports = nextConfig;
