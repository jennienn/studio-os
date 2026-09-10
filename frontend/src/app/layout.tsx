import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = {
  title: "Studio OS Prototype",
  description: "Small studio operations SaaS prototype",
};
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body>
        {children}
      </body>
    </html>
  );
}
