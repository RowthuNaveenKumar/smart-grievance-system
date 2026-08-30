import React, { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import moment from "moment";
import { motion } from "framer-motion";

import { api } from "@/services/api";

import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

import {
  ArrowLeft,
  User,
  FileText,
  AlertTriangle,
  Loader2,
  ChevronRight,
  FolderOpen,
  ShieldCheck,
  CalendarDays,
  Sparkles,
  Clock3,
  CheckCircle2,
  Lock,
  Building2,
  Edit3,
  UserPlus,
  Info,
  X,
} from "lucide-react";
import { useUser } from "@/context/UserContext";
import {
  overrideDepartment,
  reassignStaff,
  getDepartments,
  getStaffByDepartment,
} from "@/services/adminComplaintApi";
import { toast } from "sonner";

export default function ComplaintDetails() {
  const { id } = useParams();
  const { user } = useUser();
  const isAdmin = user?.role === "ADMIN";
  const isStaff = user?.accountType === "STAFF" && !isAdmin;
  const isStudent = user?.accountType === "STUDENT";
  const navigate = useNavigate();
  const location = useLocation();

  /**
   * Determine the back destination using explicit navigation state first,
   * then fall back to the authenticated user's role-based dashboard.
   * Never navigates to public pages (/, /home, /login, /submit).
   */
  const getBackDestination = () => {
    const from = location.state?.from;

    if (from === "submission") return "/student-dashboard";
    if (from === "student-dashboard") return "/student-dashboard";
    if (from === "staff-dashboard") return "/staff-dashboard";
    if (from === "admin-dashboard") return "/admin-dashboard";
    if (from === "admin-complaints") return "/admin/complaints";

    // Safe role-based fallback when state is absent (direct URL / refresh)
    if (user?.role === "ADMIN") return "/admin/complaints";
    if (user?.accountType === "STUDENT") return "/student-dashboard";
    if (user?.accountType === "STAFF") return "/staff-dashboard";

    // Last resort: login (should not be reached for authenticated users)
    return "/login";
  };

  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);

  // Admin Override Modal
  const [departments, setDepartments] = useState([]);
  const [overrideModalOpen, setOverrideModalOpen] = useState(false);
  const [targetDeptId, setTargetDeptId] = useState("");
  const [overrideReason, setOverrideReason] = useState("");
  const [overrideSubmitting, setOverrideSubmitting] = useState(false);

  // Admin Reassign Modal
  const [reassignModalOpen, setReassignModalOpen] = useState(false);
  const [deptStaffList, setDeptStaffList] = useState([]);
  const [targetStaffId, setTargetStaffId] = useState("");
  const [reassignReason, setReassignReason] = useState("");
  const [reassignLoading, setReassignLoading] = useState(false);
  const [reassignSubmitting, setReassignSubmitting] = useState(false);

  const [note, setNote] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    loadComplaint();
  }, [id]);

  const loadComplaint = async () => {
    try {
      const res = await api.get(`/complaints/${id}`);
      setComplaint(res.data);
    } catch (err) {
      console.error("Error loading complaint:", err);
    }
    setLoading(false);
  };

  const getPriorityStyles = (priority) => {
    switch ((priority || "").toUpperCase()) {
      case "HIGH":
        return "bg-red-500/15 text-red-300 border border-red-400/20";
      case "MEDIUM":
        return "bg-yellow-500/15 text-yellow-300 border border-yellow-400/20";
      case "LOW":
        return "bg-cyan-500/15 text-cyan-300 border border-cyan-400/20";
      default:
        return "bg-indigo-500/15 text-indigo-300 border border-indigo-400/20";
    }
  };

  const getStatusStyles = (status) => {
    switch ((status || "").toUpperCase()) {
      case "OPEN":
      case "IN_PROGRESS":
        return {
          className: "bg-cyan-500/15 text-cyan-300 border border-cyan-400/20",
          icon: <Clock3 className="h-3.5 w-3.5" />,
        };
      case "RESOLVED":
        return {
          className:
            "bg-emerald-500/15 text-emerald-300 border border-emerald-400/20",
          icon: <CheckCircle2 className="h-3.5 w-3.5" />,
        };
      case "CLOSED":
        return {
          className: "bg-rose-500/15 text-rose-300 border border-rose-400/20",
          icon: <Lock className="h-3.5 w-3.5" />,
        };
      default:
        return {
          className:
            "bg-slate-500/15 text-slate-300 border border-slate-400/20",
          icon: <ShieldCheck className="h-3.5 w-3.5" />,
        };
    }
  };

  /* =========================================
      Mark In Progress
    ========================================= */

  const markInProgress = async () => {
    try {
      setActionLoading(true);

      await api.patch(
        `/complaints/${id}/status`,
        {
          note,
        },
        {
          params: { action: "MARK_IN_PROGRESS" },
        },
      );
      setNote("");
      loadComplaint();
    } catch (err) {
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  };

  const resolveComplaint = async () => {
    if (!note.trim()) {
      alert("Please add resolution note");
      return;
    }
    try {
      setActionLoading(true);

      await api.patch(
        `/complaints/${id}/status`,
        {
          note,
        },
        {
          params: { action: "RESOLVE" },
        },
      );

      setNote("");
      loadComplaint();
    } catch (err) {
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  };

  const escalateComplaint = async () => {
    if (!note.trim()) {
      alert("Please provide reason for escalation");
      return;
    }
    try {
      setActionLoading(true);

      await api.patch(`/complaints/${id}/escalate`, {
        note,
      });
      setNote("");
      loadComplaint();
    } catch (err) {
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  };

  const submitFeedback = async (accepted) => {
    try {
      setActionLoading(true);

      await api.post(`/complaints/${id}/feedback`, null, {
        params: { accepted },
      });

      loadComplaint();
    } catch (err) {
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  };

  const openOverrideModal = async () => {
    setTargetDeptId(complaint.departmentId ? String(complaint.departmentId) : "");
    setOverrideReason("");
    setOverrideModalOpen(true);
    try {
      const depts = await getDepartments();
      setDepartments(depts || []);
    } catch (err) {
      console.error(err);
    }
  };

  const handleOverrideSubmit = async (e) => {
    e.preventDefault();
    if (!targetDeptId) {
      toast.error("Please select a target department");
      return;
    }
    if (!overrideReason.trim()) {
      toast.error("Please provide an override justification note");
      return;
    }
    try {
      setOverrideSubmitting(true);
      await overrideDepartment(complaint.complaintId, {
        departmentId: Number(targetDeptId),
        note: overrideReason.trim(),
      });
      toast.success("Department overridden successfully");
      setOverrideModalOpen(false);
      loadComplaint();
    } catch (err) {
      console.error(err);
      toast.error(err.response?.data?.message || "Failed to override department");
    } finally {
      setOverrideSubmitting(false);
    }
  };

  const openReassignModal = async () => {
    setTargetStaffId(complaint.assignedStaffId ? String(complaint.assignedStaffId) : "");
    setReassignReason("");
    setReassignModalOpen(true);
    if (complaint.departmentId) {
      try {
        setReassignLoading(true);
        const staff = await getStaffByDepartment(complaint.departmentId);
        setDeptStaffList(staff || []);
      } catch (err) {
        console.error(err);
      } finally {
        setReassignLoading(false);
      }
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
      await reassignStaff(complaint.complaintId, {
        staffId: Number(targetStaffId),
        note: reassignReason.trim() || undefined,
      });
      toast.success("Staff reassigned successfully");
      setReassignModalOpen(false);
      loadComplaint();
    } catch (err) {
      console.error(err);
      toast.error(err.response?.data?.message || "Failed to reassign staff");
    } finally {
      setReassignSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 text-white">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
        <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
        <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
        <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

        <div className="relative z-10 flex flex-col items-center">
          <Loader2 className="h-10 w-10 animate-spin text-indigo-400" />
          <p className="mt-4 text-slate-300">Loading complaint details...</p>
        </div>
      </div>
    );
  }

  if (!complaint) {
    return (
      <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 text-white">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
        <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
        <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
        <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

        <div className="relative z-10 text-center">
          <AlertTriangle className="mx-auto mb-4 h-12 w-12 text-slate-400" />
          <p className="text-slate-300">Complaint not found</p>
        </div>
      </div>
    );
  }

  const statusStyle = getStatusStyles(complaint.status);

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

      <div className="relative z-10 mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: -18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="mb-8 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-5 py-4 shadow-2xl backdrop-blur-xl"
        >
          <div>
            <h2 className="text-lg font-semibold tracking-wide text-white">
              Complaint Details
            </h2>
            <p className="text-xs text-slate-300">
              Full grievance information and activity timeline
            </p>
          </div>

          <Button
            variant="ghost"
            onClick={() => navigate(getBackDestination())}
            className="flex items-center rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-slate-300 hover:bg-white/10 hover:text-white"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back
          </Button>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 shadow-[0_20px_80px_rgba(0,0,0,0.35)] backdrop-blur-2xl"
        >
          {/* Top section */}
          <div className="relative overflow-hidden border-b border-white/10">
            <div className="absolute inset-0 bg-gradient-to-r from-indigo-500/20 via-blue-500/15 to-cyan-500/15" />
            <div className="absolute -right-10 -top-10 h-36 w-36 rounded-full bg-indigo-500/20 blur-3xl" />
            <div className="absolute bottom-0 left-0 h-28 w-28 rounded-full bg-cyan-500/10 blur-3xl" />

            <div className="relative p-8 sm:p-10">
              <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-indigo-400/20 bg-indigo-500/10 px-3 py-1 text-xs text-indigo-200">
                <Sparkles className="h-3.5 w-3.5" />
                Complaint Overview #{complaint.complaintId}
              </div>

              <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <h1 className="text-3xl font-bold text-white sm:text-4xl">
                    {complaint.title}
                  </h1>
                  <p className="mt-3 text-sm text-slate-300">
                    Submitted {moment(complaint.createdAt).fromNow()}
                    {complaint.studentName && <span> by <strong className="text-white">{complaint.studentName}</strong></span>}
                  </p>
                </div>

                <div className="flex flex-wrap gap-3">
                  <Badge className="border border-indigo-400/20 bg-indigo-500/15 px-3 py-1 text-indigo-200">
                    {complaint.category || "Uncategorized"}
                  </Badge>

                  <Badge
                    className={`px-3 py-1 ${getPriorityStyles(
                      complaint.priority,
                    )}`}
                  >
                    Priority: {complaint.priority}
                  </Badge>

                  <Badge
                    className={`inline-flex items-center gap-1 px-3 py-1 ${statusStyle.className}`}
                  >
                    {statusStyle.icon}
                    {complaint.status}
                  </Badge>
                </div>
              </div>
            </div>
          </div>

          <Card className="border-0 bg-transparent text-white shadow-none">
            <CardContent className="space-y-8 p-6 sm:p-8">
              {/* Description */}
              <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
                <h3 className="mb-3 text-lg font-semibold text-white">
                  Description
                </h3>
                <p className="text-base leading-8 text-slate-200">
                  {complaint.description}
                </p>
              </div>

              {/* AI Prediction & Routing Separation Section */}
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                {/* AI Prediction (Immutable Audit) */}
                <div className="rounded-2xl border border-purple-500/20 bg-purple-500/5 p-5 backdrop-blur-xl">
                  <div className="flex items-center gap-2 mb-2">
                    <Sparkles className="h-4 w-4 text-purple-400" />
                    <h4 className="text-xs font-bold uppercase tracking-wider text-purple-300">
                      AI Prediction (Immutable Audit)
                    </h4>
                  </div>
                  <div className="flex items-center justify-between mt-3">
                    <div>
                      <p className="text-xs text-slate-400">Predicted Class</p>
                      <p className="text-base font-semibold text-white mt-0.5">
                        {complaint.mlPredictedClass || "Not Classified"}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-slate-400">Confidence Score</p>
                      <p className="text-base font-semibold text-purple-300 mt-0.5">
                        {complaint.mlConfidence ? `${Math.round(complaint.mlConfidence * 100)}%` : "N/A"}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Current Operational Routing */}
                <div className="rounded-2xl border border-blue-500/20 bg-blue-500/5 p-5 backdrop-blur-xl">
                  <div className="flex items-center gap-2 mb-2">
                    <Building2 className="h-4 w-4 text-blue-400" />
                    <h4 className="text-xs font-bold uppercase tracking-wider text-blue-300">
                      Current Operational Routing
                    </h4>
                  </div>
                  <div className="flex items-center justify-between mt-3">
                    <div>
                      <p className="text-xs text-slate-400">Current Department</p>
                      <p className="text-base font-semibold text-white mt-0.5">
                        {complaint.department || "Unassigned"}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-slate-400">Category</p>
                      <p className="text-base font-semibold text-blue-200 mt-0.5">
                        {complaint.category || "None"}
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Admin Override Alert Note if Overridden */}
              {complaint.adminOverrideNote && (
                <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4 backdrop-blur-xl flex items-start gap-3">
                  <Info className="h-5 w-5 text-amber-400 shrink-0 mt-0.5" />
                  <div>
                    <h5 className="text-xs font-bold uppercase tracking-wide text-amber-300">
                      Administrative Override Justification
                    </h5>
                    <p className="text-sm text-slate-200 mt-1 italic">
                      "{complaint.adminOverrideNote}"
                    </p>
                  </div>
                </div>
              )}

              {/* Info boxes */}
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                <InfoBox
                  title="Department"
                  value={complaint.department || "Unassigned"}
                  icon={<Building2 className="h-4 w-4 text-indigo-300" />}
                />
                <InfoBox
                  title="Category"
                  value={complaint.category || "Unassigned"}
                  icon={<FolderOpen className="h-4 w-4 text-indigo-300" />}
                />
                <InfoBox
                  title="Status"
                  value={complaint.status}
                  icon={<ShieldCheck className="h-4 w-4 text-emerald-300" />}
                />
                <InfoBox
                  title="Assigned Staff"
                  value={complaint.assignedTo || "Unassigned"}
                  icon={<User className="h-4 w-4 text-cyan-300" />}
                />
              </div>

              {/* Admin Controls Section */}
              {isAdmin && (
                <div className="rounded-[1.5rem] border border-indigo-500/25 bg-indigo-500/10 p-6 backdrop-blur-xl">
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                    <div>
                      <h3 className="text-lg font-semibold text-white flex items-center gap-2">
                        <ShieldCheck className="h-5 w-5 text-indigo-400" />
                        Admin Routing Controls
                      </h3>
                      <p className="text-xs text-slate-300 mt-1">
                        Override department classification or reassign staff while maintaining a permanent audit trail.
                      </p>
                    </div>

                    <div className="flex flex-wrap gap-3">
                      <Button
                        onClick={openOverrideModal}
                        className="rounded-xl bg-indigo-600 hover:bg-indigo-700 font-semibold text-xs text-white"
                      >
                        <Edit3 className="mr-1.5 h-3.5 w-3.5" />
                        Override Department
                      </Button>
                      <Button
                        onClick={openReassignModal}
                        className="rounded-xl bg-cyan-600 hover:bg-cyan-700 font-semibold text-xs text-white"
                      >
                        <UserPlus className="mr-1.5 h-3.5 w-3.5" />
                        Reassign Staff
                      </Button>
                    </div>
                  </div>
                </div>
              )}

              {isStaff && (
                <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
                  <h3 className="mb-4 text-lg font-semibold text-white">
                    Staff Actions
                  </h3>

                  <textarea
                    placeholder="Add a note..."
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    className="w-full rounded-xl border border-white/10 bg-slate-900/40 p-3 text-sm text-white"
                  />

                  <div className="mt-4 flex flex-wrap gap-3">
                    {complaint.status === "OPEN" && (
                      <Button disabled={actionLoading} onClick={markInProgress}>
                        Mark In Progress
                      </Button>
                    )}

                    {complaint.status === "IN_PROGRESS" && (
                      <Button
                        disabled={actionLoading}
                        className="bg-emerald-600 hover:bg-emerald-700"
                        onClick={resolveComplaint}
                      >
                        Resolve
                      </Button>
                    )}

                    {complaint.status != "CLOSED" && (
                      <Button
                        disabled={actionLoading}
                        className="bg-orange-600 hover:bg-orange-700"
                        onClick={escalateComplaint}
                      >
                        Escalate
                      </Button>
                    )}
                  </div>
                </div>
              )}

              {/* =========================================
                Student Feedback Section
                ========================================= */}

              {isStudent && complaint.status === "RESOLVED" && (
                <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
                  <h3 className="mb-4 text-lg font-semibold text-white">
                    Resolution Feedback
                  </h3>

                  <p className="text-sm text-slate-300 mb-4">
                    Please confirm if your issue has been resolved
                    satisfactorily.
                  </p>

                  <div className="flex gap-4">
                    <Button
                      disabled={actionLoading}
                      className="bg-emerald-600 hover:bg-emerald-700"
                      onClick={() => submitFeedback(true)}
                    >
                      Accept Resolution
                    </Button>

                    <Button
                      disabled={actionLoading}
                      className="bg-red-600 hover:bg-red-700"
                      onClick={() => submitFeedback(false)}
                    >
                      Reject & Reopen
                    </Button>
                  </div>
                </div>
              )}

              {/* Attachments */}
              {complaint.files?.length > 0 && (
                <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
                  <p className="mb-4 text-lg font-semibold text-white">
                    Attachments
                  </p>

                  <div className="flex flex-col gap-3">
                    {complaint.files.map((file, i) => (
                      <a
                        key={i}
                        href={`http://localhost:8080${file}`}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center justify-between rounded-2xl border border-white/10 bg-slate-900/30 px-4 py-4 text-indigo-200 transition hover:bg-white/10"
                      >
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-500/15">
                            <FileText className="h-5 w-5 text-indigo-300" />
                          </div>
                          <span className="font-medium">
                            Attachment {i + 1}
                          </span>
                        </div>
                        <ChevronRight className="h-4 w-4 text-slate-400" />
                      </a>
                    ))}
                  </div>
                </div>
              )}

              {/* Timeline */}
              <div className="rounded-[1.5rem] border border-white/10 bg-white/5 p-6 backdrop-blur-xl">
                <h3 className="mb-5 text-lg font-semibold text-white">
                  Activity Timeline
                </h3>

                {complaint.timeline?.length > 0 ? (
                  <div className="space-y-5">
                    {complaint.timeline.map((t, index) => (
                      <TimelineItem key={index} item={t} />
                    ))}
                  </div>
                ) : (
                  <div className="rounded-2xl border border-dashed border-white/10 bg-slate-900/20 p-6 text-center text-slate-400">
                    No timeline activity available.
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* OVERRIDE MODAL */}
      {overrideModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-lg rounded-3xl border border-white/15 bg-slate-900 p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div className="flex items-center gap-2.5">
                <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-500/20 text-indigo-400">
                  <Edit3 className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-white">Override Department</h2>
                  <p className="text-xs text-slate-400">Complaint #{complaint.complaintId}</p>
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
              <div className="rounded-xl border border-purple-500/20 bg-purple-500/10 p-3 text-xs text-purple-200">
                <div className="flex items-center gap-1.5 font-semibold text-purple-300">
                  <Sparkles className="h-3.5 w-3.5" />
                  AI Prediction: {complaint.mlPredictedClass || "None"} ({Math.round((complaint.mlConfidence || 0) * 100)}%)
                </div>
                <p className="mt-1 text-slate-300 text-[11px]">
                  The AI prediction remains permanent for auditing. Overriding will only modify operational routing.
                </p>
              </div>

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
          </div>
        </div>
      )}

      {/* REASSIGN MODAL */}
      {reassignModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4 backdrop-blur-md">
          <div className="w-full max-w-lg rounded-3xl border border-white/15 bg-slate-900 p-6 shadow-2xl">
            <div className="flex items-center justify-between pb-4 border-b border-white/10">
              <div className="flex items-center gap-2.5">
                <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-500/20 text-cyan-400">
                  <UserPlus className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-lg font-bold text-white">Reassign Staff</h2>
                  <p className="text-xs text-slate-400">Department: {complaint.department || "None"}</p>
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
          </div>
        </div>
      )}
    </div>
  );
}

