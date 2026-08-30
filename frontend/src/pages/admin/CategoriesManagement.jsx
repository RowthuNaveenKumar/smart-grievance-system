import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  getAllAdminCategories,
  createAdminCategory,
  updateAdminCategory,
  updateCategoryStatus,
} from "@/services/adminCategoryApi";
import { getAllAdminDepartments } from "@/services/adminDepartmentApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Layers,
  Building2,
  Search,
  Plus,
  Edit2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ArrowLeft,
  FileText,
  X,
  Sparkles,
  ShieldCheck,
  Brain,
  Filter,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

export default function CategoriesManagement() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialDeptFilter = searchParams.get("departmentId") || "ALL";

  const [categories, setCategories] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [deptFilter, setDeptFilter] = useState(initialDeptFilter);
  const [statusFilter, setStatusFilter] = useState("ALL"); // ALL, ACTIVE, INACTIVE, ML_LINKED, CUSTOM

  // Create Modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createName, setCreateName] = useState("");
  const [createDeptId, setCreateDeptId] = useState("");
  const [createDesc, setCreateDesc] = useState("");
  const [createDisplayOrder, setCreateDisplayOrder] = useState("0");
  const [createError, setCreateError] = useState("");
  const [submittingCreate, setSubmittingCreate] = useState(false);

  // Edit Modal state
  const [showEditModal, setShowEditModal] = useState(false);
  const [editCategory, setEditCategory] = useState(null);
  const [editName, setEditName] = useState("");
  const [editDesc, setEditDesc] = useState("");
  const [editDisplayOrder, setEditDisplayOrder] = useState("0");
  const [editError, setEditError] = useState("");
  const [submittingEdit, setSubmittingEdit] = useState(false);

  // Status Action state
  const [actionLoadingId, setActionLoadingId] = useState(null);

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    try {
      setLoading(true);
      const [cats, depts] = await Promise.all([
        getAllAdminCategories(),
        getAllAdminDepartments(),
      ]);
      setCategories(cats || []);
      setDepartments(depts || []);
    } catch (err) {
      console.error("Failed to load category management data:", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchCategories = async () => {
    try {
      const cats = await getAllAdminCategories();
      setCategories(cats || []);
    } catch (err) {
      console.error("Failed to reload categories:", err);
    }
  };

  const handleCreateCategory = async (e) => {
    e.preventDefault();
    setCreateError("");
    const trimmedName = createName.trim();

    if (!trimmedName) {
      setCreateError("Category name is required");
      return;
    }
    if (trimmedName.length < 2 || trimmedName.length > 100) {
      setCreateError("Category name must be between 2 and 100 characters");
      return;
    }
    if (!createDeptId) {
      setCreateError("Please select a target department");
      return;
    }

    try {
      setSubmittingCreate(true);
      await createAdminCategory({
        name: trimmedName,
        departmentId: Number(createDeptId),
        description: createDesc.trim() || null,
        displayOrder: parseInt(createDisplayOrder, 10) || 0,
      });
      setShowCreateModal(false);
      setCreateName("");
      setCreateDeptId("");
      setCreateDesc("");
      setCreateDisplayOrder("0");
      fetchCategories();
    } catch (err) {
      setCreateError(
        err.response?.data?.message ||
          "Failed to create category. Check uniqueness within department.",
      );
    } finally {
      setSubmittingCreate(false);
    }
  };

  const handleOpenEdit = (cat) => {
    setEditCategory(cat);
    setEditName(cat.name || "");
    setEditDesc(cat.description || "");
    setEditDisplayOrder(String(cat.displayOrder ?? 0));
    setEditError("");
    setShowEditModal(true);
  };

  const handleUpdateCategory = async (e) => {
    e.preventDefault();
    setEditError("");
    const trimmedName = editName.trim();

    if (!trimmedName) {
      setEditError("Category name is required");
      return;
    }
    if (trimmedName.length < 2 || trimmedName.length > 100) {
      setEditError("Category name must be between 2 and 100 characters");
      return;
    }

    try {
      setSubmittingEdit(true);
      await updateAdminCategory(editCategory.categoryId, {
        name: trimmedName,
        description: editDesc.trim() || null,
        displayOrder: parseInt(editDisplayOrder, 10) || 0,
      });
      setShowEditModal(false);
      setEditCategory(null);
      fetchCategories();
    } catch (err) {
      setEditError(
        err.response?.data?.message || "Failed to update category.",
      );
    } finally {
      setSubmittingEdit(false);
    }
  };

  const handleToggleStatus = async (cat) => {
    const newStatus = !cat.active;
    const actionText = newStatus ? "reactivate" : "soft-deactivate";

    if (
      !window.confirm(
        `Are you sure you want to ${actionText} category "${cat.name}" in "${cat.departmentName}"?`,
      )
    ) {
      return;
    }

    try {
      setActionLoadingId(cat.categoryId);
      await updateCategoryStatus(cat.categoryId, newStatus);
      await fetchCategories();
    } catch (err) {
      alert(
        err.response?.data?.message ||
          `Failed to ${actionText} category. Verify department is active.`,
      );
    } finally {
      setActionLoadingId(null);
    }
  };

  // Filtered list
  const filteredCategories = categories.filter((cat) => {
    const matchesSearch =
      cat.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      cat.departmentName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      cat.departmentCode?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      cat.mlClass?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      cat.description?.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesDept =
      deptFilter === "ALL" || String(cat.departmentId) === String(deptFilter);

    let matchesStatus = true;
    if (statusFilter === "ACTIVE") matchesStatus = cat.active;
    if (statusFilter === "INACTIVE") matchesStatus = !cat.active;
    if (statusFilter === "ML_LINKED") matchesStatus = Boolean(cat.mlClass);
    if (statusFilter === "CUSTOM") matchesStatus = !cat.mlClass;

    return matchesSearch && matchesDept && matchesStatus;
  });

  const totalCategories = categories.length;
  const activeCategoriesCount = categories.filter((c) => c.active).length;
  const mlLinkedCount = categories.filter((c) => Boolean(c.mlClass)).length;
  const customCount = categories.filter((c) => !c.mlClass).length;

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Dynamic Background Glows */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_30%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.14),transparent_35%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-purple-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-teal-400/10 blur-3xl animate-pulse" />

      <div className="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {/* Navigation & Header */}
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              size="icon"
              onClick={() => navigate("/admin-dashboard")}
              className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
            >
              <ArrowLeft className="h-5 w-5" />
            </Button>
            <div>
              <div className="flex items-center gap-2">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-purple-500 via-indigo-500 to-blue-500 shadow-lg shadow-purple-500/30">
                  <Layers className="h-5 w-5 text-white" />
                </div>
                <h1 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
                  Category Management
                </h1>
                <span className="flex items-center gap-1 rounded-full border border-purple-400/30 bg-purple-500/10 px-2.5 py-0.5 text-xs font-semibold text-purple-300">
                  <Sparkles className="h-3 w-3" /> Phase 10C Dynamic
                </span>
              </div>
              <p className="mt-1 text-sm text-slate-400">
                Configure operational complaint categories per department with decoupled ML classification
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              onClick={() => navigate("/admin/departments")}
              className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
            >
              <Building2 className="mr-2 h-4 w-4 text-cyan-400" /> Departments
            </Button>
            <Button
              onClick={() => setShowCreateModal(true)}
              className="bg-gradient-to-r from-purple-600 to-indigo-600 font-semibold text-white shadow-lg shadow-purple-600/30 hover:from-purple-500 hover:to-indigo-500"
            >
              <Plus className="mr-2 h-4 w-4" /> Add Category
            </Button>
          </div>
        </div>

        {/* Stats Overview */}
        <div className="mb-8 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium uppercase tracking-wider">Total Categories</span>
              <Layers className="h-4 w-4 text-purple-400" />
            </div>
            <p className="mt-2 text-2xl font-bold text-white">{totalCategories}</p>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium uppercase tracking-wider">Active Categories</span>
              <CheckCircle2 className="h-4 w-4 text-emerald-400" />
            </div>
            <p className="mt-2 text-2xl font-bold text-emerald-300">{activeCategoriesCount}</p>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium uppercase tracking-wider">ML Linked (Fixed 8)</span>
              <Brain className="h-4 w-4 text-indigo-400" />
            </div>
            <p className="mt-2 text-2xl font-bold text-indigo-300">{mlLinkedCount}</p>
          </div>

          <div className="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl">
            <div className="flex items-center justify-between text-slate-400">
              <span className="text-xs font-medium uppercase tracking-wider">Custom (Manual-Only)</span>
              <ShieldCheck className="h-4 w-4 text-amber-400" />
            </div>
            <p className="mt-2 text-2xl font-bold text-amber-300">{customCount}</p>
          </div>
        </div>

        {/* Search & Filter Bar */}
        <div className="mb-6 flex flex-col gap-4 rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl md:flex-row md:items-center md:justify-between">
          <div className="relative flex-1">
            <Search className="absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              type="text"
              placeholder="Search category name, department, ML class, description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="border-white/10 bg-black/20 pl-9 text-white placeholder-slate-500 focus:border-purple-500 focus:ring-purple-500"
            />
          </div>

          <div className="flex flex-wrap items-center gap-3">
            {/* Department Filter */}
            <div className="flex items-center gap-2">
              <Building2 className="h-4 w-4 text-slate-400" />
              <select
                value={deptFilter}
                onChange={(e) => setDeptFilter(e.target.value)}
                className="rounded-xl border border-white/10 bg-slate-900 px-3 py-2 text-xs font-medium text-slate-200 focus:border-purple-500 focus:outline-none"
              >
                <option value="ALL">All Departments</option>
                {departments.map((dept) => (
                  <option key={dept.departmentId} value={dept.departmentId}>
                    {dept.name} ({dept.code})
                  </option>
                ))}
              </select>
            </div>

            {/* Type / Status Filter */}
            <div className="flex items-center gap-1 rounded-xl border border-white/10 bg-black/20 p-1">
              {[
                { label: "All", val: "ALL" },
                { label: "Active", val: "ACTIVE" },
                { label: "Inactive", val: "INACTIVE" },
                { label: "ML Linked", val: "ML_LINKED" },
                { label: "Custom", val: "CUSTOM" },
              ].map((btn) => (
                <button
                  key={btn.val}
                  onClick={() => setStatusFilter(btn.val)}
                  className={`rounded-lg px-2.5 py-1 text-xs font-medium transition ${
                    statusFilter === btn.val
                      ? "bg-purple-600 text-white shadow-md"
                      : "text-slate-400 hover:text-white"
                  }`}
                >
                  {btn.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Categories Table / List */}
        {loading ? (
          <div className="flex h-64 items-center justify-center rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl">
            <div className="flex flex-col items-center gap-3">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-purple-500 border-t-transparent" />
              <p className="text-sm text-slate-400">Loading categories...</p>
            </div>
          </div>
        ) : filteredCategories.length === 0 ? (
          <div className="flex h-64 flex-col items-center justify-center rounded-2xl border border-white/10 bg-white/5 p-6 text-center backdrop-blur-xl">
            <Layers className="h-12 w-12 text-slate-600" />
            <h3 className="mt-3 text-lg font-semibold text-white">No categories found</h3>
            <p className="mt-1 max-w-sm text-xs text-slate-400">
              {searchQuery || deptFilter !== "ALL" || statusFilter !== "ALL"
                ? "Try adjusting your filters or search terms."
                : "Get started by adding your first operational category."}
            </p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl shadow-2xl">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="border-b border-white/10 bg-white/5 text-xs uppercase tracking-wider text-slate-400">
                  <tr>
                    <th className="px-6 py-4">Category Name</th>
                    <th className="px-6 py-4">Department</th>
                    <th className="px-6 py-4">ML Association</th>
                    <th className="px-6 py-4">Display Order</th>
                    <th className="px-6 py-4">Complaints</th>
                    <th className="px-6 py-4">Status</th>
                    <th className="px-6 py-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 font-normal">
                  {filteredCategories.map((cat) => (
                    <tr
                      key={cat.categoryId}
                      className="transition hover:bg-white/5"
                    >
                      {/* Name & Description */}
                      <td className="px-6 py-4">
                        <div className="flex flex-col">
                          <span className="font-semibold text-white">
                            {cat.name}
                          </span>
                          {cat.description && (
                            <span className="mt-0.5 line-clamp-1 text-xs text-slate-400">
                              {cat.description}
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Department */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <span className="rounded-md border border-cyan-500/30 bg-cyan-500/10 px-2 py-0.5 font-mono text-xs text-cyan-300">
                            {cat.departmentCode}
                          </span>
                          <span className="text-xs text-slate-300">
                            {cat.departmentName}
                          </span>
                        </div>
                      </td>

                      {/* ML Association */}
                      <td className="px-6 py-4">
                        {cat.mlClass ? (
                          <div className="inline-flex items-center gap-1.5 rounded-full border border-indigo-400/30 bg-indigo-500/10 px-2.5 py-0.5 text-xs font-semibold text-indigo-300">
                            <Brain className="h-3 w-3" />
                            {cat.mlClass}
                          </div>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full border border-slate-700 bg-slate-800 px-2.5 py-0.5 text-xs text-slate-400">
                            Custom (Manual)
                          </span>
                        )}
                      </td>

                      {/* Display Order */}
                      <td className="px-6 py-4 font-mono text-xs text-slate-300">
                        {cat.displayOrder ?? 0}
                      </td>

                      {/* Complaints */}
                      <td className="px-6 py-4">
                        <span className="inline-flex items-center gap-1 text-xs text-slate-300">
                          <FileText className="h-3.5 w-3.5 text-slate-400" />
                          {cat.complaintCount ?? 0}
                        </span>
                      </td>

                      {/* Status */}
                      <td className="px-6 py-4">
                        {cat.active ? (
                          <span className="inline-flex items-center gap-1 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-0.5 text-xs font-medium text-emerald-300">
                            <CheckCircle2 className="h-3 w-3" /> Active
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full border border-rose-500/30 bg-rose-500/10 px-2.5 py-0.5 text-xs font-medium text-rose-300">
                            <XCircle className="h-3 w-3" /> Inactive
                          </span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleOpenEdit(cat)}
                            className="h-8 border border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                          >
                            <Edit2 className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={actionLoadingId === cat.categoryId}
                            onClick={() => handleToggleStatus(cat)}
                            className={`h-8 border text-xs ${
                              cat.active
                                ? "border-rose-500/30 bg-rose-500/10 text-rose-300 hover:bg-rose-500/20"
                                : "border-emerald-500/30 bg-emerald-500/10 text-emerald-300 hover:bg-emerald-500/20"
                            }`}
                          >
                            {actionLoadingId === cat.categoryId ? (
                              <div className="h-3 w-3 animate-spin rounded-full border-2 border-current border-t-transparent" />
                            ) : cat.active ? (
                              "Deactivate"
                            ) : (
                              "Reactivate"
                            )}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Create Category Modal */}
        <AnimatePresence>
          {showCreateModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowCreateModal(false)}
                className="absolute inset-0 bg-black/70 backdrop-blur-sm"
              />

              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-lg rounded-2xl border border-white/10 bg-slate-900 p-6 shadow-2xl"
              >
                <div className="flex items-center justify-between border-b border-white/10 pb-4">
                  <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-purple-500/20 text-purple-400">
                      <Plus className="h-4 w-4" />
                    </div>
                    <h3 className="text-lg font-bold text-white">Create New Category</h3>
                  </div>
                  <button
                    onClick={() => setShowCreateModal(false)}
                    className="rounded-lg p-1 text-slate-400 hover:bg-white/10 hover:text-white"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                {createError && (
                  <div className="mt-4 flex items-center gap-2 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-xs text-rose-300">
                    <AlertTriangle className="h-4 w-4 shrink-0" />
                    <span>{createError}</span>
                  </div>
                )}

                <form onSubmit={handleCreateCategory} className="mt-4 space-y-4">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Target Department *
                    </label>
                    <select
                      value={createDeptId}
                      onChange={(e) => setCreateDeptId(e.target.value)}
                      required
                      className="mt-1.5 w-full rounded-xl border border-white/10 bg-slate-800 px-3.5 py-2.5 text-sm text-white focus:border-purple-500 focus:outline-none"
                    >
                      <option value="">Select a department...</option>
                      {departments
                        .filter((d) => d.active)
                        .map((d) => (
                          <option key={d.departmentId} value={d.departmentId}>
                            {d.name} ({d.code})
                          </option>
                        ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Category Name *
                    </label>
                    <Input
                      type="text"
                      placeholder="e.g. Tuition Fee Dispute, Lab Equipment Defect"
                      value={createName}
                      onChange={(e) => setCreateName(e.target.value)}
                      required
                      className="mt-1.5 border-white/10 bg-slate-800 text-white placeholder-slate-500"
                    />
                    <p className="mt-1 text-[11px] text-slate-400">
                      Must be unique within the selected department (2–100 chars).
                    </p>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                        Display Order
                      </label>
                      <Input
                        type="number"
                        value={createDisplayOrder}
                        onChange={(e) => setCreateDisplayOrder(e.target.value)}
                        className="mt-1.5 border-white/10 bg-slate-800 text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                        ML Classification
                      </label>
                      <div className="mt-1.5 flex h-10 items-center rounded-xl border border-white/10 bg-slate-800/50 px-3 font-mono text-xs text-slate-400">
                        NULL (Manual-Only)
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Description
                    </label>
                    <textarea
                      rows={3}
                      placeholder="Optional explanation of category scope..."
                      value={createDesc}
                      onChange={(e) => setCreateDesc(e.target.value)}
                      maxLength={500}
                      className="mt-1.5 w-full rounded-xl border border-white/10 bg-slate-800 p-3 text-sm text-white placeholder-slate-500 focus:border-purple-500 focus:outline-none"
                    />
                  </div>

                  <div className="mt-6 flex items-center justify-end gap-3 border-t border-white/10 pt-4">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setShowCreateModal(false)}
                      className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      disabled={submittingCreate}
                      className="bg-gradient-to-r from-purple-600 to-indigo-600 font-semibold text-white hover:from-purple-500 hover:to-indigo-500"
                    >
                      {submittingCreate ? "Creating..." : "Create Category"}
                    </Button>
                  </div>
                </form>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

        {/* Edit Category Modal */}
        <AnimatePresence>
          {showEditModal && editCategory && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowEditModal(false)}
                className="absolute inset-0 bg-black/70 backdrop-blur-sm"
              />

              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-lg rounded-2xl border border-white/10 bg-slate-900 p-6 shadow-2xl"
              >
                <div className="flex items-center justify-between border-b border-white/10 pb-4">
                  <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/20 text-indigo-400">
                      <Edit2 className="h-4 w-4" />
                    </div>
                    <h3 className="text-lg font-bold text-white">
                      Edit Category
                    </h3>
                  </div>
                  <button
                    onClick={() => setShowEditModal(false)}
                    className="rounded-lg p-1 text-slate-400 hover:bg-white/10 hover:text-white"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                {editError && (
                  <div className="mt-4 flex items-center gap-2 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-xs text-rose-300">
                    <AlertTriangle className="h-4 w-4 shrink-0" />
                    <span>{editError}</span>
                  </div>
                )}

                <form onSubmit={handleUpdateCategory} className="mt-4 space-y-4">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                        Department (Immutable)
                      </label>
                      <div className="mt-1.5 flex h-10 items-center rounded-xl border border-white/10 bg-slate-800/60 px-3 text-xs text-slate-300">
                        {editCategory.departmentName} ({editCategory.departmentCode})
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                        ML Class (Immutable)
                      </label>
                      <div className="mt-1.5 flex h-10 items-center rounded-xl border border-white/10 bg-slate-800/60 px-3 text-xs text-slate-300 font-mono">
                        {editCategory.mlClass || "NULL"}
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Category Name *
                    </label>
                    <Input
                      type="text"
                      value={editName}
                      onChange={(e) => setEditName(e.target.value)}
                      required
                      className="mt-1.5 border-white/10 bg-slate-800 text-white placeholder-slate-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Display Order
                    </label>
                    <Input
                      type="number"
                      value={editDisplayOrder}
                      onChange={(e) => setEditDisplayOrder(e.target.value)}
                      className="mt-1.5 border-white/10 bg-slate-800 text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Description
                    </label>
                    <textarea
                      rows={3}
                      value={editDesc}
                      onChange={(e) => setEditDesc(e.target.value)}
                      maxLength={500}
                      className="mt-1.5 w-full rounded-xl border border-white/10 bg-slate-800 p-3 text-sm text-white placeholder-slate-500 focus:border-purple-500 focus:outline-none"
                    />
                  </div>

                  <div className="mt-6 flex items-center justify-end gap-3 border-t border-white/10 pt-4">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setShowEditModal(false)}
                      className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      disabled={submittingEdit}
                      className="bg-gradient-to-r from-indigo-600 to-purple-600 font-semibold text-white hover:from-indigo-500 hover:to-purple-500"
                    >
                      {submittingEdit ? "Saving..." : "Save Changes"}
                    </Button>
                  </div>
                </form>
              </motion.div>
            </div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
