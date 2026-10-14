import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { getStudentComplaintsPage, getMyComplaints } from "@/services/complaintApi";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import ComplaintRow from "@/components/complaints/ComplaintRow";
import Pagination from "@/components/ui/Pagination";

import {
  Layers,
  Activity,
  BadgeCheck,
  Lock,
  Plus,
  Search,
  ArrowRight,
  FileText,
  GraduationCap,
  ArrowUpDown,
  RefreshCw,
  Info,
} from "lucide-react";

import { motion } from "framer-motion";
import { useUser } from "@/context/UserContext";

export default function StudentDashboard() {
  const { user, loading: userLoading } = useUser();
  const studentId = user?.student?.studentId;

  // Stream 1: Global summary statistics & counts from existing array endpoint
  const [summaryStats, setSummaryStats] = useState({
    total: 0,
    active: 0,
    resolved: 0,
    closed: 0,
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
  const [sortBy, setSortBy] = useState("UPDATED");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 10;
  const [mouse, setMouse] = useState({ x: 0, y: 0 });

  const navigate = useNavigate();

  // Map UI sort keys to backend whitelisted properties
  const getSortParams = (uiSort) => {
    switch (uiSort) {
      case "NEWEST":
        return { sortBy: "createdAt", direction: "desc" };
      case "OLDEST":
        return { sortBy: "createdAt", direction: "asc" };
      case "PRIORITY":
        return { sortBy: "priority", direction: "desc" };
      case "UPDATED":
      default:
        return { sortBy: "updatedAt", direction: "desc" };
    }
  };

  // Fetch summary stream for overall stats
  const loadSummaryData = useCallback(async () => {
    if (userLoading || !studentId) return;
    try {
      const data = await getMyComplaints();
      const list = Array.isArray(data) ? data : [];
      setSummaryStats({
        total: list.length,
        active: list.filter((c) =>
          ["OPEN", "IN_PROGRESS", "ESCALATED"].includes(c.status)
        ).length,
        resolved: list.filter((c) => c.status === "RESOLVED").length,
        closed: list.filter((c) => c.status === "CLOSED").length,
      });
    } catch (err) {
      console.error("Failed to load student summary stats:", err);
    }
  }, [userLoading, studentId]);

  // Fetch paginated server stream
  const loadPageData = useCallback(
    async (pageToLoad = currentPage, sortOption = sortBy) => {
      if (userLoading || !studentId) return;

      try {
        setPageLoading(true);
        setPageError(null);
        const { sortBy: backendSortBy, direction } = getSortParams(sortOption);
        const data = await getStudentComplaintsPage(studentId, {
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
        console.error("Failed to load student complaints page:", err);
        setPageError("Failed to load complaints. Please try again.");
      } finally {
        setPageLoading(false);
      }
    },
    [userLoading, studentId, currentPage, sortBy, pageSize]
  );

  // Load summary once user authentication is resolved
  useEffect(() => {
    if (!userLoading && studentId) {
      loadSummaryData();
    }
  }, [userLoading, studentId, loadSummaryData]);

  // Load paginated data on page or sort changes
  useEffect(() => {
    if (!userLoading && studentId) {
      loadPageData(currentPage, sortBy);
    }
  }, [userLoading, studentId, currentPage, sortBy, loadPageData]);

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
      .getElementById("complaints-section")
      ?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  // Client-side search and status filter applied to current page content
  const displayedComplaints = useMemo(() => {
    let list = pageData.content || [];

    if (filter === "ACTIVE") {
      list = list.filter((c) =>
        ["OPEN", "IN_PROGRESS", "ESCALATED"].includes(c.status)
      );
    } else if (filter === "RESOLVED") {
      list = list.filter((c) => c.status === "RESOLVED");
    } else if (filter === "CLOSED") {
      list = list.filter((c) => c.status === "CLOSED");
    }

    if (search.trim()) {
      const q = search.trim().toLowerCase();
      list = list.filter((c) => {
        const matchId =
          String(c.complaintId || "").toLowerCase().includes(q) ||
          `#${c.complaintId}`.toLowerCase().includes(q);
        const matchTitle = (c.title || "").toLowerCase().includes(q);
        const matchCategory = (c.category || "").toLowerCase().includes(q);
        const matchDesc = (c.description || "").toLowerCase().includes(q);
        return matchId || matchTitle || matchCategory || matchDesc;
      });
    }

    return list;
  }, [pageData.content, filter, search]);

  const isFilteringActive = filter !== "ALL" || search.trim().length > 0;

  const stats = {
    total: summaryStats.total,
    active: summaryStats.active,
    resolved: summaryStats.resolved,
    closed: summaryStats.closed,
  };

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
              <GraduationCap className="h-5 w-5 text-white" />
            </div>

            <div>
              <h2 className="text-lg font-semibold tracking-wide text-white">
                SmartGriev
              </h2>
              <p className="text-xs text-slate-300">
                Premium student grievance workspace
              </p>
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
            setMouse({
              x: e.clientX - rect.left,
              y: e.clientY - rect.top,
            });
          }}
          className="relative mb-8 overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur-2xl shadow-[0_20px_80px_rgba(0,0,0,0.35)]"
        >
          <div
            className="pointer-events-none absolute h-72 w-72 rounded-full bg-indigo-400/20 blur-3xl transition-all duration-200"
            style={{
              left: mouse.x - 144,
              top: mouse.y - 144,
            }}
          />

          <div className="relative grid grid-cols-1 gap-8 p-8 sm:p-10 lg:grid-cols-3">
            <div className="lg:col-span-2">
              <h1 className="text-4xl font-black leading-tight tracking-tight sm:text-5xl xl:text-6xl">
                Welcome back,{" "}
                <span className="bg-gradient-to-r from-indigo-200 to-blue-300 bg-clip-text text-transparent">
                  {user?.student?.name || "Student"}
                </span>
              </h1>

              <p className="mt-5 max-w-2xl text-base leading-8 text-slate-300 sm:text-lg">
                Monitor your grievances, check live progress, and submit new
                complaints through a modern and intelligent complaint management
                workspace.
              </p>

              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <Button
                  onClick={() => navigate("/submit")}
                  className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
                >
                  <Plus className="mr-2 h-4 w-4" />
                  Submit New Complaint
                </Button>

                <Button
                  variant="outline"
                  onClick={() =>
                    document
                      .getElementById("complaints-section")
                      ?.scrollIntoView({ behavior: "smooth", block: "start" })
                  }
                  className="h-12 rounded-xl border-white/15 bg-white/5 text-white hover:bg-white/10"
                >
                  View All Complaints
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            </div>

            <div className="flex flex-col justify-between gap-4">
              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-slate-400">Quick Summary</p>
                <h3 className="text-3xl font-bold text-white">{stats.total}</h3>
                <p className="mt-1 text-sm text-slate-300">
                  Total complaints submitted
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/15 to-cyan-500/10 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-emerald-300">Resolved Rate</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.total > 0
                    ? `${Math.round((stats.resolved / stats.total) * 100)}%`
                    : "0%"}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Based on completed grievances
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-slate-400">Active Right Now</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.active}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Complaints currently in progress
                </p>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Premium Stats */}
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
                <p className="text-sm font-medium text-slate-300">
                  Total Complaints
                </p>
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.total}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  All complaints submitted
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-blue-500 shadow-lg shadow-indigo-500/30">
                <Layers className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-cyan-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/10 via-transparent to-blue-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">
                  Active Complaints
                </p>
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.active}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  Currently under process
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-400 via-sky-500 to-blue-500 shadow-lg shadow-cyan-500/30">
                <Activity className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-emerald-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-emerald-500/10 via-transparent to-green-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">
                  Resolved Complaints
                </p>
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.resolved}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  Successfully resolved
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-400 via-green-500 to-teal-500 shadow-lg shadow-emerald-500/30">
                <BadgeCheck className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-rose-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-rose-500/10 via-transparent to-pink-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">
                  Closed Complaints
                </p>
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.closed}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  Finalized and closed
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-rose-400 via-pink-500 to-red-500 shadow-lg shadow-rose-500/30">
                <Lock className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        </motion.div>

        {/* Search + submit */}
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
                placeholder="Search complaints by title..."
                className="h-12 rounded-2xl border-white/10 bg-slate-900/40 pl-11 text-white placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-indigo-500"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            <Button
              onClick={() => navigate("/submit")}
              className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
            >
              <Plus className="mr-2 h-4 w-4" />
              Submit New Complaint
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
          <button
            onClick={() => setFilter("ALL")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              filter === "ALL"
                ? "bg-indigo-500 text-white shadow-lg shadow-indigo-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            All
          </button>

          <button
            onClick={() => setFilter("ACTIVE")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              filter === "ACTIVE"
                ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            Active
          </button>

          <button
            onClick={() => setFilter("RESOLVED")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              filter === "RESOLVED"
                ? "bg-emerald-500 text-white shadow-lg shadow-emerald-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            Resolved
          </button>

          <button
            onClick={() => setFilter("CLOSED")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              filter === "CLOSED"
                ? "bg-rose-500 text-white shadow-lg shadow-rose-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            Closed
          </button>
        </motion.div>

        {/* Complaints */}
        <motion.div
          id="complaints-section"
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9 }}
        >
          {/* Section header + sort controls */}
          <div className="mb-5 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <h2 className="text-2xl font-bold text-white sm:text-3xl">
                Your Complaints
              </h2>
              <p className="mt-2 text-slate-400">
                Explore complaint history, status updates, and detailed records.
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
                  <option value="UPDATED" className="bg-slate-900">Last Updated</option>
                  <option value="NEWEST" className="bg-slate-900">Newest First</option>
                  <option value="OLDEST" className="bg-slate-900">Oldest First</option>
                  <option value="PRIORITY" className="bg-slate-900">Priority</option>
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
                      onClick={() =>
                        navigate(`/complaint/${c.complaintId}`, {
                          state: { from: "student-dashboard" },
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
                  ? "No complaints match your filter on this page. Try resetting your filter or navigating between pages."
                  : "There are no complaints recorded yet. Submit a new grievance to get started."}
              </p>

              <Button
                onClick={() => navigate("/submit")}
                className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 px-6 font-semibold transition-all hover:scale-[1.02]"
              >
                <Plus className="mr-2 h-4 w-4" />
                Submit Complaint
              </Button>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}
