import { describe, expect, it } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import Page from "@/app/page";
import { ProductPreview } from "@/components/ProductPreview";
import { ConfigFields } from "@/demo/components/ConfigFields";
import { initial } from "@/demo/mock/data";
import { normalizeDemoConfig, recommend } from "@/demo/lib/config";

describe("preserved visual preview", () => {
  it("renders the landing and its entry links", () => {
    render(<Page />);
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("고객에게 집중하세요.");
    expect(screen.getByRole("link", { name: /데모 둘러보기/ })).toHaveAttribute("href", "/dashboard");
  });
  it("keeps the two preview taxonomies separate when switching", () => {
    render(<ProductPreview />);
    expect(screen.getByText("개인 2 · 그룹 1")).toBeVisible();
    expect(screen.queryByText("젤 네일")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "뷰티 / 예약샵" }));
    expect(screen.getByText("젤 네일")).toBeVisible();
    expect(screen.queryByText("출석")).not.toBeInTheDocument();
    expect(screen.queryByText("개인 2 · 그룹 1")).not.toBeInTheDocument();
  });
  it.each(["lesson", "beauty"] as const)("limits %s demo subtypes and removes legacy mixed settings", (mode) => {
    const config = normalizeDemoConfig({ ...recommend(initial.config, mode), lessonModule: true, appointmentModule: true });
    expect(config.lessonModule).toBe(mode === "lesson");
    expect(config.appointmentModule).toBe(mode === "beauty");
    render(<ConfigFields step={1} c={config} set={() => {}} />);
    const options = within(screen.getByRole("combobox", { name: "업종" }));
    expect(options.queryByRole("option", { name: mode === "lesson" ? "네일" : "댄스" })).not.toBeInTheDocument();
  });
});
