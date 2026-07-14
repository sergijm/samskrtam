import { create } from "zustand";
import { persist } from "zustand/middleware";

interface SidebarState {
  collapsed: boolean;
  width: number;
  expandedKeys: Record<string, boolean>;
  toggle: () => void;
  setCollapsed: (collapsed: boolean) => void;
  setWidth: (width: number) => void;
  setExpandedKeys: (keys: Record<string, boolean>) => void;
}

const DEFAULT_WIDTH = 300;
const MIN_WIDTH = 200;
const MAX_WIDTH = 500;

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set, get) => ({
      collapsed: false,
      width: DEFAULT_WIDTH,
      expandedKeys: {} as Record<string, boolean>,
      toggle: () => set({ collapsed: !get().collapsed }),
      setCollapsed: (collapsed) => set({ collapsed }),
      setWidth: (width) =>
        set({ width: Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width)) }),
      setExpandedKeys: (expandedKeys) => set({ expandedKeys }),
    }),
    {
      name: "sidebar-storage",
    },
  ),
);
