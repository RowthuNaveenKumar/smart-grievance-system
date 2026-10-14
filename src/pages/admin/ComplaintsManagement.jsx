import React, { useEffect, useMemo, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";
import {
  FileText,
  Search,
  Filter,
  Sparkles,
  ShieldCheck,
  Building2,
  UserCheck,
  Clock,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Edit3,
  UserPlus,
  ChevronRight,
  Info,
  X,
  Lock,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import BackButton from "@/components/ui/BackButton";
import Pagination from "@/components/ui/Pagination";
import { Input } from "@/components/ui/input";
import {
  getAdminComplaintsPage,
  overrideDepartment,
  reassignStaff,
  getDepartments,
  getStaffByDepartment,
} from "@/services/adminComplaintApi";

export default function ComplaintsManagement() {
  const navigate = useNavigate();

  const [pageData, setPageData] = useState({
    content: [],
    totalElements: 0,
    totalPages: 1,
    number: 0,
    size: 10,
  });
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [deptFilter, setDeptFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");

  // Override Modal
  const [overrideModalOpen, setOverrideModalOpen] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [targetDeptId, setTargetDeptId] = useState("");
  const [overrideReason, setOverrideReason] = useState("");
  const [overrideSubmitting, setOverrideSubmitting] = useState(false);

  // Reassign Modal
  const [reassignModalOpen, setReassignModalOpen] = useState(false);
  const [deptStaffList, setDeptStaffList] = useState([]);
  const [targetStaffId, setTargetStaffId] = useState("");
  const [reassignReason, setReassignReason] = useState("");
  const [reassignLoading, setReassignLoading] = useState(false);
  const [reassignSubmitting, setReassignSubmitting] = useState(false);

  // Load departments once on mount
  useEffect(() => {
    loadDepartments();
  }, []);

  const loadDepartments = async () => {
    try {
      const deptsData = await getDepartments();
      setDepartments(deptsData || []);
    } catch (err) {
      console.error("Failed to load departments:", err);
    }
  };

  // Load paginated complaints from server
  const loadComplaintsPage = useCallback(
    async (pageToLoad = currentPage) => {
      try {
        setLoading(true);
        const data = await getAdminComplaintsPage({
          page: pageToLoad - 1, // 1-based UI to 0-based Spring Page
          size: pageSize,
          sortBy: "createdAt",
          direction: "DESC",
          status: statusFilter !== "ALL" ? statusFilter : undefined,
          priority: priorityFilter !== "ALL" ? priorityFilter : undefined,
          departmentId: deptFilter !== "ALL" ? Number(deptFilter) : undefined,
        });

        setPageData({
          content: Array.isArray(data?.content) ? data.content : [],
          totalElements: typeof data?.totalElements === "number" ? data.totalElements : 0,
          totalPages: Math.max(1, data?.totalPages || 1),
          number: data?.number || 0,
          size: data?.size || pageSize,
        });
      } catch (err) {
        console.error("Failed to load complaints page:", err);
        toast.error("Failed to load complaints");
      } finally {
        setLoading(false);
      }
    },
    [currentPage, pageSize, statusFilter, deptFilter, priorityFilter]
  );

  useEffect(() => {
    loadComplaintsPage(currentPage);
  }, [currentPage, statusFilter, deptFilter, priorityFilter, loadComplaintsPage]);

  // Safe page boundary clamping if totalPages shrinks
  useEffect(() => {
    if (pageData.totalPages > 0 && currentPage > pageData.totalPages) {
      setCurrentPage(pageData.totalPages);
    }
  }, [pageData.totalPages, currentPage]);

  const handleStatusFilterChange = (val) => {
    setStatusFilter(val);
    setCurrentPage(1);
  };

  const handleDeptFilterChange = (val) => {
    setDeptFilter(val);
    setCurrentPage(1);
  };

  const handlePriorityFilterChange = (val) => {
    setPriorityFilter(val);
    setCurrentPage(1);
  };

  // Explicitly scoped to the currently loaded page
  const displayedComplaints = useMemo(() => {
    if (!search.trim()) return pageData.content;
    const query = search.toLowerCase();
    return pageData.content.filter((c) =>
      c.title?.toLowerCase().includes(query) ||
      c.description?.toLowerCase().includes(query) ||
      c.studentName?.toLowerCase().includes(query) ||
      String(c.complaintId).includes(query)
    );
  }, [pageData.content, search]);

  // Open Override Modal
  const openOverrideModal = (complaint) => {
    setSelectedComplaint(complaint);
    setTargetDeptId(complaint.departmentId ? String(complaint.departmentId) : "");
    setOverrideReason("");
    setOverrideModalOpen(true);
  };

  const handleOverrideSubmit = async (e) => {
    e.preventDefault();
    if (!targetDeptId) {
      toast.error("Please select a target department");
      return;
    }
    if (!overrideReason.trim()) {
      toast.error("Please provide a justification reason for the override");
      return;
    }

    try {
      setOverrideSubmitting(true);
      await overrideDepartment(selectedComplaint.complaintId, {
        departmentId: Number(targetDeptId),
        note: overrideReason.trim(),
      });
      toast.success("Department overridden successfully");
      setOverrideModalOpen(false);
      loadComplaintsPage(currentPage);
    } catch (err) {
      console.error(err);
      toast.error(err.response?.data?.message || "Failed to override department");
    } finally {
      setOverrideSubmitting(false);
    }
  };

  // Open Reassign Modal
  const openReassignModal = async (complaint) => {
    setSelectedComplaint(complaint);
    setTargetStaffId(complaint.assignedStaffId ? String(complaint.assignedStaffId) : "");
    setReassignReason("");
    setReassignModalOpen(true);

    if (complaint.departmentId) {
      try {
        setReassignLoading(true);
        const staffData = await getStaffByDepartment(complaint.departmentId);
        setDeptStaffList(staffData || []);
      } catch (err) {
        console.error(err);
        toast.error("Failed to load department staff");
      } finally {
        setReassignLoading(false);
      }
    } else {
      setDeptStaffList([]);
    }
  };

  const handleReassignSubmit = async (e) => {
    e.preventDefault();
    if (!targetStaffId) {
      toast.error("Please select a staff member");
      return;
    }

    try {
      setReassignSubmitting(true);
      await reassignStaff(selectedComplaint.complaintId, {
        staffId: Number(targetStaffId),
        note: reassignReason.trim() || undefined,
      });
      toast.success("Staff reassigned successfully");
      setReassignModalOpen(false);
      loadComplaintsPage(currentPage);
    } catch (err) {
      console.error(err);
      toast.error(err.response?.data?.message || "Failed to reassign staff");
    } finally {
      setReassignSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch ((status || "").toUpperCase()) {
      case "OPEN":
        return "bg-amber-500/15 text-amber-300 border-amber-500/30";
      case "IN_PROGRESS":
        return "bg-cyan-500/15 text-cyan-300 border-cyan-500/30";
      case "ESCALATED":
        return "bg-orange-500/15 text-orange-300 border-orange-500/30";
      case "RESOLVED":
        return "bg-emerald-500/15 text-emerald-300 border-emerald-500/30";
      case "CLOSED":
        return "bg-slate-500/15 text-slate-300 border-slate-500/30";
      default:
        return "bg-indigo-500/15 text-indigo-300 border-indigo-500/30";
    }
  };

  const getPriorityBadge = (priority) => {
    switch ((priority || "").toUpperCase()) {
      case "CRITICAL":
      case "HIGH":
        return "bg-rose-500/15 text-rose-300 border-rose-500/30";
      case "MEDIUM":
        return "bg-yellow-500/15 text-yellow-300 border-yellow-500/30";
      case "LOW":
        return "bg-cyan-500/15 text-cyan-300 border-cyan-500/30";
      default:
        return "bg-slate-500/15 text-slate-300 border-slate-500/30";
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Background gradients */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.15),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.12),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/15 blur-3xl" />
      <div className="absolute top-1/3 -right-20 h-96 w-96 rounded-full bg-blue-500/15 blur-3xl" />

      <div className="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <BackButton fallback="/admin-dashboard" />

        {/* Top Header */}
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between rounded-2xl border border-white/10 bg-white/5 px-6 py-4 backdrop-blur-xl shadow-2xl">
          <div className="flex items-center gap-4">
            <div>
              <h1 className="text-xl font-bold text-white flex items-center gap-2">
                <FileText className="h-5 w-5 text-indigo-400" />
                Complaints Management
              </h1>
              <p className="text-xs text-slate-400">
                Oversee, audit, and re-route platform grievances
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="rounded-xl border border-white/10 bg-slate-900/60 px-3 py-1.5 text-xs text-slate-300">
              Total: <span className="font-semibold text-indigo-300">{pageData.totalElements}</span>
            </div>
            <Button
              onClick={() => loadComplaintsPage(currentPage)}
              variant="outline"
              size="sm"
              className="rounded-xl border-white/10 bg-white/5 hover:bg-white/10 text-xs"
            >
              <RefreshCw className={`mr-1.5 h-3.5 w-3.5 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </Button>
          </div>
        </div>

        {/* Filter Controls Bar */}
        <div className="mb-8 rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur-xl shadow-lg">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {/* Search Input (Explicitly scoped to loaded page) */}
            <div className="relative">
              <Search className="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
              <Input
                placeholder="Filter loaded page (ID, title)..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9 bg-slate-900/60 border-white/10 text-sm rounded-xl text-white placeholder:text-slate-500"
              />
            </div>

            {/* Status Filter */}
            <div>
              <select
                value={statusFilter}
                onChange={(e) => handleStatusFilterChange(e.target.value)}
                className="w-full h-10 px-3 rounded-xl border border-white/10 bg-slate-900/60 text-sm text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="ALL">All Statuses</option>
                <option value="OPEN">OPEN</option>
                <option value="IN_PROGRESS">IN_PROGRESS</option>
                <option value="ESCALATED">ESCALATED</option>
                <option value="RESOLVED">RESOLVED</option>
                <option value="CLOSED">CLOSED</option>
              </select>
            </div>

            {/* Department Filter */}
            <div>
              <select
                value={deptFilter}
                onChange={(e) => handleDeptFilterChange(e.target.value)}
                className="w-full h-10 px-3 rounded-xl border border-white/10 bg-slate-900/60 text-sm text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="ALL">All Departments</option>
                {departments.map((d) => (
                  <option key={d.departmentId} value={String(d.departmentId)}>
                    {d.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Priority Filter */}
            <div>
              <select
                value={priorityFilter}
                onChange={(e) => handlePriorityFilterChange(e.target.value)}
                className="w-full h-10 px-3 rounded-xl border border-white/10 bg-slate-900/60 text-sm text-slate-200 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                <option value="ALL">All Priorities</option>
                <option value="CRITICAL">CRITICAL</option>
                <option value="HIGH">HIGH</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="LOW">LOW</option>
              </select>
            </div>
          </div>

          {search.trim() && (
            <div className="mt-3 flex items-center justify-between text-xs text-slate-400 border-t border-white/5 pt-3">
              <span>
                Filtering visible complaints on page {currentPage} for: <strong className="text-white">"{search}"</strong>
              </span>
              <button
                type="button"
                onClick={() => setSearch("")}
                className="text-indigo-400 hover:text-indigo-300 underline"
              >
                Clear page filter
              </button>
            </div>
          )}
        </div>

        {/* Complaint Cards List */}
        {loading ? (
          <div className="rounded-3xl border border-white/10 bg-white/5 p-16 text-center backdrop-blur-xl">
            <div className="mx-auto mb-4 h-10 w-10 animate-spin rounded-full border-4 border-indigo-500 border-r-transparent" />
            <p className="text-slate-300">Loading complaints registry...</p>
          </div>
        ) : displayedComplaints.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-white/10 bg-white/5 p-16 text-center backdrop-blur-xl">
            <FileText className="mx-auto mb-3 h-12 w-12 text-slate-500" />
            <h3 className="text-lg font-semibold text-white">
              {search.trim()
                ? "No complaints on this page match your search"
                : "No complaints match your filters"}
            </h3>
            <p className="mt-1 text-sm text-slate-400">
              {search.trim()
                ? "Search is scoped to the currently loaded page. Clear search or navigate other pages."
                : "Try adjusting your status, department, or priority filters."}
            </p>
            {search.trim() && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => setSearch("")}
                className="mt-4 rounded-xl border-white/10 bg-white/5 hover:bg-white/10 text-xs"
              >
                Clear page filter
              </Button>
            )}
          </div>
        ) : (
          <div className="space-y-4">
            {displayedComplaints.map((c) => (
              <motion.div
                key={c.complaintId}
                layout
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                className="group rounded-2xl border border-white/10 bg-white/5 p-5 backdrop-blur-xl transition hover:border-indigo-500/30 hover:bg-white/[0.07] shadow-lg"
              >
                <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                  {/* Left: Info */}
                  <div className="flex-1 space-y-2">
                    <div className="flex flex-wrap items-center gap-2.5">
                      <span className="font-mono text-xs font-bold text-indigo-400 bg-indigo-500/10 px-2.5 py-1 rounded-lg border border-indigo-500/20">
                        #{c.complaintId}
                      </span>
                      <h3 className="text-base font-semibold text-white">
                        {c.title}
                      </h3>
                      <span className={`text-xs px-2.5 py-0.5 rounded-full border font-medium ${getStatusBadge(c.status)}`}>
                        {c.status}
                      </span>
                      <span className={`text-xs px-2.5 py-0.5 rounded-full border font-medium ${getPriorityBadge(c.priority)}`}>
                        {c.priority}
                      </span>
                    </div>

                    <p className="line-clamp-2 text-sm text-slate-300">
                      {c.description}
                    </p>

                    {/* Routing & AI Metadata Dual-Pill Display */}
                    <div className="flex flex-wrap items-center gap-3 pt-1 text-xs">
                      {/* AI Prediction Audit Pill */}
                      <div className="flex items-center gap-1.5 rounded-lg border border-purple-500/25 bg-purple-500/10 px-2.5 py-1 text-purple-300">
                        <Sparkles className="h-3.5 w-3.5 text-purple-400" />
                        <span>AI: <strong className="text-white">{c.mlPredictedClass || "N/A"}</strong></span>
                        {c.mlConfidence && (
                          <span className="text-[11px] text-purple-400">({Math.round(c.mlConfidence * 100)}%)</span>
                        )}
                      </div>

                      {/* Current Operational Routing Pill */}
                      <div className="flex items-center gap-1.5 rounded-lg border border-blue-500/25 bg-blue-500/10 px-2.5 py-1 text-blue-300">
                        <Building2 className="h-3.5 w-3.5 text-blue-400" />
                        <span>Dept: <strong className="text-white">{c.department || "Unassigned"}</strong></span>
                      </div>

                      {/* Staff Pill */}
                      <div className="flex items-center gap-1.5 rounded-lg border border-cyan-500/25 bg-cyan-500/10 px-2.5 py-1 text-cyan-300">
                        <UserCheck className="h-3.5 w-3.5 text-cyan-400" />
                        <span>Staff: <strong className="text-white">{c.assignedTo || "Unassigned"}</strong></span>
                      </div>

                      {/* Override Pill if overridden */}
                      {c.adminOverrideNote && (
                        <div className="flex items-center gap-1.5 rounded-lg border border-amber-500/25 bg-amber-500/10 px-2.5 py-1 text-amber-300" title={c.adminOverrideNote}>
                          <Info className="h-3.5 w-3.5 text-amber-400" />
                          <span>Overridden</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Right: Actions */}
                  <div className="flex items-center gap-2 self-end lg:self-center">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => openOverrideModal(c)}
                      className="rounded-xl border-white/10 bg-white/5 hover:bg-indigo-600 hover:text-white text-xs font-medium"
                    >
                      <Edit3 className="mr-1.5 h-3.5 w-3.5" />
                      Override Dept
                    </Button>

                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => openReassignModal(c)}
                      className="rounded-xl border-white/10 bg-white/5 hover:bg-cyan-600 hover:text-white text-xs font-medium"
                    >
                      <UserPlus className="mr-1.5 h-3.5 w-3.5" />
                      Reassign
                    </Button>

                    <Button
                      size="sm"
                      onClick={() => navigate(`/complaint/${c.complaintId}`, { state: { from: "admin-complaints" } })}
                      className="rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 hover:from-indigo-600 hover:to-blue-600 text-xs font-semibold"
                    >
                      Details
                      <ChevronRight className="ml-1 h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              </motion.div>
            ))}

            {/* Pagination Controls */}
            <div className="pt-4">
              <Pagination
                currentPage={currentPage}
                totalPages={pageData.totalPages}
                totalItems={pageData.totalElements}
                pageSize={pageData.size || pageSize}
                onPageChange={(p) => setCurrentPage(p)}
              />
            </div>
          </div>
        )}
      </div>

      {/* OVERRIDE DEPARTMENT MODAL */}
      <AnimatePresence>
        {overrideModalOpen && selectedComplaint && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-lg rounded-3xl border border-white/15 bg-slate-900 p-6 shadow-2xl"
            >
              <div className="flex items-center justify-between pb-4 border-b border-white/10">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/20 text-indigo-400">
                    <Edit3 className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-white">Override Department</h2>
                    <p className="text-xs text-slate-400">Complaint #{selectedComplaint.complaintId}</p>
                  </div>
                </div>
                <button
                  onClick={() => setOverrideModalOpen(false)}
                  className="rounded-lg p-1.5 text-slate-400 hover:bg-white/10 hover:text-white"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <form onSubmit={handleOverrideSubmit} className="mt-5 space-y-4">
                {/* AI Prediction Notice (Explaining Immutability) */}
                <div className="rounded-xl border border-purple-500/20 bg-purple-500/10 p-3.5 text-xs text-purple-200">
                  <div className="flex items-center gap-1.5 font-semibold text-purple-300">
                    <Sparkles className="h-3.5 w-3.5" />
                    AI Prediction: {selectedComplaint.mlPredictedClass || "None"} ({Math.round((selectedComplaint.mlConfidence || 0) * 100)}%)
                  </div>
                  <p className="mt-1 text-slate-300 text-[11px]">
                    The AI prediction remains permanent for auditing. Overriding will only modify operational routing.
                  </p>
                </div>

                {/* Target Department Selection */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Target Department *
                  </label>
                  <select
                    value={targetDeptId}
                    onChange={(e) => setTargetDeptId(e.target.value)}
                    className="w-full h-11 px-3.5 rounded-xl border border-white/10 bg-slate-800 text-sm text-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    required
                  >
                    <option value="">Select Target Department</option>
                    {departments
                      .filter((d) => d.active !== false)
                      .map((d) => (
                        <option key={d.departmentId} value={String(d.departmentId)}>
                          {d.name}
                        </option>
                      ))}
                  </select>
                </div>

                {/* Justification Reason Note (Mandatory) */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Override Justification Reason *
                  </label>
                  <textarea
                    rows={3}
                    placeholder="Provide justification note (e.g. Incident physically occurred in hostel, misclassified by student)..."
                    value={overrideReason}
                    onChange={(e) => setOverrideReason(e.target.value)}
                    className="w-full p-3 rounded-xl border border-white/10 bg-slate-800 text-sm text-white placeholder:text-slate-500 focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    required
                  />
                </div>

                {/* Actions */}
                <div className="flex justify-end gap-3 pt-2">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setOverrideModalOpen(false)}
                    className="rounded-xl text-slate-300 hover:bg-white/10 hover:text-white"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={overrideSubmitting}
                    className="rounded-xl bg-indigo-600 hover:bg-indigo-700 font-semibold text-white px-5"
                  >
                    {overrideSubmitting ? "Overriding..." : "Confirm Override"}
                  </Button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* REASSIGN STAFF MODAL */}
      <AnimatePresence>
        {reassignModalOpen && selectedComplaint && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="w-full max-w-lg rounded-3xl border border-white/15 bg-slate-900 p-6 shadow-2xl"
            >
              <div className="flex items-center justify-between pb-4 border-b border-white/10">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/20 text-cyan-400">
                    <UserPlus className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-white">Reassign Staff</h2>
                    <p className="text-xs text-slate-400">Department: {selectedComplaint.department || "None"}</p>
                  </div>
                </div>
                <button
                  onClick={() => setReassignModalOpen(false)}
                  className="rounded-lg p-1.5 text-slate-400 hover:bg-white/10 hover:text-white"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <form onSubmit={handleReassignSubmit} className="mt-5 space-y-4">
                {/* Staff Selection */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Assign to Department Staff *
                  </label>
                  {reassignLoading ? (
                    <div className="p-3 text-center text-xs text-slate-400 bg-slate-800 rounded-xl">
                      Loading department staff...
                    </div>
                  ) : deptStaffList.length === 0 ? (
                    <div className="p-3 text-center text-xs text-amber-300 bg-amber-500/10 border border-amber-500/20 rounded-xl">
                      No active staff found for this department. Please create/assign staff in Staff Management first.
                    </div>
                  ) : (
                    <select
                      value={targetStaffId}
                      onChange={(e) => setTargetStaffId(e.target.value)}
                      className="w-full h-11 px-3.5 rounded-xl border border-white/10 bg-slate-800 text-sm text-white focus:ring-2 focus:ring-cyan-500 focus:outline-none"
                      required
                    >
                      <option value="">Select Staff Member</option>
                      {deptStaffList.map((s) => (
                        <option key={s.staffId} value={String(s.staffId)}>
                          {s.name} ({s.email})
                        </option>
                      ))}
                    </select>
                  )}
                </div>

                {/* Optional Note */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                    Reassignment Note (Optional)
                  </label>
                  <textarea
                    rows={2}
                    placeholder="e.g. Assigned to senior officer for resolution..."
                    value={reassignReason}
                    onChange={(e) => setReassignReason(e.target.value)}
                    className="w-full p-3 rounded-xl border border-white/10 bg-slate-800 text-sm text-white placeholder:text-slate-500 focus:ring-2 focus:ring-cyan-500 focus:outline-none"
                  />
                </div>

                {/* Actions */}
                <div className="flex justify-end gap-3 pt-2">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setReassignModalOpen(false)}
                    className="rounded-xl text-slate-300 hover:bg-white/10 hover:text-white"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={reassignSubmitting || deptStaffList.length === 0}
                    className="rounded-xl bg-cyan-600 hover:bg-cyan-700 font-semibold text-white px-5"
                  >
                    {reassignSubmitting ? "Reassigning..." : "Confirm Reassignment"}
                  </Button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
