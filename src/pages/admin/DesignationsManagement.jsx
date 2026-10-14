import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  getAllDesignations,
  createDesignation,
  updateDesignation,
  deleteDesignation,
  getStaffDesignations,
  assignStaffDesignations,
  removeStaffDesignation,
} from "@/services/adminDesignationApi";
import { getAllStaff } from "@/services/adminStaffApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import BackButton from "@/components/ui/BackButton";
import {
  Award,
  Search,
  Plus,
  Edit2,
  Trash2,
  ShieldCheck,
  ShieldAlert,
  Users,
  GitBranch,
  Building2,
  Layers,
  MapPin,
  Globe,
  AlertTriangle,
  X,
  CheckCircle2,
  RefreshCw,
  Info,
  Check,
  UserCheck,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";

const SCOPES = [
  {
    value: "GLOBAL",
    label: "Global / Campus-Wide",
    desc: "Applies across all university departments and divisions (e.g. Chief Proctor, Registrar)",
    color: "from-purple-500/20 to-indigo-500/20 text-purple-300 border-purple-400/30",
    badge: "bg-purple-500/15 text-purple-300 border border-purple-400/30",
    icon: Globe,
  },
  {
    value: "DEPARTMENT",
    label: "Departmental",
    desc: "Applies to staff within their assigned business department (e.g. HOD, Dean, Librarian)",
    color: "from-cyan-500/20 to-blue-500/20 text-cyan-300 border-cyan-400/30",
    badge: "bg-cyan-500/15 text-cyan-300 border border-cyan-400/30",
    icon: Building2,
  },
  {
    value: "DIVISION",
    label: "Academic Division",
    desc: "Applies to staff within their academic division (e.g. MFT, Program Coordinator)",
    color: "from-amber-500/20 to-orange-500/20 text-amber-300 border-amber-400/30",
    badge: "bg-amber-500/15 text-amber-300 border border-amber-400/30",
    icon: Layers,
  },
  {
    value: "FLOOR",
    label: "Hostel Floor",
    desc: "Applies to staff assigned to a specific hostel floor (e.g. Warden)",
    color: "from-rose-500/20 to-pink-500/20 text-rose-300 border-rose-400/30",
    badge: "bg-rose-500/15 text-rose-300 border border-rose-400/30",
    icon: MapPin,
  },
];

export default function DesignationsManagement() {
  const navigate = useNavigate();

  // Core Data
  const [designations, setDesignations] = useState([]);
  const [staffList, setStaffList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedScope, setSelectedScope] = useState("ALL");

  // Create Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createName, setCreateName] = useState("");
  const [createScope, setCreateScope] = useState("DEPARTMENT");
  const [createError, setCreateError] = useState("");
  const [submittingCreate, setSubmittingCreate] = useState(false);

  // Edit Modal
  const [showEditModal, setShowEditModal] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [editName, setEditName] = useState("");
  const [editScope, setEditScope] = useState("DEPARTMENT");
  const [editError, setEditError] = useState("");
  const [submittingEdit, setSubmittingEdit] = useState(false);

  // Delete Confirmation Modal
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteError, setDeleteError] = useState("");
  const [submittingDelete, setSubmittingDelete] = useState(false);

  // Staff Assignment Modal / Drawer
  const [showStaffModal, setShowStaffModal] = useState(false);
  const [selectedStaffId, setSelectedStaffId] = useState("");
  const [staffAssignmentData, setStaffAssignmentData] = useState(null);
  const [loadingStaffData, setLoadingStaffData] = useState(false);
  const [selectedDesignationIds, setSelectedDesignationIds] = useState(new Set());
  const [staffModalError, setStaffModalError] = useState("");
  const [submittingStaffAssignment, setSubmittingStaffAssignment] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [desigs, staff] = await Promise.all([
        getAllDesignations(),
        getAllStaff().catch(() => []),
      ]);
      setDesignations(Array.isArray(desigs) ? desigs : []);
      setStaffList(Array.isArray(staff) ? staff : []);
    } catch (err) {
      console.error("Failed to load designations:", err);
      const status = err.response?.status;
      if (status === 403) {
        setError("Access Denied: You must be a platform administrator (ROLE_ADMIN) to view and manage designations.");
      } else if (status === 401) {
        setError("Session expired or unauthorized. Please re-authenticate.");
      } else {
        setError(err.response?.data?.message || "Failed to load designations. Please check your network connection.");
      }
    } finally {
      setLoading(false);
    }
  };

  // ──────────────────────────────────────────────────────────────────────────
  // Create Designation Handler
  // ──────────────────────────────────────────────────────────────────────────
  const handleCreate = async (e) => {
    e.preventDefault();
    setCreateError("");
    const trimmed = createName.trim().toUpperCase();

    if (!trimmed) {
      setCreateError("Designation name is required");
      return;
    }
    if (trimmed.length < 2 || trimmed.length > 100) {
      setCreateError("Designation name must be between 2 and 100 characters");
      return;
    }
    if (trimmed === "ADMIN" || trimmed === "STUDENT") {
      setCreateError(`Cannot create designation with reserved system role name '${trimmed}'`);
      return;
    }

    try {
      setSubmittingCreate(true);
      await createDesignation({
        name: trimmed,
        assignmentScope: createScope,
      });
      toast.success(`Designation '${trimmed}' created successfully`);
      setShowCreateModal(false);
      setCreateName("");
      setCreateScope("DEPARTMENT");
      await loadData();
    } catch (err) {
      console.error("Create failed:", err);
      setCreateError(err.response?.data?.message || "Failed to create designation. Ensure name is unique.");
    } finally {
      setSubmittingCreate(false);
    }
  };

  // ──────────────────────────────────────────────────────────────────────────
  // Edit Designation Handler
  // ──────────────────────────────────────────────────────────────────────────
  const openEditModal = (desig) => {
    const isSystem = desig.systemRole ?? desig.isSystemRole;
    if (isSystem) {
      toast.error("Reserved platform system roles cannot be modified");
      return;
    }
    setEditTarget(desig);
    setEditName(desig.name);
    setEditScope(desig.assignmentScope);
    setEditError("");
    setShowEditModal(true);
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    if (!editTarget) return;
    setEditError("");
    const trimmed = editName.trim().toUpperCase();

    if (!trimmed) {
      setEditError("Designation name is required");
      return;
    }
    if (trimmed.length < 2 || trimmed.length > 100) {
      setEditError("Designation name must be between 2 and 100 characters");
      return;
    }
    if (trimmed === "ADMIN" || trimmed === "STUDENT") {
      setEditError(`Cannot rename designation to reserved system role name '${trimmed}'`);
      return;
    }

    try {
      setSubmittingEdit(true);
      await updateDesignation(editTarget.id, {
        name: trimmed,
        assignmentScope: editScope,
      });
      toast.success(`Designation updated to '${trimmed}'`);
      setShowEditModal(false);
      setEditTarget(null);
      await loadData();
    } catch (err) {
      console.error("Update failed:", err);
      setEditError(err.response?.data?.message || "Failed to update designation");
    } finally {
      setSubmittingEdit(false);
    }
  };

  // ──────────────────────────────────────────────────────────────────────────
  // Delete Designation Handler
  // ──────────────────────────────────────────────────────────────────────────
  const openDeleteModal = (desig) => {
    const isSystem = desig.systemRole ?? desig.isSystemRole;
    if (isSystem) {
      toast.error("Reserved platform system roles cannot be deleted");
      return;
    }
    setDeleteTarget(desig);
    setDeleteError("");
    setShowDeleteModal(true);
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleteError("");

    try {
      setSubmittingDelete(true);
      await deleteDesignation(deleteTarget.id);
      toast.success(`Designation '${deleteTarget.name}' deleted successfully`);
      setShowDeleteModal(false);
      setDeleteTarget(null);
      await loadData();
    } catch (err) {
      console.error("Delete failed:", err);
      setDeleteError(
        err.response?.data?.message ||
        "Failed to delete designation. Active staff or workflow references may prevent deletion."
      );
    } finally {
      setSubmittingDelete(false);
    }
  };

  // ──────────────────────────────────────────────────────────────────────────
  // Staff Designation Assignment Handlers
  // ──────────────────────────────────────────────────────────────────────────
  const handleStaffSelect = async (staffId) => {
    setSelectedStaffId(staffId);
    setStaffAssignmentData(null);
    setStaffModalError("");
    if (!staffId) return;

    try {
      setLoadingStaffData(true);
      const res = await getStaffDesignations(staffId);
      setStaffAssignmentData(res);
      const assignedIds = new Set(
        (res.designations || []).map((d) => d.id)
      );
      setSelectedDesignationIds(assignedIds);
    } catch (err) {
      console.error("Failed to load staff designations:", err);
      setStaffModalError(
        err.response?.data?.message || "Failed to load staff designation details"
      );
    } finally {
      setLoadingStaffData(false);
    }
  };

  const toggleDesignationSelection = (id) => {
    const next = new Set(selectedDesignationIds);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    setSelectedDesignationIds(next);
  };

  const handleSaveStaffAssignments = async () => {
    if (!selectedStaffId) return;
    setStaffModalError("");

    try {
      setSubmittingStaffAssignment(true);
      const res = await assignStaffDesignations(selectedStaffId, {
        designationIds: Array.from(selectedDesignationIds),
      });
      setStaffAssignmentData(res);
      toast.success(
        `Staff designations for '${res.staffName}' updated successfully`
      );
      await loadData();
    } catch (err) {
      console.error("Staff assignment failed:", err);
      setStaffModalError(
        err.response?.data?.message || "Failed to update staff designations"
      );
    } finally {
      setSubmittingStaffAssignment(false);
    }
  };

  const handleRemoveSingleDesignation = async (designationId, desigName) => {
    if (!selectedStaffId) return;
    try {
      const res = await removeStaffDesignation(selectedStaffId, designationId);
      setStaffAssignmentData(res);
      const assignedIds = new Set(
        (res.designations || []).map((d) => d.id)
      );
      setSelectedDesignationIds(assignedIds);
      toast.success(`Removed '${desigName}' from staff member`);
      await loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || "Failed to remove designation");
    }
  };

  // ──────────────────────────────────────────────────────────────────────────
  // Filtered List & Counts
  // ──────────────────────────────────────────────────────────────────────────
  const filteredDesignations = designations.filter((d) => {
    const matchesSearch =
      d.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.assignmentScope?.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesScope =
      selectedScope === "ALL" || d.assignmentScope === selectedScope;
    return matchesSearch && matchesScope;
  });

  const totalSystemRoles = designations.filter(
    (d) => d.systemRole || d.isSystemRole
  ).length;
  const totalStaffAssignments = designations.reduce(
    (acc, d) => acc + (d.assignedStaffCount || 0),
    0
  );
  const totalWorkflowSteps = designations.reduce(
    (acc, d) => acc + (d.workflowStepCount || 0),
    0
  );

  const selectedStaffMember = staffList.find(
    (s) => String(s.staffId) === String(selectedStaffId)
  );

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white p-6">
      {/* Background Gradients */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)] pointer-events-none" />
      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/15 blur-3xl pointer-events-none" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/15 blur-3xl pointer-events-none" />

      <div className="relative max-w-7xl mx-auto space-y-6">
        {/* Navigation & Header */}
        <BackButton fallback="/admin-dashboard" />

        <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 shadow-2xl">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
            <div>
              <div className="flex items-center gap-3">
                <div className="h-12 w-12 rounded-2xl bg-gradient-to-tr from-amber-500 via-rose-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-rose-500/20">
                  <Award className="h-6 w-6 text-white" />
                </div>
                <div>
                  <h1 className="text-3xl font-black tracking-tight text-white">
                    Designations & Workflow Roles
                  </h1>
                  <p className="text-slate-400 text-sm mt-0.5">
                    Configure university business designations, operational scopes & multi-role staff assignments
                  </p>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3 flex-wrap">
              <Button
                variant="outline"
                onClick={() => {
                  setShowStaffModal(true);
                  if (staffList.length > 0 && !selectedStaffId) {
                    handleStaffSelect(staffList[0].staffId);
                  }
                }}
                className="rounded-xl border-white/15 bg-white/5 hover:bg-white/10 text-white"
              >
                <UserCheck className="h-4 w-4 mr-2 text-indigo-400" />
                Staff Role Assignments
              </Button>

              <Button
                onClick={() => setShowCreateModal(true)}
                className="rounded-xl bg-gradient-to-r from-indigo-600 via-indigo-500 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white shadow-lg shadow-indigo-500/25 font-semibold"
              >
                <Plus className="h-4 w-4 mr-2" />
                New Designation
              </Button>

              <Button
                variant="ghost"
                size="icon"
                onClick={loadData}
                disabled={loading}
                className="rounded-xl text-slate-400 hover:text-white hover:bg-white/10"
                title="Refresh"
              >
                <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin text-indigo-400" : ""}`} />
              </Button>
            </div>
          </div>

          {/* Quick Stats Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-white/10">
            <div className="rounded-2xl border border-white/5 bg-slate-900/40 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Total Roles</span>
                <Award className="h-4 w-4 text-indigo-400" />
              </div>
              <p className="text-2xl font-bold text-white mt-1">{designations.length}</p>
            </div>

            <div className="rounded-2xl border border-white/5 bg-slate-900/40 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Protected System</span>
                <ShieldCheck className="h-4 w-4 text-amber-400" />
              </div>
              <p className="text-2xl font-bold text-amber-300 mt-1">{totalSystemRoles}</p>
            </div>

            <div className="rounded-2xl border border-white/5 bg-slate-900/40 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Staff Assigned</span>
                <Users className="h-4 w-4 text-cyan-400" />
              </div>
              <p className="text-2xl font-bold text-cyan-300 mt-1">{totalStaffAssignments}</p>
            </div>

            <div className="rounded-2xl border border-white/5 bg-slate-900/40 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">Workflow Steps</span>
                <GitBranch className="h-4 w-4 text-emerald-400" />
              </div>
              <p className="text-2xl font-bold text-emerald-300 mt-1">{totalWorkflowSteps}</p>
            </div>
          </div>
        </div>

        {/* Error State Banner */}
        {error && (
          <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-5 flex items-start gap-4 text-rose-200">
            <AlertTriangle className="h-6 w-6 text-rose-400 shrink-0 mt-0.5" />
            <div className="flex-1">
              <h4 className="font-semibold text-rose-300">Authorization / Network Notice</h4>
              <p className="text-sm mt-1">{error}</p>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={loadData}
              className="border-rose-400/30 text-rose-300 hover:bg-rose-500/20"
            >
              Retry
            </Button>
          </div>
        )}

        {/* Filters & Search */}
        <div className="rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              type="text"
              placeholder="Search by name or scope..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10 rounded-xl bg-slate-900/60 border-white/10 text-white placeholder:text-slate-500 focus:border-indigo-500"
            />
          </div>

          <div className="flex items-center gap-1.5 flex-wrap w-full md:w-auto">
            <button
              onClick={() => setSelectedScope("ALL")}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                selectedScope === "ALL"
                  ? "bg-indigo-600 text-white shadow-md shadow-indigo-500/25"
                  : "bg-white/5 text-slate-400 hover:text-white hover:bg-white/10"
              }`}
            >
              All Scopes ({designations.length})
            </button>
            {SCOPES.map((s) => {
              const count = designations.filter((d) => d.assignmentScope === s.value).length;
              return (
                <button
                  key={s.value}
                  onClick={() => setSelectedScope(s.value)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                    selectedScope === s.value
                      ? "bg-indigo-600 text-white shadow-md shadow-indigo-500/25"
                      : "bg-white/5 text-slate-400 hover:text-white hover:bg-white/10"
                  }`}
                >
                  {s.value} ({count})
                </button>
              );
            })}
          </div>
        </div>

        {/* Table / List View */}
        <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl overflow-hidden shadow-2xl">
          {loading ? (
            <div className="p-16 flex flex-col items-center justify-center gap-4 text-slate-400">
              <RefreshCw className="h-8 w-8 animate-spin text-indigo-400" />
              <p className="text-sm font-medium">Loading university designations & roles...</p>
            </div>
          ) : filteredDesignations.length === 0 ? (
            <div className="p-16 text-center">
              <div className="h-16 w-16 mx-auto rounded-full bg-white/5 border border-white/10 flex items-center justify-center text-slate-500 mb-4">
                <Award className="h-8 w-8" />
              </div>
              <h3 className="text-lg font-semibold text-white">No designations found</h3>
              <p className="text-sm text-slate-400 mt-1 max-w-sm mx-auto">
                {searchQuery || selectedScope !== "ALL"
                  ? "No designations match your current search or scope filter criteria."
                  : "No designations are currently configured. Click 'New Designation' to create one."}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-white/10 bg-slate-900/60 text-slate-400 text-xs uppercase tracking-wider">
                    <th className="py-4 px-6">ID</th>
                    <th className="py-4 px-6">Designation Name</th>
                    <th className="py-4 px-6">Assignment Scope</th>
                    <th className="py-4 px-6 text-center">Staff Assigned</th>
                    <th className="py-4 px-6 text-center">Workflow References</th>
                    <th className="py-4 px-6 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5 text-slate-200">
                  {filteredDesignations.map((desig) => {
                    const isSystem = desig.systemRole ?? desig.isSystemRole;
                    const scopeMeta = SCOPES.find((s) => s.value === desig.assignmentScope);

                    return (
                      <tr
                        key={desig.id}
                        className="hover:bg-white/[0.02] transition-colors group"
                      >
                        <td className="py-4 px-6 font-mono text-xs text-slate-500">
                          #{desig.id}
                        </td>

                        <td className="py-4 px-6">
                          <div className="flex items-center gap-2.5">
                            <span className="font-bold text-white text-base">
                              {desig.name}
                            </span>
                            {isSystem && (
                              <span
                                className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/15 text-amber-300 border border-amber-400/30"
                                title="Reserved platform system role (immutable)"
                              >
                                <ShieldCheck className="h-3 w-3" />
                                Protected System Role
                              </span>
                            )}
                          </div>
                        </td>

                        <td className="py-4 px-6">
                          <span
                            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${
                              scopeMeta?.badge || "bg-slate-800 text-slate-300"
                            }`}
                          >
                            {scopeMeta && <scopeMeta.icon className="h-3 w-3" />}
                            {desig.assignmentScope}
                          </span>
                        </td>

                        <td className="py-4 px-6 text-center">
                          <span
                            className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold ${
                              desig.assignedStaffCount > 0
                                ? "bg-cyan-500/10 text-cyan-300 border border-cyan-500/20"
                                : "text-slate-500"
                            }`}
                          >
                            <Users className="h-3 w-3" />
                            {desig.assignedStaffCount}
                          </span>
                        </td>

                        <td className="py-4 px-6 text-center">
                          <span
                            className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold ${
                              desig.workflowStepCount > 0
                                ? "bg-emerald-500/10 text-emerald-300 border border-emerald-500/20"
                                : "text-slate-500"
                            }`}
                          >
                            <GitBranch className="h-3 w-3" />
                            {desig.workflowStepCount}
                          </span>
                        </td>

                        <td className="py-4 px-6 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {isSystem ? (
                              <span className="text-xs text-slate-500 italic px-2">
                                System Immutable
                              </span>
                            ) : (
                              <>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => openEditModal(desig)}
                                  className="h-8 w-8 p-0 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg"
                                  title="Edit Designation"
                                >
                                  <Edit2 className="h-4 w-4" />
                                </Button>

                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => openDeleteModal(desig)}
                                  className="h-8 w-8 p-0 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 rounded-lg"
                                  title="Delete Designation"
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* ────────────────────────────────────────────────────────────────────────── */}
      {/* CREATE DESIGNATION MODAL */}
      {/* ────────────────────────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {showCreateModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="relative w-full max-w-lg rounded-3xl border border-white/15 bg-slate-900/95 p-6 shadow-2xl text-white"
            >
              <div className="flex items-center justify-between pb-4 border-b border-white/10">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
                    <Plus className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold">New Designation</h3>
                    <p className="text-xs text-slate-400">Add an organizational workflow role</p>
                  </div>
                </div>
                <button
                  onClick={() => setShowCreateModal(false)}
                  className="rounded-lg p-1.5 text-slate-400 hover:text-white hover:bg-white/10"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {createError && (
                <div className="mt-4 p-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-200 text-xs flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
                  <span>{createError}</span>
                </div>
              )}

              <form onSubmit={handleCreate} className="mt-5 space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Designation Name <span className="text-rose-400">*</span>
                  </label>
                  <Input
                    type="text"
                    required
                    placeholder="e.g. LAB_COORDINATOR, PROCTOR, DEAN"
                    value={createName}
                    onChange={(e) => setCreateName(e.target.value.toUpperCase())}
                    className="rounded-xl bg-slate-950 border-white/10 text-white placeholder:text-slate-600 focus:border-indigo-500"
                  />
                  <p className="text-xs text-slate-500 mt-1">
                    Uppercase alphanumeric & underscores. Reserved names 'ADMIN' and 'STUDENT' are forbidden.
                  </p>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Assignment Scope <span className="text-rose-400">*</span>
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    {SCOPES.map((s) => {
                      const isSelected = createScope === s.value;
                      return (
                        <div
                          key={s.value}
                          onClick={() => setCreateScope(s.value)}
                          className={`cursor-pointer p-3 rounded-xl border transition-all ${
                            isSelected
                              ? "border-indigo-500 bg-indigo-500/15 shadow-md shadow-indigo-500/20"
                              : "border-white/10 bg-slate-950/60 hover:bg-white/5"
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <s.icon className={`h-4 w-4 ${isSelected ? "text-indigo-400" : "text-slate-400"}`} />
                            <span className="text-xs font-bold text-white">{s.value}</span>
                          </div>
                          <p className="text-[11px] text-slate-400 mt-1 line-clamp-2 leading-tight">
                            {s.label}
                          </p>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/10">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setShowCreateModal(false)}
                    className="text-slate-400 hover:text-white"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={submittingCreate}
                    className="rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white px-6 font-semibold"
                  >
                    {submittingCreate ? (
                      <>
                        <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                        Creating...
                      </>
                    ) : (
                      "Create Designation"
                    )}
                  </Button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* ────────────────────────────────────────────────────────────────────────── */}
      {/* EDIT DESIGNATION MODAL */}
      {/* ────────────────────────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {showEditModal && editTarget && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="relative w-full max-w-lg rounded-3xl border border-white/15 bg-slate-900/95 p-6 shadow-2xl text-white"
            >
              <div className="flex items-center justify-between pb-4 border-b border-white/10">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
                    <Edit2 className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold">Edit Designation #{editTarget.id}</h3>
                    <p className="text-xs text-slate-400">Modify designation properties & scope</p>
                  </div>
                </div>
                <button
                  onClick={() => setShowEditModal(false)}
                  className="rounded-lg p-1.5 text-slate-400 hover:text-white hover:bg-white/10"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {editError && (
                <div className="mt-4 p-3 rounded-xl border border-rose-500/30 bg-rose-500/10 text-rose-200 text-xs flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0" />
                  <span>{editError}</span>
                </div>
              )}

              <form onSubmit={handleUpdate} className="mt-5 space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Designation Name <span className="text-rose-400">*</span>
                  </label>
                  <Input
                    type="text"
                    required
                    value={editName}
                    onChange={(e) => setEditName(e.target.value.toUpperCase())}
                    className="rounded-xl bg-slate-950 border-white/10 text-white placeholder:text-slate-600 focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Assignment Scope <span className="text-rose-400">*</span>
                  </label>
                  <div className="grid grid-cols-2 gap-2">
                    {SCOPES.map((s) => {
                      const isSelected = editScope === s.value;
                      return (
                        <div
                          key={s.value}
                          onClick={() => setEditScope(s.value)}
                          className={`cursor-pointer p-3 rounded-xl border transition-all ${
                            isSelected
                              ? "border-indigo-500 bg-indigo-500/15 shadow-md shadow-indigo-500/20"
                              : "border-white/10 bg-slate-950/60 hover:bg-white/5"
                          }`}
                        >
                          <div className="flex items-center gap-2">
                            <s.icon className={`h-4 w-4 ${isSelected ? "text-indigo-400" : "text-slate-400"}`} />
                            <span className="text-xs font-bold text-white">{s.value}</span>
                          </div>
                          <p className="text-[11px] text-slate-400 mt-1 line-clamp-2 leading-tight">
                            {s.label}
                          </p>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="flex items-center justify-end gap-3 pt-4 border-t border-white/10">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setShowEditModal(false)}
                    className="text-slate-400 hover:text-white"
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    disabled={submittingEdit}
                    className="rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white px-6 font-semibold"
                  >
                    {submittingEdit ? (
                      <>
                        <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                        Saving...
                      </>
                    ) : (
                      "Save Changes"
                    )}
                  </Button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* ────────────────────────────────────────────────────────────────────────── */}
      {/* DELETE CONFIRMATION MODAL */}
      {/* ────────────────────────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {showDeleteModal && deleteTarget && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="relative w-full max-w-md rounded-3xl border border-rose-500/20 bg-slate-900/95 p-6 shadow-2xl text-white"
            >
              <div className="flex items-center gap-3 pb-4 border-b border-white/10">
                <div className="h-10 w-10 rounded-xl bg-rose-500/20 text-rose-400 flex items-center justify-center">
                  <Trash2 className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-white">Delete Designation</h3>
                  <p className="text-xs text-slate-400">Confirm deletion of '{deleteTarget.name}'</p>
                </div>
              </div>

              {deleteError && (
                <div className="mt-4 p-3.5 rounded-xl border border-rose-500/30 bg-rose-500/15 text-rose-200 text-xs flex items-start gap-2.5">
                  <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0 mt-0.5" />
                  <span>{deleteError}</span>
                </div>
              )}

              <div className="mt-4 space-y-3">
                <p className="text-sm text-slate-300">
                  Are you sure you want to permanently delete designation{" "}
                  <strong className="text-white font-mono bg-white/10 px-1.5 py-0.5 rounded">
                    {deleteTarget.name}
                  </strong>{" "}
                  (Scope: {deleteTarget.assignmentScope})?
                </p>

                {(deleteTarget.assignedStaffCount > 0 || deleteTarget.workflowStepCount > 0) && (
                  <div className="p-3.5 rounded-xl border border-amber-500/30 bg-amber-500/10 text-amber-200 text-xs space-y-1">
                    <div className="flex items-center gap-1.5 font-bold text-amber-300">
                      <AlertTriangle className="h-4 w-4" />
                      Active References Warning:
                    </div>
                    <p>
                      This role is currently assigned to{" "}
                      <strong>{deleteTarget.assignedStaffCount} staff member(s)</strong> and referenced by{" "}
                      <strong>{deleteTarget.workflowStepCount} workflow step(s)</strong>.
                    </p>
                    <p className="text-slate-400 text-[11px]">
                      The backend referential integrity guardrails will reject deletion until all staff assignments and workflow bindings are removed.
                    </p>
                  </div>
                )}
              </div>

              <div className="flex items-center justify-end gap-3 mt-6 pt-4 border-t border-white/10">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setShowDeleteModal(false)}
                  className="text-slate-400 hover:text-white"
                >
                  Cancel
                </Button>
                <Button
                  onClick={handleDelete}
                  disabled={submittingDelete}
                  className="rounded-xl bg-rose-600 hover:bg-rose-500 text-white px-5 font-semibold"
                >
                  {submittingDelete ? (
                    <>
                      <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                      Deleting...
                    </>
                  ) : (
                    "Delete Designation"
                  )}
                </Button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* ────────────────────────────────────────────────────────────────────────── */}
      {/* STAFF DESIGNATION ASSIGNMENT MODAL / DRAWER */}
      {/* ────────────────────────────────────────────────────────────────────────── */}
      <AnimatePresence>
        {showStaffModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="relative w-full max-w-2xl max-h-[90vh] overflow-hidden rounded-3xl border border-white/15 bg-slate-900/95 shadow-2xl text-white flex flex-col"
            >
              {/* Modal Header */}
              <div className="p-6 pb-4 border-b border-white/10 flex items-center justify-between shrink-0">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
                    <UserCheck className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold">Staff Role & Designation Assignments</h3>
                    <p className="text-xs text-slate-400">
                      Manage multiple concurrent designations without overwriting system administrator identity
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setShowStaffModal(false)}
                  className="rounded-lg p-1.5 text-slate-400 hover:text-white hover:bg-white/10"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {/* Modal Body */}
              <div className="p-6 overflow-y-auto space-y-5 flex-1">
                {/* Staff Member Selector */}
                <div>
                  <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                    Select Staff Member <span className="text-rose-400">*</span>
                  </label>
                  <select
                    value={selectedStaffId}
                    onChange={(e) => handleStaffSelect(e.target.value)}
                    className="w-full rounded-xl bg-slate-950 border border-white/15 text-white px-3.5 py-2.5 text-sm focus:border-indigo-500 focus:outline-none"
                  >
                    <option value="">-- Choose a staff member --</option>
                    {staffList.map((st) => (
                      <option key={st.staffId} value={st.staffId}>
                        {st.name} ({st.email}) — Dept: {st.department?.name || "None"}
                      </option>
                    ))}
                  </select>
                </div>

                {staffModalError && (
                  <div className="p-3.5 rounded-xl border border-rose-500/30 bg-rose-500/15 text-rose-200 text-xs flex items-start gap-2.5">
                    <AlertTriangle className="h-4 w-4 text-rose-400 shrink-0 mt-0.5" />
                    <span>{staffModalError}</span>
                  </div>
                )}

                {loadingStaffData ? (
                  <div className="py-12 flex flex-col items-center justify-center gap-3 text-slate-400">
                    <RefreshCw className="h-6 w-6 animate-spin text-indigo-400" />
                    <p className="text-xs">Loading staff assignment profile...</p>
                  </div>
                ) : staffAssignmentData ? (
                  <>
                    {/* Staff Profile Card & System Role Indicator */}
                    <div className="rounded-2xl border border-white/10 bg-slate-950/70 p-4 space-y-3">
                      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                        <div>
                          <h4 className="font-bold text-white text-base">
                            {staffAssignmentData.staffName}
                          </h4>
                          <p className="text-xs text-slate-400">{staffAssignmentData.email}</p>
                        </div>

                        {staffAssignmentData.platformAdmin && (
                          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-500/20 text-amber-300 border border-amber-400/40 shadow-sm shadow-amber-500/10">
                            <ShieldCheck className="h-3.5 w-3.5 text-amber-400" />
                            Platform Administrator (Role ID 1)
                          </span>
                        )}
                      </div>

                      {/* Staff Attributes Pills */}
                      <div className="grid grid-cols-3 gap-2 pt-2 border-t border-white/5 text-[11px]">
                        <div>
                          <span className="text-slate-500 block uppercase font-mono">Department:</span>
                          <span className="text-slate-300 font-medium">
                            {selectedStaffMember?.department?.name || "Not Assigned"}
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-500 block uppercase font-mono">Division:</span>
                          <span className="text-slate-300 font-medium">
                            {selectedStaffMember?.academicDivision?.name || "Not Assigned"}
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-500 block uppercase font-mono">Floor:</span>
                          <span className="text-slate-300 font-medium">
                            {selectedStaffMember?.floor ? `Floor ${selectedStaffMember.floor.floorNumber || selectedStaffMember.floor.floorId}` : "Not Assigned"}
                          </span>
                        </div>
                      </div>

                      {/* Currently Active Assigned Designations */}
                      <div className="pt-2">
                        <span className="text-xs font-semibold text-slate-400 block mb-1.5">
                          Currently Active Designations:
                        </span>
                        {(!staffAssignmentData.designations || staffAssignmentData.designations.length === 0) ? (
                          <p className="text-xs text-slate-500 italic">No business designations assigned.</p>
                        ) : (
                          <div className="flex flex-wrap gap-1.5">
                            {staffAssignmentData.designations.map((d) => (
                              <span
                                key={d.id}
                                className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-500/15 text-indigo-300 border border-indigo-500/30"
                              >
                                {d.name} ({d.assignmentScope})
                                <button
                                  type="button"
                                  onClick={() => handleRemoveSingleDesignation(d.id, d.name)}
                                  className="text-slate-400 hover:text-rose-400 ml-0.5"
                                  title="Remove this designation"
                                >
                                  <X className="h-3 w-3" />
                                </button>
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Multi-Select Designation Checkboxes */}
                    <div>
                      <h4 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                        Assign / Update University Designations:
                      </h4>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                        {designations
                          .filter((d) => !(d.systemRole || d.isSystemRole))
                          .map((desig) => {
                            const isChecked = selectedDesignationIds.has(desig.id);
                            const scopeMeta = SCOPES.find((s) => s.value === desig.assignmentScope);

                            // Check Scope Feasibility
                            let scopeWarning = null;
                            if (desig.assignmentScope === "DEPARTMENT" && !selectedStaffMember?.department) {
                              scopeWarning = "Staff missing Department";
                            } else if (desig.assignmentScope === "DIVISION" && !selectedStaffMember?.academicDivision) {
                              scopeWarning = "Staff missing Division";
                            } else if (desig.assignmentScope === "FLOOR" && !selectedStaffMember?.floor) {
                              scopeWarning = "Staff missing Floor";
                            }

                            return (
                              <div
                                key={desig.id}
                                onClick={() => toggleDesignationSelection(desig.id)}
                                className={`cursor-pointer p-3 rounded-2xl border transition-all flex items-start justify-between gap-3 ${
                                  isChecked
                                    ? "bg-indigo-600/15 border-indigo-500/50 shadow-sm shadow-indigo-500/15"
                                    : "bg-slate-950/40 border-white/10 hover:border-white/20"
                                }`}
                              >
                                <div className="space-y-1">
                                  <div className="flex items-center gap-2">
                                    <span className="font-bold text-sm text-white">{desig.name}</span>
                                    <span
                                      className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                                        scopeMeta?.badge || "bg-slate-800 text-slate-300"
                                      }`}
                                    >
                                      {desig.assignmentScope}
                                    </span>
                                  </div>
                                  {scopeWarning && (
                                    <span className="inline-flex items-center gap-1 text-[11px] text-amber-400 font-medium">
                                      <AlertTriangle className="h-3 w-3" />
                                      {scopeWarning}
                                    </span>
                                  )}
                                </div>

                                <div
                                  className={`h-5 w-5 rounded-md border flex items-center justify-center shrink-0 transition-colors ${
                                    isChecked
                                      ? "bg-indigo-600 border-indigo-500 text-white"
                                      : "border-white/20 bg-slate-900"
                                  }`}
                                >
                                  {isChecked && <Check className="h-3.5 w-3.5 stroke-[3]" />}
                                </div>
                              </div>
                            );
                          })}
                      </div>
                    </div>
                  </>
                ) : (
                  <div className="py-12 text-center text-slate-500 text-sm">
                    Select a staff member above to inspect and assign workflow designations.
                  </div>
                )}
              </div>

              {/* Modal Footer */}
              <div className="p-6 pt-4 border-t border-white/10 flex items-center justify-end gap-3 shrink-0">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setShowStaffModal(false)}
                  className="text-slate-400 hover:text-white"
                >
                  Close
                </Button>
                {staffAssignmentData && (
                  <Button
                    onClick={handleSaveStaffAssignments}
                    disabled={submittingStaffAssignment}
                    className="rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white px-6 font-semibold shadow-lg shadow-indigo-500/25"
                  >
                    {submittingStaffAssignment ? (
                      <>
                        <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                        Saving Assignments...
                      </>
                    ) : (
                      "Save Assignments"
                    )}
                  </Button>
                )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
