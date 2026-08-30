import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  getAllAdminDepartments,
  createAdminDepartment,
  updateAdminDepartment,
  updateDepartmentStatus,
} from "@/services/adminDepartmentApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Building2,
  Search,
  Plus,
  Edit2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ArrowLeft,
  Users,
  Layers,
  FileText,
  GitBranch,
  X,
  Sparkles,
  ShieldCheck,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

export default function DepartmentsManagement() {
  const navigate = useNavigate();
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL"); // ALL, ACTIVE, INACTIVE, READY

  // Create Modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createCode, setCreateCode] = useState("");
  const [createName, setCreateName] = useState("");
  const [createDesc, setCreateDesc] = useState("");
  const [createError, setCreateError] = useState("");
  const [submittingCreate, setSubmittingCreate] = useState(false);

  // Edit Modal state
  const [showEditModal, setShowEditModal] = useState(false);
  const [editDept, setEditDept] = useState(null);
  const [editName, setEditName] = useState("");
  const [editDesc, setEditDesc] = useState("");
  const [editError, setEditError] = useState("");
  const [submittingEdit, setSubmittingEdit] = useState(false);

  // Status Action state
  const [actionLoadingId, setActionLoadingId] = useState(null);

  useEffect(() => {
    fetchDepartments();
  }, []);

  const fetchDepartments = async () => {
    try {
      setLoading(true);
      const data = await getAllAdminDepartments();
      setDepartments(data || []);
    } catch (err) {
      console.error("Failed to load departments:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateDepartment = async (e) => {
    e.preventDefault();
    setCreateError("");
    const trimmedCode = createCode.trim().toUpperCase();
    const trimmedName = createName.trim();

    if (!trimmedCode) {
      setCreateError("Department code is required");
      return;
    }
    if (!/^[A-Z0-9_]{2,50}$/.test(trimmedCode)) {
      setCreateError(
        "Code must be uppercase letters, numbers, or underscores (2-50 chars)",
      );
      return;
    }
    if (!trimmedName) {
      setCreateError("Department name is required");
      return;
    }

    try {
      setSubmittingCreate(true);
      await createAdminDepartment({
        code: trimmedCode,
        name: trimmedName,
        description: createDesc.trim() || null,
      });
      setShowCreateModal(false);
      setCreateCode("");
      setCreateName("");
      setCreateDesc("");
      fetchDepartments();
    } catch (err) {
      setCreateError(
        err.response?.data?.message ||
          "Failed to create department. Check uniqueness of code and name.",
      );
    } finally {
      setSubmittingCreate(false);
    }
  };

  const handleOpenEdit = (dept) => {
    setEditDept(dept);
    setEditName(dept.name || "");
    setEditDesc(dept.description || "");
    setEditError("");
    setShowEditModal(true);
  };

  const handleUpdateDepartment = async (e) => {
    e.preventDefault();
    setEditError("");
    const trimmedName = editName.trim();

    if (!trimmedName) {
      setEditError("Department name is required");
      return;
    }

    try {
      setSubmittingEdit(true);
      await updateAdminDepartment(editDept.departmentId, {
        name: trimmedName,
        description: editDesc.trim() || null,
      });
      setShowEditModal(false);
      setEditDept(null);
      fetchDepartments();
    } catch (err) {
      setEditError(
        err.response?.data?.message || "Failed to update department.",
      );
    } finally {
      setSubmittingEdit(false);
    }
  };

  const handleToggleStatus = async (dept) => {
    try {
      setActionLoadingId(dept.departmentId);
      await updateDepartmentStatus(dept.departmentId, !dept.active);
      fetchDepartments();
    } catch (err) {
      console.error("Failed to toggle department status:", err);
    } finally {
      setActionLoadingId(null);
    }
  };

  const filteredDepartments = departments.filter((d) => {
    const q = searchQuery.toLowerCase();
    const matchesQuery =
      (d.name && d.name.toLowerCase().includes(q)) ||
      (d.code && d.code.toLowerCase().includes(q)) ||
      (d.description && d.description.toLowerCase().includes(q));

    if (!matchesQuery) return false;

    if (statusFilter === "ACTIVE") return d.active === true;
    if (statusFilter === "INACTIVE") return d.active === false;
    if (statusFilter === "READY") return d.isOperationallyReady === true;

    return true;
  });

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Background gradients */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.15),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.12),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl" />

      <div className="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {/* Navigation & Header */}
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <Button
              onClick={() => navigate("/admin-dashboard")}
              variant="outline"
              size="sm"
              className="rounded-xl border-white/10 bg-white/5 text-slate-300 hover:bg-white/10"
            >
              <ArrowLeft className="mr-1.5 h-4 w-4" /> Back to Dashboard
            </Button>
            <Button
              onClick={() => navigate("/admin/categories")}
              variant="outline"
              size="sm"
              className="rounded-xl border-purple-500/30 bg-purple-500/10 text-purple-300 hover:bg-purple-500/20"
            >
              <Layers className="mr-1.5 h-4 w-4" /> Categories
            </Button>
            <Button
              onClick={() => navigate("/admin/workflows")}
              variant="outline"
              size="sm"
              className="rounded-xl border-cyan-500/30 bg-cyan-500/10 text-cyan-300 hover:bg-cyan-500/20"
            >
              <GitBranch className="mr-1.5 h-4 w-4" /> Workflows
            </Button>
          </div>

          <Button
            onClick={() => {
              setCreateCode("");
              setCreateName("");
              setCreateDesc("");
              setCreateError("");
              setShowCreateModal(true);
            }}
            className="rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 font-semibold text-white shadow-lg shadow-indigo-500/30 transition hover:scale-[1.02]"
          >
            <Plus className="mr-2 h-4 w-4" /> Add Department
          </Button>
        </div>

        {/* Title Bar */}
        <div className="mb-8 rounded-[2rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl sm:p-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h1 className="text-3xl font-black tracking-tight text-white sm:text-4xl">
                Department Management
              </h1>
              <p className="mt-2 text-sm text-slate-300">
                Configure operational business departments, stable codes, and workflow readiness.
              </p>
            </div>
            <div className="flex items-center gap-3">
              <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-2.5 text-center">
                <span className="text-xs text-slate-400">Total</span>
                <p className="text-xl font-bold text-white">{departments.length}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-emerald-500/10 px-4 py-2.5 text-center">
                <span className="text-xs text-emerald-300">Ready</span>
                <p className="text-xl font-bold text-emerald-400">
                  {departments.filter((d) => d.isOperationallyReady).length}
                </p>
              </div>
            </div>
          </div>

          {/* Search & Status Filters */}
          <div className="mt-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="relative max-w-md flex-1">
              <Search className="absolute left-3.5 top-3 h-4 w-4 text-slate-400" />
              <Input
                type="text"
                placeholder="Search by code, name, or description..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="h-10 rounded-xl border-white/10 bg-white/5 pl-10 text-white placeholder-slate-400 focus:border-indigo-500"
              />
            </div>

            <div className="flex flex-wrap gap-2">
              {[
                { label: "All", value: "ALL" },
                { label: "Active", value: "ACTIVE" },
                { label: "Ready for Ingress", value: "READY" },
                { label: "Inactive", value: "INACTIVE" },
              ].map((filter) => (
                <button
                  key={filter.value}
                  onClick={() => setStatusFilter(filter.value)}
                  className={`rounded-xl px-3.5 py-2 text-xs font-semibold transition ${
                    statusFilter === filter.value
                      ? "bg-indigo-600 text-white shadow-md shadow-indigo-600/30"
                      : "border border-white/10 bg-white/5 text-slate-300 hover:bg-white/10"
                  }`}
                >
                  {filter.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Department Table */}
        <div className="overflow-hidden rounded-[2rem] border border-white/10 bg-white/5 backdrop-blur-xl">
          {loading ? (
            <div className="p-12 text-center text-slate-400">
              <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-indigo-500 border-r-transparent" />
              Loading departments...
            </div>
          ) : filteredDepartments.length === 0 ? (
            <div className="p-12 text-center text-slate-400">
              <Building2 className="mx-auto mb-3 h-12 w-12 text-slate-500" />
              <p className="text-lg font-medium text-white">No departments found</p>
              <p className="mt-1 text-sm">Try adjusting your search query or filter options.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="border-b border-white/10 bg-white/5 text-xs uppercase tracking-wider text-slate-400">
                  <tr>
                    <th className="px-6 py-4">Code</th>
                    <th className="px-6 py-4">Department Name</th>
                    <th className="px-6 py-4">Status</th>
                    <th className="px-6 py-4">Ingress Readiness</th>
                    <th className="px-6 py-4">Categories</th>
                    <th className="px-6 py-4">Staff</th>
                    <th className="px-6 py-4">Open Cases</th>
                    <th className="px-6 py-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {filteredDepartments.map((dept) => (
                    <tr
                      key={dept.departmentId}
                      className="transition hover:bg-white/5"
                    >
                      {/* Code Badge */}
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center rounded-lg border border-indigo-500/30 bg-indigo-500/10 px-2.5 py-1 font-mono text-xs font-bold text-indigo-300">
                          {dept.code}
                        </span>
                      </td>

                      {/* Name & Description */}
                      <td className="px-6 py-4">
                        <div>
                          <p className="font-semibold text-white">{dept.name}</p>
                          {dept.description && (
                            <p className="mt-0.5 line-clamp-1 text-xs text-slate-400">
                              {dept.description}
                            </p>
                          )}
                        </div>
                      </td>

                      {/* Active Status */}
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${
                            dept.active
                              ? "bg-emerald-500/15 text-emerald-300 border border-emerald-500/20"
                              : "bg-rose-500/15 text-rose-300 border border-rose-500/20"
                          }`}
                        >
                          {dept.active ? (
                            <CheckCircle2 className="h-3 w-3" />
                          ) : (
                            <XCircle className="h-3 w-3" />
                          )}
                          {dept.active ? "Active" : "Inactive"}
                        </span>
                      </td>

                      {/* Operational Readiness */}
                      <td className="px-6 py-4">
                        <button
                          onClick={() => navigate(`/admin/workflows?departmentId=${dept.departmentId}`)}
                          className="transition hover:scale-105"
                          title="Manage Workflows for this Department"
                        >
                          {dept.isOperationallyReady ? (
                            <span className="inline-flex items-center gap-1 rounded-full border border-cyan-500/20 bg-cyan-500/10 px-2.5 py-0.5 text-xs font-medium text-cyan-300">
                              <Sparkles className="h-3 w-3" /> Ready
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/20 bg-amber-500/10 px-2.5 py-0.5 text-xs font-medium text-amber-300">
                              <AlertTriangle className="h-3 w-3" /> Draft Workflow
                            </span>
                          )}
                        </button>
                      </td>

                      {/* Counts */}
                      <td className="px-6 py-4">
                        <button
                          onClick={() => navigate(`/admin/categories?departmentId=${dept.departmentId}`)}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-purple-500/20 bg-purple-500/10 px-2 py-1 text-xs font-semibold text-purple-300 transition hover:bg-purple-500/20"
                          title="View Categories for this Department"
                        >
                          <Layers className="h-3.5 w-3.5 text-purple-400" />
                          {dept.categoryCount ?? 0}
                        </button>
                      </td>
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center gap-1.5 text-xs">
                          <Users className="h-3.5 w-3.5 text-slate-400" />
                          {dept.staffCount ?? 0}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center gap-1.5 text-xs">
                          <FileText className="h-3.5 w-3.5 text-slate-400" />
                          {dept.openComplaintsCount ?? 0}
                        </span>
                      </td>

                      {/* Actions */}
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleOpenEdit(dept)}
                            className="h-8 rounded-lg border-white/10 bg-white/5 px-2.5 text-xs text-slate-300 hover:bg-white/10"
                          >
                            <Edit2 className="mr-1 h-3 w-3" /> Edit
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={actionLoadingId === dept.departmentId}
                            onClick={() => handleToggleStatus(dept)}
                            className={`h-8 rounded-lg px-2.5 text-xs ${
                              dept.active
                                ? "border-rose-500/20 bg-rose-500/10 text-rose-300 hover:bg-rose-500/20"
                                : "border-emerald-500/20 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20"
                            }`}
                          >
                            {dept.active ? "Deactivate" : "Activate"}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* CREATE MODAL */}
      <AnimatePresence>
        {showCreateModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="fixed inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => setShowCreateModal(false)}
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="relative w-full max-w-md overflow-hidden rounded-2xl border border-white/10 bg-slate-900 p-6 shadow-2xl"
            >
              <div className="flex items-center justify-between pb-4 border-b border-white/10">
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <Plus className="h-5 w-5 text-indigo-400" /> Create Department
                </h3>
                <button
                  onClick={() => setShowCreateModal(false)}
                  className="rounded-lg p-1 text-slate-400 hover:bg-white/10 hover:text-white"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {createError && (
                <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-xs text-rose-300">
                  {createError}
                </div>
              )}

              <form onSubmit={handleCreateDepartment} className="mt-4 space-y-4">
                <div>
                  <label className="text-xs font-semibold text-slate-300">
                    Department Code * (Immutable)
                  </label>
                  <Input
                    type="text"
                    placeholder="e.g. FINANCE, PLACEMENT"
                    value={createCode}
                    onChange={(e) => setCreateCode(e.target.value.toUpperCase())}
                    className="mt-1 font-mono uppercase bg-white/5 border-white/10 text-white placeholder-slate-500"
                    required
                  />
                  <p className="mt-1 text-[11px] text-slate-400">
                    Must be uppercase letters, numbers, or underscores (2-50 chars).
                  </p>
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-300">
                    Department Name *
                  </label>
                  <Input
                    type="text"
                    placeholder="e.g. Finance & Accounts"
                    value={createName}
                    onChange={(e) => setCreateName(e.target.value)}
                    className="mt-1 bg-white/5 border-white/10 text-white placeholder-slate-500"
                    required
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-300">
                    Description (Optional)
                  </label>
                  <textarea
                    rows={3}
                    placeholder="Brief description of department scope and responsibilities..."
                    value={createDesc}
                    onChange={(e) => setCreateDesc(e.target.value)}
                    className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 p-2.5 text-xs text-white placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
                    maxLength={500}
                  />
                </div>

                <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/10">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setShowCreateModal(false)}
                    className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={submittingCreate}
                    className="bg-gradient-to-r from-indigo-500 to-blue-500 font-semibold text-white"
                  >
                    {submittingCreate ? "Creating..." : "Create Department"}
                  </Button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* EDIT MODAL */}
      <AnimatePresence>
        {showEditModal && editDept && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="fixed inset-0 bg-black/60 backdrop-blur-sm"
              onClick={() => setShowEditModal(false)}
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="relative w-full max-w-md overflow-hidden rounded-2xl border border-white/10 bg-slate-900 p-6 shadow-2xl"
            >
              <div className="flex items-center justify-between pb-4 border-b border-white/10">
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <Edit2 className="h-5 w-5 text-indigo-400" /> Edit Department
                </h3>
                <button
                  onClick={() => setShowEditModal(false)}
                  className="rounded-lg p-1 text-slate-400 hover:bg-white/10 hover:text-white"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {editError && (
                <div className="mt-4 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-xs text-rose-300">
                  {editError}
                </div>
              )}

              <form onSubmit={handleUpdateDepartment} className="mt-4 space-y-4">
                <div>
                  <label className="text-xs font-semibold text-slate-400">
                    Department Code (Immutable)
                  </label>
                  <Input
                    type="text"
                    value={editDept.code}
                    disabled
                    className="mt-1 font-mono bg-white/5 border-white/10 text-slate-400 cursor-not-allowed opacity-70"
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-300">
                    Department Name *
                  </label>
                  <Input
                    type="text"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    className="mt-1 bg-white/5 border-white/10 text-white placeholder-slate-500"
                    required
                  />
                </div>

                <div>
                  <label className="text-xs font-semibold text-slate-300">
                    Description (Optional)
                  </label>
                  <textarea
                    rows={3}
                    value={editDesc}
                    onChange={(e) => setEditDesc(e.target.value)}
                    className="mt-1 w-full rounded-xl border border-white/10 bg-white/5 p-2.5 text-xs text-white placeholder-slate-500 focus:border-indigo-500 focus:outline-none"
                    maxLength={500}
                  />
                </div>

                <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/10">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setShowEditModal(false)}
                    className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={submittingEdit}
                    className="bg-gradient-to-r from-indigo-500 to-blue-500 font-semibold text-white"
                  >
                    {submittingEdit ? "Updating..." : "Save Changes"}
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