function InfoBox({ title, value, icon }) {
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4 shadow-sm backdrop-blur-xl">
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
        {title}
      </p>
      <div className="flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/5">
          {icon}
        </div>
        <span className="text-sm font-medium text-slate-200">{value}</span>
      </div>
    </div>
  );
}

function TimelineItem({ item }) {
  return (
    <div className="relative pl-8">
      <div className="absolute left-0 top-1 h-full border-l-2 border-indigo-400/20" />
      <div className="absolute -left-[5px] top-1 h-3 w-3 rounded-full bg-gradient-to-br from-indigo-400 to-cyan-400 shadow-lg shadow-indigo-500/30" />

      <div className="rounded-2xl border border-white/10 bg-slate-900/25 p-4 backdrop-blur-xl">
        <p className="text-sm font-semibold text-white">{item.action}</p>
        <p className="mb-1 mt-1 text-xs text-slate-300">
          {item.fromStatus} → {item.toStatus}
        </p>
        {item.note && (
          <div className="mt-2 rounded-lg bg-indigo-500/10 border border-indigo-400/20 p-3">
            <p className="text-xs text-indigo-200 font-medium">Note</p>
            <p className="text-sm text-indigo-100 mt-1 italic">{item.note}</p>
          </div>
        )}

        <p className="text-xs text-slate-400 mt-2">
          by {item.performedBy} •{" "}
          {moment(item.createdAt).format("DD MMM, HH:mm")}
        </p>
      </div>
    </div>
  );
}
