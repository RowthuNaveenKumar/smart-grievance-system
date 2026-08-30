import React, { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  getWorkflowsByDepartment,
  createWorkflowVersion,
  addWorkflowStep,
  updateWorkflowStep,
  deleteWorkflowStep,
  activateWorkflow,
  getAllRoles,
} from "@/services/adminWorkflowApi";
import { getAllAdminDepartments } from "@/services/adminDepartmentApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  GitBranch,
  Building2,
  Plus,
  Edit2,
  Trash2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  ShieldCheck,
  Sparkles,
  Lock,
  Unlock,
  Clock,
  UserCheck,
  Layers,
  FileText,
  X,
} from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";

export default function WorkflowsManagement() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialDeptId = searchParams.get("departmentId") || "";

  const [departments, setDepartments] = useState([]);
  const [selectedDeptId, setSelectedDeptId] = useState(initialDeptId);
  const [workflows, setWorkflows] = useState([]);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [roles, setRoles] = useState([]);
  const [loadingDepts, setLoadingDepts] = useState(true);
  const [loadingWorkflows, setLoadingWorkflows] = useState(false);

  // Modals
  const [showCreateVersionModal, setShowCreateVersionModal] = useState(false);
  const [versionName, setVersionName] = useState("");
  const [creatingVersion, setCreatingVersion] = useState(false);

  const [showStepModal, setShowStepModal] = useState(false);
  const [editingStep, setEditingStep] = useState(null);
  const [stepLevel, setStepLevel] = useState("1");
  const [stepRoleId, setStepRoleId] = useState("");
  const [stepSla, setStepSla] = useState("24");
  const [stepError, setStepError] = useState("");
  const [submittingStep, setSubmittingStep] = useState(false);

  const [showActivateModal, setShowActivateModal] = useState(false);
  const [activating, setActivating] = useState(false);
  const [activateError, setActivateError] = useState("");

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedDeptId) {
      loadWorkflows(selectedDeptId);
    } else {
      setWorkflows([]);
      setSelectedWorkflow(null);
    }
  }, [selectedDeptId]);

  const loadInitialData = async () => {
    try {
      setLoadingDepts(true);
      const [deptsData, rolesData] = await Promise.all([
        getAllAdminDepartments(),
        getAllRoles(),
      ]);
      setDepartments(deptsData || []);
      setRoles(rolesData || []);

      if (deptsData && deptsData.length > 0) {
        const defaultDept = initialDeptId
          ? deptsData.find((d) => String(d.departmentId) === String(initialDeptId)) || deptsData[0]
          : deptsData[0];
        setSelectedDeptId(String(defaultDept.departmentId));
      }
    } catch (err) {
      console.error("Failed to load initial workflow data:", err);
    } finally {
      setLoadingDepts(false);
    }
  };

  const loadWorkflows = async (deptId) => {
    try {
      setLoadingWorkflows(true);
      const data = await getWorkflowsByDepartment(deptId);
      setWorkflows(data || []);
      if (data && data.length > 0) {
        // Default select the active workflow, or the first one
        const activeWf = data.find((w) => w.active) || data[0];
        setSelectedWorkflow(activeWf);
      } else {
        setSelectedWorkflow(null);
      }
    } catch (err) {
      console.error("Failed to load workflows for dept:", err);
      setWorkflows([]);
      setSelectedWorkflow(null);
    } finally {
      setLoadingWorkflows(false);
    }
  };

  const handleSelectDepartment = (deptId) => {
    setSelectedDeptId(deptId);
    setSearchParams({ departmentId: deptId });
  };

  const handleCreateVersion = async (e) => {
    e.preventDefault();
    try {
      setCreatingVersion(true);
      const res = await createWorkflowVersion(Number(selectedDeptId), {
        name: versionName.trim() || null,
      });
      setShowCreateVersionModal(false);
      setVersionName("");
      await loadWorkflows(selectedDeptId);
      // Select newly created workflow
      if (res && res.workflowId) {
        setSelectedWorkflow(res);
      }
    } catch (err) {
      alert(err.response?.data?.message || "Failed to create workflow version");
    } finally {
      setCreatingVersion(false);
    }
  };

  const handleOpenAddStep = () => {
    setEditingStep(null);
    const nextLevel = (selectedWorkflow?.steps?.length || 0) + 1;
    setStepLevel(String(nextLevel));
    setStepRoleId(roles.length > 0 ? String(roles[0].roleId) : "");
    setStepSla("24");
    setStepError("");
    setShowStepModal(true);
  };

  const handleOpenEditStep = (step) => {
    setEditingStep(step);
    setStepLevel(String(step.level));
    setStepRoleId(String(step.roleId));
    setStepSla(String(step.resolutionTimeHours));
    setStepError("");
    setShowStepModal(true);
  };

  const handleSaveStep = async (e) => {
    e.preventDefault();
    setStepError("");

    const level = parseInt(stepLevel, 10);
    const sla = parseInt(stepSla, 10);
    const roleId = Number(stepRoleId);

    if (!level || level < 1) {
      setStepError("Level must be an integer >= 1");
      return;
    }
    if (!sla || sla < 1) {
      setStepError("Resolution SLA must be at least 1 hour");
      return;
    }
    if (!roleId) {
      setStepError("Please select a valid role");
      return;
    }

    try {
      setSubmittingStep(true);
      if (editingStep) {
        await updateWorkflowStep(editingStep.stepId, {
          level,
          roleId,
          resolutionTimeHours: sla,
        });
      } else {
        await addWorkflowStep(selectedWorkflow.workflowId, {
          level,
          roleId,
          resolutionTimeHours: sla,
        });
      }
      setShowStepModal(false);
      // Refresh current workflow
      const updatedList = await getWorkflowsByDepartment(selectedDeptId);
      setWorkflows(updatedList || []);
      const current = updatedList.find((w) => w.workflowId === selectedWorkflow.workflowId);
      if (current) setSelectedWorkflow(current);
    } catch (err) {
      setStepError(err.response?.data?.message || "Failed to save workflow step");
    } finally {
      setSubmittingStep(false);
    }
  };

  const handleDeleteStep = async (stepId) => {
    if (!window.confirm("Are you sure you want to delete this workflow step?")) return;
    try {
      await deleteWorkflowStep(stepId);
      const updatedList = await getWorkflowsByDepartment(selectedDeptId);
      setWorkflows(updatedList || []);
      const current = updatedList.find((w) => w.workflowId === selectedWorkflow.workflowId);
      if (current) setSelectedWorkflow(current);
    } catch (err) {
      alert(err.response?.data?.message || "Failed to delete workflow step");
    }
  };

  const handleActivate = async () => {
    setActivateError("");
    try {
      setActivating(true);
      const activated = await activateWorkflow(selectedWorkflow.workflowId);
      setShowActivateModal(false);
      await loadWorkflows(selectedDeptId);
      setSelectedWorkflow(activated);
    } catch (err) {
      setActivateError(
        err.response?.data?.message || "Activation failed. Ensure steps start at 1 and are contiguous.",
      );
    } finally {
      setActivating(false);
    }
  };

  const currentDept = departments.find((d) => String(d.departmentId) === String(selectedDeptId));

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Background Glows */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_30%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_35%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-cyan-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-purple-400/10 blur-3xl animate-pulse" />

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
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 via-blue-500 to-indigo-500 shadow-lg shadow-cyan-500/30">
                  <GitBranch className="h-5 w-5 text-white" />
                </div>
                <h1 className="text-2xl font-bold tracking-tight text-white sm:text-3xl">
                  Workflow Management
                </h1>
                <span className="flex items-center gap-1 rounded-full border border-cyan-400/30 bg-cyan-500/10 px-2.5 py-0.5 text-xs font-semibold text-cyan-300">
                  <Sparkles className="h-3 w-3" /> Phase 10D Versioning
                </span>
              </div>
              <p className="mt-1 text-sm text-slate-400">
                Configure hierarchical multi-level grievance routing, SLAs, and immutable version lifecycle
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
              variant="outline"
              onClick={() => navigate("/admin/categories")}
              className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
            >
              <Layers className="mr-2 h-4 w-4 text-purple-400" /> Categories
            </Button>
          </div>
        </div>

        {/* Department Selector Bar */}
        <div className="mb-8 rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <Building2 className="h-5 w-5 text-cyan-400" />
              <label className="text-sm font-semibold text-slate-300">
                Select Department:
              </label>
              <select
                value={selectedDeptId}
                onChange={(e) => handleSelectDepartment(e.target.value)}
                className="rounded-xl border border-white/10 bg-slate-900 px-4 py-2 text-sm font-medium text-white focus:border-cyan-500 focus:outline-none"
              >
                {departments.map((dept) => (
                  <option key={dept.departmentId} value={dept.departmentId}>
                    {dept.name} ({dept.code}) {!dept.active ? "[Inactive]" : ""}
                  </option>
                ))}
              </select>
            </div>

            {currentDept && (
              <div className="flex items-center gap-2">
                <Button
                  onClick={() => setShowCreateVersionModal(true)}
                  disabled={!currentDept.active}
                  className="bg-gradient-to-r from-cyan-500 via-blue-600 to-indigo-600 font-semibold text-white shadow-lg shadow-cyan-500/20 hover:from-cyan-400 hover:to-indigo-500"
                >
                  <Plus className="mr-2 h-4 w-4" /> Create New Version
                </Button>
              </div>
            )}
          </div>
        </div>

        {/* Workflow Versions Tabs & Details */}
        {loadingWorkflows ? (
          <div className="flex h-64 items-center justify-center rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl">
            <div className="flex flex-col items-center gap-3">
              <div className="h-8 w-8 animate-spin rounded-full border-2 border-cyan-500 border-t-transparent" />
              <p className="text-sm text-slate-400">Loading department workflows...</p>
            </div>
          </div>
        ) : workflows.length === 0 ? (
          <div className="flex h-64 flex-col items-center justify-center rounded-2xl border border-white/10 bg-white/5 p-6 text-center backdrop-blur-xl">
            <GitBranch className="h-12 w-12 text-slate-600" />
            <h3 className="mt-3 text-lg font-semibold text-white">No workflows configured</h3>
            <p className="mt-1 max-w-sm text-xs text-slate-400">
              Create the first workflow version for this department to enable automatic staff assignment.
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Version Selector Tabs */}
            <div className="flex flex-wrap items-center gap-3">
              {workflows.map((wf) => {
                const isSelected = selectedWorkflow?.workflowId === wf.workflowId;
                return (
                  <button
                    key={wf.workflowId}
                    onClick={() => setSelectedWorkflow(wf)}
                    className={`flex items-center gap-2.5 rounded-2xl border px-4 py-3 text-left transition ${
                      isSelected
                        ? "border-cyan-400/50 bg-cyan-500/10 shadow-lg shadow-cyan-500/10"
                        : "border-white/10 bg-white/5 hover:bg-white/10"
                    }`}
                  >
                    <div className="flex flex-col">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-white">
                          Version {wf.version}
                        </span>
                        {wf.active ? (
                          <span className="inline-flex items-center gap-1 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2 py-0.5 text-[10px] font-semibold text-emerald-300">
                            <CheckCircle2 className="h-3 w-3" /> Active
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 rounded-full border border-slate-700 bg-slate-800 px-2 py-0.5 text-[10px] font-semibold text-slate-400">
                            Draft / Inactive
                          </span>
                        )}
                      </div>
                      <div className="mt-1 flex items-center gap-3 text-xs text-slate-400">
                        <span>{wf.stepCount ?? 0} Steps</span>
                        <span>•</span>
                        <span>{wf.complaintCount ?? 0} Complaints</span>
                        <span>•</span>
                        {wf.isLocked ? (
                          <span className="flex items-center gap-1 text-blue-300">
                            <Lock className="h-3 w-3" /> Locked
                          </span>
                        ) : (
                          <span className="flex items-center gap-1 text-amber-300">
                            <Unlock className="h-3 w-3" /> Editable
                          </span>
                        )}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Selected Workflow Inspection & Step Management */}
            {selectedWorkflow && (
              <div className="rounded-2xl border border-white/10 bg-white/5 p-6 backdrop-blur-xl shadow-2xl">
                {/* Workflow Meta Header */}
                <div className="flex flex-col gap-4 border-b border-white/10 pb-6 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <div className="flex items-center gap-3">
                      <h2 className="text-xl font-bold text-white">
                        {selectedWorkflow.name || `${selectedWorkflow.departmentName} Workflow v${selectedWorkflow.version}`}
                      </h2>
                      {selectedWorkflow.active ? (
                        <span className="inline-flex items-center gap-1 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-3 py-1 text-xs font-semibold text-emerald-300">
                          <CheckCircle2 className="h-3.5 w-3.5" /> Active for New Complaints
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full border border-amber-500/30 bg-amber-500/10 px-3 py-1 text-xs font-semibold text-amber-300">
                          <AlertTriangle className="h-3.5 w-3.5" /> Inactive Version
                        </span>
                      )}
                      {selectedWorkflow.isLocked ? (
                        <span className="inline-flex items-center gap-1 rounded-full border border-blue-500/30 bg-blue-500/10 px-3 py-1 text-xs font-semibold text-blue-300">
                          <Lock className="h-3.5 w-3.5" /> Locked ({selectedWorkflow.complaintCount} Complaints)
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full border border-purple-500/30 bg-purple-500/10 px-3 py-1 text-xs font-semibold text-purple-300">
                          <Unlock className="h-3.5 w-3.5" /> Draft (Editable)
                        </span>
                      )}
                    </div>
                    <p className="mt-1.5 text-xs text-slate-400">
                      Workflow ID: <code className="text-cyan-300">{selectedWorkflow.workflowId}</code> • Department:{" "}
                      <span className="text-white">{selectedWorkflow.departmentName} ({selectedWorkflow.departmentCode})</span>
                    </p>
                  </div>

                  <div className="flex items-center gap-3">
                    {!selectedWorkflow.isLocked && (
                      <Button
                        onClick={handleOpenAddStep}
                        className="border border-white/10 bg-white/5 font-semibold text-slate-200 hover:bg-white/10 hover:text-white"
                      >
                        <Plus className="mr-1.5 h-4 w-4 text-cyan-400" /> Add Step
                      </Button>
                    )}

                    {!selectedWorkflow.active && (
                      <Button
                        onClick={() => {
                          setActivateError("");
                          setShowActivateModal(true);
                        }}
                        className="bg-gradient-to-r from-emerald-600 to-teal-600 font-semibold text-white shadow-lg shadow-emerald-600/30 hover:from-emerald-500 hover:to-teal-500"
                      >
                        <CheckCircle2 className="mr-1.5 h-4 w-4" /> Activate This Version
                      </Button>
                    )}
                  </div>
                </div>

                {/* Steps Timeline Hierarchy */}
                <div className="mt-6">
                  <div className="mb-4 flex items-center justify-between">
                    <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-300">
                      Escalation Hierarchy & Routing Steps ({selectedWorkflow.steps?.length || 0})
                    </h3>
                    {selectedWorkflow.isLocked && (
                      <span className="text-xs text-slate-400">
                        Historical workflow is immutable. Create a new version to modify escalation rules.
                      </span>
                    )}
                  </div>

                  {(!selectedWorkflow.steps || selectedWorkflow.steps.length === 0) ? (
                    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-white/10 bg-black/20 p-8 text-center">
                      <GitBranch className="h-10 w-10 text-slate-600" />
                      <p className="mt-2 text-sm font-medium text-slate-300">No steps defined for this version</p>
                      <p className="mt-1 text-xs text-slate-500">
                        Workflows require at least 1 sequential step (Level 1) to be activated.
                      </p>
                      {!selectedWorkflow.isLocked && (
                        <Button
                          onClick={handleOpenAddStep}
                          size="sm"
                          className="mt-4 bg-cyan-600 text-white hover:bg-cyan-500"
                        >
                          <Plus className="mr-1.5 h-4 w-4" /> Add Level 1 Step
                        </Button>
                      )}
                    </div>
                  ) : (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                      {selectedWorkflow.steps.map((step, idx) => (
                        <div
                          key={step.stepId}
                          className="relative flex flex-col justify-between rounded-2xl border border-white/10 bg-slate-900/60 p-5 shadow-xl transition hover:border-cyan-500/30"
                        >
                          <div>
                            {/* Level Header */}
                            <div className="flex items-center justify-between">
                              <span className="flex h-7 w-7 items-center justify-center rounded-xl bg-cyan-500/20 font-mono text-xs font-bold text-cyan-300">
                                L{step.level}
                              </span>
                              <span className="rounded-md border border-white/10 bg-black/30 px-2 py-0.5 text-[11px] text-slate-400">
                                Step #{idx + 1}
                              </span>
                            </div>

                            {/* Role Details */}
                            <div className="mt-4">
                              <span className="text-xs font-medium text-slate-400 uppercase tracking-wider">
                                Assigned Role
                              </span>
                              <p className="mt-0.5 text-base font-bold text-white">
                                {step.roleName}
                              </p>
                            </div>

                            {/* Assignment Scope & SLA */}
                            <div className="mt-4 space-y-2">
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-slate-400">Assignment Scope:</span>
                                <span className="rounded-md border border-indigo-500/30 bg-indigo-500/10 px-2 py-0.5 font-mono font-semibold text-indigo-300">
                                  {step.assignmentScope}
                                </span>
                              </div>
                              <div className="flex items-center justify-between text-xs">
                                <span className="text-slate-400">Resolution SLA:</span>
                                <span className="flex items-center gap-1 font-mono font-semibold text-emerald-300">
                                  <Clock className="h-3 w-3 text-emerald-400" /> {step.resolutionTimeHours}h
                                </span>
                              </div>
                            </div>
                          </div>

                          {/* Step Actions (Only if unlocked) */}
                          {!selectedWorkflow.isLocked && (
                            <div className="mt-5 flex items-center justify-end gap-2 border-t border-white/5 pt-3">
                              <button
                                onClick={() => handleOpenEditStep(step)}
                                className="rounded-lg p-1.5 text-slate-400 hover:bg-white/10 hover:text-white"
                                title="Edit Step"
                              >
                                <Edit2 className="h-4 w-4" />
                              </button>
                              <button
                                onClick={() => handleDeleteStep(step.stepId)}
                                className="rounded-lg p-1.5 text-rose-400 hover:bg-rose-500/10 hover:text-rose-300"
                                title="Delete Step"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        {/* CREATE VERSION MODAL */}
        <AnimatePresence>
          {showCreateVersionModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowCreateVersionModal(false)}
                className="absolute inset-0 bg-black/70 backdrop-blur-sm"
              />

              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-md rounded-2xl border border-white/10 bg-slate-900 p-6 shadow-2xl"
              >
                <div className="flex items-center justify-between border-b border-white/10 pb-4">
                  <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-500/20 text-cyan-400">
                      <Plus className="h-4 w-4" />
                    </div>
                    <h3 className="text-lg font-bold text-white">Create Workflow Version</h3>
                  </div>
                  <button
                    onClick={() => setShowCreateVersionModal(false)}
                    className="rounded-lg p-1 text-slate-400 hover:bg-white/10 hover:text-white"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                <form onSubmit={handleCreateVersion} className="mt-4 space-y-4">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Target Department
                    </label>
                    <div className="mt-1.5 flex h-10 items-center rounded-xl border border-white/10 bg-slate-800/60 px-3 text-sm text-slate-200">
                      {currentDept?.name} ({currentDept?.code})
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Workflow Name (Optional)
                    </label>
                    <Input
                      type="text"
                      placeholder={`e.g. ${currentDept?.name} Workflow v${(workflows.length || 0) + 1}`}
                      value={versionName}
                      onChange={(e) => setVersionName(e.target.value)}
                      className="mt-1.5 border-white/10 bg-slate-800 text-white placeholder-slate-500"
                    />
                    <p className="mt-1 text-[11px] text-slate-400">
                      New workflow will be created in DRAFT state (inactive) with zero initial steps.
                    </p>
                  </div>

                  <div className="mt-6 flex items-center justify-end gap-3 border-t border-white/10 pt-4">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setShowCreateVersionModal(false)}
                      className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      disabled={creatingVersion}
                      className="bg-gradient-to-r from-cyan-500 to-blue-600 font-semibold text-white hover:from-cyan-400 hover:to-blue-500"
                    >
                      {creatingVersion ? "Creating..." : "Create Draft"}
                    </Button>
                  </div>
                </form>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

        {/* ADD / EDIT STEP MODAL */}
        <AnimatePresence>
          {showStepModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowStepModal(false)}
                className="absolute inset-0 bg-black/70 backdrop-blur-sm"
              />

              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-md rounded-2xl border border-white/10 bg-slate-900 p-6 shadow-2xl"
              >
                <div className="flex items-center justify-between border-b border-white/10 pb-4">
                  <div className="flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-500/20 text-indigo-400">
                      <GitBranch className="h-4 w-4" />
                    </div>
                    <h3 className="text-lg font-bold text-white">
                      {editingStep ? "Edit Workflow Step" : "Add Workflow Step"}
                    </h3>
                  </div>
                  <button
                    onClick={() => setShowStepModal(false)}
                    className="rounded-lg p-1 text-slate-400 hover:bg-white/10 hover:text-white"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                {stepError && (
                  <div className="mt-4 flex items-center gap-2 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-xs text-rose-300">
                    <AlertTriangle className="h-4 w-4 shrink-0" />
                    <span>{stepError}</span>
                  </div>
                )}

                <form onSubmit={handleSaveStep} className="mt-4 space-y-4">
                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Step Level (Contiguous 1, 2, 3...) *
                    </label>
                    <Input
                      type="number"
                      min="1"
                      value={stepLevel}
                      onChange={(e) => setStepLevel(e.target.value)}
                      required
                      className="mt-1.5 border-white/10 bg-slate-800 text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Assigned Role *
                    </label>
                    <select
                      value={stepRoleId}
                      onChange={(e) => setStepRoleId(e.target.value)}
                      required
                      className="mt-1.5 w-full rounded-xl border border-white/10 bg-slate-800 px-3.5 py-2.5 text-sm text-white focus:border-cyan-500 focus:outline-none"
                    >
                      <option value="">Select a role...</option>
                      {roles.map((r) => (
                        <option key={r.roleId} value={r.roleId}>
                          {r.roleName} ({r.assignmentScope})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
                      Resolution SLA (Hours) *
                    </label>
                    <Input
                      type="number"
                      min="1"
                      value={stepSla}
                      onChange={(e) => setStepSla(e.target.value)}
                      required
                      className="mt-1.5 border-white/10 bg-slate-800 text-white"
                    />
                  </div>

                  <div className="mt-6 flex items-center justify-end gap-3 border-t border-white/10 pt-4">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setShowStepModal(false)}
                      className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      disabled={submittingStep}
                      className="bg-gradient-to-r from-cyan-500 to-indigo-600 font-semibold text-white hover:from-cyan-400 hover:to-indigo-500"
                    >
                      {submittingStep ? "Saving..." : editingStep ? "Save Changes" : "Add Step"}
                    </Button>
                  </div>
                </form>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

        {/* ACTIVATE WORKFLOW CONFIRMATION MODAL */}
        <AnimatePresence>
          {showActivateModal && selectedWorkflow && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowActivateModal(false)}
                className="absolute inset-0 bg-black/70 backdrop-blur-sm"
              />

              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-md rounded-2xl border border-white/10 bg-slate-900 p-6 shadow-2xl"
              >
                <div className="flex items-center gap-3 border-b border-white/10 pb-4">
                  <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-500/20 text-emerald-400">
                    <CheckCircle2 className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white">Activate Workflow Version</h3>
                    <p className="text-xs text-slate-400">Version {selectedWorkflow.version}</p>
                  </div>
                </div>

                {activateError && (
                  <div className="mt-4 flex items-center gap-2 rounded-xl border border-rose-500/30 bg-rose-500/10 p-3 text-xs text-rose-300">
                    <AlertTriangle className="h-4 w-4 shrink-0" />
                    <span>{activateError}</span>
                  </div>
                )}

                <div className="mt-4 space-y-3 text-xs text-slate-300">
                  <div className="rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-amber-200">
                    <p className="font-semibold">⚠️ Important Architectural Rule:</p>
                    <p className="mt-1">
                      Activating <strong>Version {selectedWorkflow.version}</strong> will make it the active workflow for all <strong>NEW</strong> complaints filed in <strong>{selectedWorkflow.departmentName}</strong>.
                    </p>
                    <p className="mt-1">
                      Existing complaints will continue using their historical workflow version and remain 100% unaffected.
                    </p>
                  </div>
                  <p>
                    All other workflow versions for this department will be set to <strong>Inactive</strong>.
                  </p>
                </div>

                <div className="mt-6 flex items-center justify-end gap-3 border-t border-white/10 pt-4">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setShowActivateModal(false)}
                    className="border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleActivate}
                    disabled={activating}
                    className="bg-gradient-to-r from-emerald-600 to-teal-600 font-semibold text-white shadow-lg shadow-emerald-600/30 hover:from-emerald-500 hover:to-teal-500"
                  >
                    {activating ? "Activating..." : "Confirm & Activate"}
                  </Button>
                </div>
              </motion.div>
            </div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
