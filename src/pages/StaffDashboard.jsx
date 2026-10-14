import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useUser } from "@/context/UserContext";
import { getStaffComplaintsPage, getAssignedComplaints } from "@/services/complaintApi";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import ComplaintRow from "@/components/complaints/ComplaintRow";
import Pagination from "@/components/ui/Pagination";

import {
  Layers,
  Activity,
  BadgeCheck,
  Clock,
  Search,
  ArrowRight,
  FileText,
  ShieldCheck,
  AlertCircle,
  AlertTriangle,
  ArrowUpDown,
  RefreshCw,
  Info,
} from "lucide-react";

import { motion } from "framer-motion";

export default function StaffDashboard() {
  const { user, loading: userLoading } = useUser();
  const staffId = user?.staff?.staffId;

  // Stream 1: Global summary statistics & badge counts from existing array endpoint
  const [summaryStats, setSummaryStats] = useState({
    total: 0,
    needsAttention: 0,
    inProgress: 0,
    resolved: 0,
    escalated: 0,
  });

  const [tabCounts, setTabCounts] = useState({
    ALL: 0,
    NEEDS_ATTENTION: 0,
    OPEN: 0,
    IN_PROGRESS: 0,
    ESCALATED: 0,
    RESOLVED: 0,
    CLOSED: 0,
  });

  // Stream 2: Server-side paginated complaints from Spring Page endpoint
  const [pageData, setPageData] = useState({
    content: [],
    totalElements: 0,
    totalPages: 1,
    number: 0,
    size: 10,
  });
  const [pageLoading, setPageLoading] = useState(true);
  const [pageError, setPageError] = useState(null);

  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [sortBy, setSortBy] = useState("URGENCY");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;
  const [mouse, setMouse] = useState({ x: 0, y: 0 });

  const navigate = useNavigate();

  // Map UI sort keys to backend whitelisted properties
  // Note: Compound urgency sort (status rank + priority rank) is mapped to priority desc in backend
  const getSortParams = (uiSort) => {
    switch (uiSort) {
      case "PRIORITY":
        return { sortBy: "priority", direction: "desc" };
      case "UPDATED":
        return { sortBy: "updatedAt", direction: "desc" };
      case "NEWEST":
        return { sortBy: "createdAt", direction: "desc" };
      case "OLDEST":
        return { sortBy: "createdAt", direction: "asc" };
      case "URGENCY":
      default:
        return { sortBy: "priority", direction: "desc" };
    }
  };

  // Fetch summary stream for overall stats & tab badges
  const loadSummaryData = useCallback(async () => {
    if (userLoading || !staffId) return;
    try {
      const data = await getAssignedComplaints();
      const list = Array.isArray(data) ? data : [];

      const openCount = list.filter((c) => c.status === "OPEN").length;
      const inProgressCount = list.filter((c) => c.status === "IN_PROGRESS").length;
      const escalatedCount = list.filter((c) => c.status === "ESCALATED").length;
      const resolvedCount = list.filter((c) => c.status === "RESOLVED").length;
      const closedCount = list.filter((c) => c.status === "CLOSED").length;
      const needsAttentionCount = list.filter((c) =>
        ["OPEN", "ESCALATED"].includes(c.status)
      ).length;

      setSummaryStats({
        total: list.length,
        needsAttention: needsAttentionCount,
        inProgress: inProgressCount,
        resolved: resolvedCount,
        escalated: escalatedCount,
      });

      setTabCounts({
        ALL: list.length,
        NEEDS_ATTENTION: needsAttentionCount,
        OPEN: openCount,
        IN_PROGRESS: inProgressCount,
        ESCALATED: escalatedCount,
        RESOLVED: resolvedCount,
        CLOSED: closedCount,
      });
    } catch (err) {
      console.error("Failed to load staff summary stats:", err);
    }
  }, [userLoading, staffId]);

  // Fetch paginated server stream
  const loadPageData = useCallback(
    async (pageToLoad = currentPage, sortOption = sortBy) => {
      if (userLoading || !staffId) return;

      try {
        setPageLoading(true);
        setPageError(null);
        const { sortBy: backendSortBy, direction } = getSortParams(sortOption);
        const data = await getStaffComplaintsPage(staffId, {
          page: pageToLoad - 1, // Convert 1-based UI to 0-based Spring Page
          size: pageSize,
          sortBy: backendSortBy,
          direction,
        });

        setPageData({
          content: Array.isArray(data?.content) ? data.content : [],
          totalElements: typeof data?.totalElements === "number" ? data.totalElements : 0,
          totalPages: Math.max(1, data?.totalPages || 1),
          number: data?.number || 0,
          size: data?.size || pageSize,
        });
      } catch (err) {
        console.error("Failed to load staff complaints page:", err);
        setPageError("Failed to load work queue. Please try again.");
      } finally {
        setPageLoading(false);
      }
    },
    [userLoading, staffId, currentPage, sortBy, pageSize]
  );

  // Load summary once user authentication is resolved
  useEffect(() => {
    if (!userLoading && staffId) {
      loadSummaryData();
    }
  }, [userLoading, staffId, loadSummaryData]);

  // Load paginated data on page or sort changes
  useEffect(() => {
    if (!userLoading && staffId) {
      loadPageData(currentPage, sortBy);
    }
  }, [userLoading, staffId, currentPage, sortBy, loadPageData]);

  // Safe page boundary clamping if totalPages shrinks
  useEffect(() => {
    if (pageData.totalPages > 0 && currentPage > pageData.totalPages) {
      setCurrentPage(pageData.totalPages);
    }
  }, [pageData.totalPages, currentPage]);

  // Handle Sort Change (resets to page 1)
  const handleSortChange = (newSort) => {
    setSortBy(newSort);
    setCurrentPage(1);
  };

  // Handle Page Change with smooth scroll
  const handlePageChange = (newPage) => {
    if (newPage < 1 || newPage > pageData.totalPages || newPage === currentPage) return;
    setCurrentPage(newPage);
    document
      .getElementById("work-queue-section")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  // Refresh both summary stream and server page
  const handleRefresh = () => {
    loadSummaryData();
    loadPageData(currentPage, sortBy);
  };

  // ── Stats ──────────────────────────────────────────────────────────────────
  const stats = {
    total: summaryStats.total,
    needsAttention: summaryStats.needsAttention,
    inProgress: summaryStats.inProgress,
    resolved: summaryStats.resolved,
    escalated: summaryStats.escalated,
  };

  // ── Filter tabs config ─────────────────────────────────────────────────────
  const filterTabs = [
    { key: "ALL", label: "All", color: "indigo", count: tabCounts.ALL },
    {
      key: "NEEDS_ATTENTION",
      label: "Needs Attention",
      color: "rose",
      count: tabCounts.NEEDS_ATTENTION,
    },
    {
      key: "OPEN",
      label: "Open",
      color: "amber",
      count: tabCounts.OPEN,
    },
    {
      key: "IN_PROGRESS",
      label: "In Progress",
      color: "cyan",
      count: tabCounts.IN_PROGRESS,
    },
    {
      key: "ESCALATED",
      label: "Escalated",
      color: "orange",
      count: tabCounts.ESCALATED,
    },
    {
      key: "RESOLVED",
      label: "Resolved",
      color: "emerald",
      count: tabCounts.RESOLVED,
    },
    {
      key: "CLOSED",
      label: "Closed",
      color: "slate",
      count: tabCounts.CLOSED,
    },
  ];

  const tabActiveClass = {
    indigo: "bg-indigo-500 text-white shadow-lg shadow-indigo-500/30",
    rose: "bg-rose-500 text-white shadow-lg shadow-rose-500/30",
    amber: "bg-amber-500 text-white shadow-lg shadow-amber-500/30",
    cyan: "bg-cyan-500 text-white shadow-lg shadow-cyan-500/30",
    orange: "bg-orange-500 text-white shadow-lg shadow-orange-500/30",
    emerald: "bg-emerald-500 text-white shadow-lg shadow-emerald-500/30",
    slate: "bg-slate-500 text-white shadow-lg shadow-slate-500/30",
  };

  // ── Client-side filtering applied to current page content ──────────────────
  const displayedComplaints = useMemo(() => {
    let list = pageData.content || [];

    // Status filter
    if (filter === "NEEDS_ATTENTION") {
      list = list.filter((c) => ["OPEN", "ESCALATED"].includes(c.status));
    } else if (filter !== "ALL") {
      list = list.filter((c) => c.status === filter);
    }

    // Search (title, complaintId, category, studentName, description)
    if (search.trim()) {
      const q = search.trim().toLowerCase();
      list = list.filter((c) => {
        const matchId =
          String(c.complaintId || "").toLowerCase().includes(q) ||
          `#${c.complaintId}`.toLowerCase().includes(q);
        const matchTitle = (c.title || "").toLowerCase().includes(q);
        const matchCategory = (c.category || "").toLowerCase().includes(q);
        const matchStudent = (c.studentName || "").toLowerCase().includes(q);
        const matchDesc = (c.description || "").toLowerCase().includes(q);
        return matchId || matchTitle || matchCategory || matchStudent || matchDesc;
      });
    }

    return list;
  }, [pageData.content, search, filter]);

  const isFilteringActive = filter !== "ALL" || search.trim().length > 0;

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Background layers */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />

      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Top strip */}
        <motion.div
          initial={{ opacity: 0, y: -18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl px-5 py-4 shadow-2xl"
        >
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-blue-500 shadow-lg shadow-indigo-500/30">
              <ShieldCheck className="h-5 w-5 text-white" />
            </div>

            <div>
              <h2 className="text-lg font-semibold tracking-wide text-white">
                SmartGriev
              </h2>
              <p className="text-xs text-slate-300">Staff grievance workspace</p>
            </div>
          </div>

          <div className="hidden md:flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300">
            <Activity className="h-4 w-4 text-emerald-400" />
            Live dashboard
          </div>
        </motion.div>

        {/* Hero section */}
        <motion.div
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          onMouseMove={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            setMouse({ x: e.clientX - rect.left, y: e.clientY - rect.top });
          }}
          className="relative mb-8 overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur-2xl shadow-[0_20px_80px_rgba(0,0,0,0.35)]"
        >
          <div
            className="pointer-events-none absolute h-72 w-72 rounded-full bg-indigo-400/20 blur-3xl transition-all duration-200"
            style={{ left: mouse.x - 144, top: mouse.y - 144 }}
          />

          <div className="relative grid grid-cols-1 gap-8 p-8 sm:p-10 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <h1 className="text-4xl font-black leading-tight tracking-tight sm:text-5xl xl:text-6xl">
                Welcome back,{" "}
                <span className="bg-gradient-to-r from-indigo-200 to-blue-300 bg-clip-text text-transparent">
                  {user?.staff?.name || "Staff"}
                </span>
              </h1>

              <p className="mt-5 max-w-2xl text-base leading-8 text-slate-300 sm:text-lg">
                Manage your assigned grievances, review live progress, and resolve
                complaints through a modern and intelligent staff workspace.
              </p>

              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <Button
                  onClick={handleRefresh}
                  className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
                >
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Refresh Queue
                </Button>

                <Button
                  variant="outline"
                  onClick={() =>
                    document
                      .getElementById("work-queue-section")
                      ?.scrollIntoView({ behavior: "smooth", block: "start" })
                  }
                  className="h-12 rounded-xl border-white/15 bg-white/5 text-white hover:bg-white/10"
                >
                  View Work Queue
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            </div>

            <div className="flex flex-col justify-between gap-4">
              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-slate-400">Quick Summary</p>
                <h3 className="text-3xl font-bold text-white">{stats.total}</h3>
                <p className="mt-1 text-sm text-slate-300">
                  Total assigned complaints
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-rose-500/15 to-orange-500/10 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-rose-300">Needs Attention</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.needsAttention}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Open + Escalated complaints
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-slate-400">In Progress</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.inProgress}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Complaints currently being handled
                </p>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Stats Row */}
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
          className="mb-8 grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-4"
        >
          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-indigo-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/10 via-transparent to-blue-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">Total Assigned</p>
                <h3 className="mt-3 text-3xl font-bold text-white">{stats.total}</h3>
                <p className="mt-2 text-xs text-slate-400">All complaints assigned</p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-blue-500 shadow-lg shadow-indigo-500/30">
                <Layers className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-rose-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-rose-500/10 via-transparent to-orange-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">Needs Attention</p>
                <h3 className="mt-3 text-3xl font-bold text-white">{stats.needsAttention}</h3>
                <p className="mt-2 text-xs text-slate-400">Open + Escalated</p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-400 via-orange-500 to-red-500 shadow-lg shadow-rose-500/30">
                <AlertTriangle className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-cyan-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/10 via-transparent to-blue-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">In Progress</p>
                <h3 className="mt-3 text-3xl font-bold text-white">{stats.inProgress}</h3>
                <p className="mt-2 text-xs text-slate-400">Currently under process</p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-400 via-sky-500 to-blue-500 shadow-lg shadow-cyan-500/30">
                <AlertCircle className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-emerald-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-emerald-500/10 via-transparent to-green-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">Resolved</p>
                <h3 className="mt-3 text-3xl font-bold text-white">{stats.resolved}</h3>
                <p className="mt-2 text-xs text-slate-400">Successfully resolved</p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-400 via-green-500 to-teal-500 shadow-lg shadow-emerald-500/30">
                <BadgeCheck className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        </motion.div>

        {/* Search bar */}
        <motion.div
          initial={{ opacity: 0, y: 22 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="mb-5 rounded-[1.75rem] border border-white/10 bg-white/8 p-5 shadow-xl backdrop-blur-2xl sm:p-6"
        >
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <Input
                placeholder="Search by title, ID, student, category..."
                className="h-12 rounded-2xl border-white/10 bg-slate-900/40 pl-11 text-white placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-indigo-500"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            <Button
              onClick={handleRefresh}
              className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
            >
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh
            </Button>
          </div>
        </motion.div>

        {/* Filter tabs */}
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.85 }}
          className="mb-8 flex flex-wrap gap-3"
        >
          {filterTabs.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setFilter(tab.key)}
              className={`rounded-xl px-4 py-2 text-sm font-medium transition flex items-center gap-1.5 ${
                filter === tab.key
                  ? tabActiveClass[tab.color]
                  : "bg-white/5 text-slate-300 hover:bg-white/10"
              }`}
            >
              {tab.label}
              <span
                className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
                  filter === tab.key
                    ? "bg-white/20 text-white"
                    : "bg-white/10 text-slate-400"
                }`}
              >
                {tab.count}
              </span>
            </button>
          ))}
        </motion.div>

        {/* Work Queue */}
        <motion.div
          id="work-queue-section"
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9 }}
        >
          {/* Section header + sort */}
          <div className="mb-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <h2 className="text-2xl font-bold text-white sm:text-3xl">
                Work Queue
              </h2>
              <p className="mt-2 text-slate-400">
                Your assigned complaints, sorted by urgency and priority.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {/* Sort control */}
              <div className="flex items-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 backdrop-blur-xl">
                <ArrowUpDown className="h-3.5 w-3.5 text-slate-400 shrink-0" />
                <span className="text-xs text-slate-400 whitespace-nowrap">Sort:</span>
                <select
                  value={sortBy}
                  onChange={(e) => handleSortChange(e.target.value)}
                  className="bg-transparent text-sm text-white focus:outline-none cursor-pointer"
                >
                  <option value="URGENCY" className="bg-slate-900">Urgency</option>
                  <option value="PRIORITY" className="bg-slate-900">Priority</option>
                  <option value="UPDATED" className="bg-slate-900">Last Updated</option>
                  <option value="NEWEST" className="bg-slate-900">Newest First</option>
                  <option value="OLDEST" className="bg-slate-900">Oldest First</option>
                </select>
              </div>

              {/* Count badge */}
              <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm text-slate-300 whitespace-nowrap">
                {isFilteringActive ? (
                  <span>
                    <span className="font-semibold text-white">{displayedComplaints.length}</span>{" "}
                    matching on page {currentPage}
                  </span>
                ) : (
                  <span>
                    <span className="font-semibold text-white">{pageData.totalElements}</span>{" "}
                    complaint{pageData.totalElements !== 1 ? "s" : ""}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Filter notice if client-side search/filter is active on page */}
          {isFilteringActive && (
            <div className="mb-4 flex items-center gap-2 rounded-xl border border-indigo-500/20 bg-indigo-500/10 px-4 py-2.5 text-xs text-indigo-300">
              <Info className="h-4 w-4 shrink-0 text-indigo-400" />
              <span>
                Filtering applied to current page ({displayedComplaints.length} of {pageData.content.length} displayed). Multi-page search and status filtering are planned for a future release.
              </span>
            </div>
          )}

          {pageLoading ? (
            <div className="flex flex-col gap-2 mb-6">
              {[...Array(3)].map((_, i) => (
                <div
                  key={i}
                  className="h-20 rounded-2xl border border-white/5 bg-white/5 animate-pulse"
                />
              ))}
            </div>
          ) : pageError ? (
            <div className="mb-6 rounded-2xl border border-rose-500/20 bg-rose-500/10 p-6 text-center text-rose-300 backdrop-blur-xl">
              <p className="text-sm font-medium">{pageError}</p>
              <Button
                onClick={() => loadPageData(currentPage, sortBy)}
                variant="outline"
                size="sm"
                className="mt-3 rounded-xl border-rose-500/30 bg-rose-500/20 text-xs text-rose-200 hover:bg-rose-500/30"
              >
                <RefreshCw className="mr-1.5 h-3.5 w-3.5" />
                Retry
              </Button>
            </div>
          ) : displayedComplaints.length > 0 ? (
            <>
              {/* Compact row list */}
              <div className="flex flex-col gap-2 mb-6">
                {displayedComplaints.map((c, index) => (
                  <motion.div
                    key={c.complaintId}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.25, delay: index * 0.03 }}
                  >
                    <ComplaintRow
                      complaint={c}
                      isStaff
                      onClick={() =>
                        navigate(`/complaint/${c.complaintId}`, {
                          state: { from: "staff-dashboard" },
                        })
                      }
                    />
                  </motion.div>
                ))}
              </div>

              {/* Pagination */}
              <Pagination
                currentPage={currentPage}
                totalPages={pageData.totalPages}
                onPageChange={handlePageChange}
                totalItems={pageData.totalElements}
                pageSize={pageSize}
              />
            </>
          ) : (
            <div className="rounded-[2rem] border border-dashed border-white/15 bg-white/6 p-12 text-center backdrop-blur-xl">
              <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-3xl bg-white/10">
                <FileText className="h-8 w-8 text-slate-300" />
              </div>

              <h3 className="mb-3 text-2xl font-bold text-white">
                No complaints found
              </h3>

              <p className="mx-auto mb-8 max-w-xl leading-7 text-slate-400">
                {isFilteringActive
                  ? "There are no assigned complaints matching your filter on this page. Try resetting your filter or navigating between pages."
                  : "There are no assigned complaints matching your work queue right now."}
              </p>

              <Button
                onClick={handleRefresh}
                className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 px-6 font-semibold transition-all hover:scale-[1.02]"
              >
                <RefreshCw className="mr-2 h-4 w-4" />
                Refresh Queue
              </Button>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}