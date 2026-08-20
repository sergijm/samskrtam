import React, { useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Tree } from "primereact/tree";
import type { TreeNode } from "primereact/treenode";
import { useMe } from "../../hooks/useUser";
import { useSidebarStore } from "../../store/sidebarStore";

interface TreeNodeData {
  route?: string;
  status: "available" | "planned";
  isPlannedLeaf: boolean;
}

const Sidebar: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: user } = useMe();
  const { expandedKeys, setExpandedKeys } = useSidebarStore();

  const treeNodes: TreeNode[] = useMemo(() => {
    const items: TreeNode[] = [];

    items.push({
      key: "dashboard",
      label: t("nav.dashboard"),
      icon: "pi pi-home",
      data: { route: "/dashboard", status: "available" as const, isPlannedLeaf: false },
      leaf: true,
      className: "sidebar-tree-item-highlighted",
    });

    items.push({
      key: "dictionary",
      label: t("nav.dictionary"),
      icon: "pi pi-search",
      data: { route: "/dictionary", status: "available" as const, isPlannedLeaf: false },
      leaf: true,
    });

    items.push({
      key: "statistics",
      label: t("nav.statistics"),
      icon: "pi pi-chart-bar",
      data: { route: "/statistics", status: "available" as const, isPlannedLeaf: false },
      leaf: true,
    });

    items.push({
      key: "leaderboard",
      label: t("nav.leaderboard"),
      icon: "pi pi-sitemap",
      data: { route: "/leaderboard", status: "available" as const, isPlannedLeaf: false },
      leaf: true,
    });

    if (user?.roles.includes("ADMIN")) {
      items.push({
        key: "admin",
        label: t("nav.admin"),
        icon: "pi pi-shield",
        data: { route: "/admin/users", status: "available" as const, isPlannedLeaf: false },
        leaf: true,
      });
    }

    return items;
  }, [t, user]);

  const handleToggle = useCallback(
    (e: { value: Record<string, boolean> }) => setExpandedKeys(e.value),
    [setExpandedKeys],
  );

  const handleNodeClick = useCallback(
    (e: { node: TreeNode }) => {
      const d = e.node.data as TreeNodeData | undefined;
      if (d?.route && d.status === "available" && !d.isPlannedLeaf) {
        navigate(d.route);
      }
    },
    [navigate],
  );

  return (
    <div className="layout-sidebar">
      <Tree
        value={treeNodes}
        expandedKeys={expandedKeys}
        onToggle={handleToggle}
        onNodeClick={handleNodeClick}
        className="sidebar-tree w-full"
      />
    </div>
  );
};

export default Sidebar;