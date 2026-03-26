import React, { useState, useEffect, useMemo } from "react";
import { api } from "@/services/api";
import { useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import ComplaintCard from "../components/complaints/ComplaintCard";

import {
  Search,
  FileText,
  Clock,
  AlertCircle,
  CheckCircle,
  ArrowRight,
  Activity,
  ShieldCheck,
  Users,
  Sparkles,
} from "lucide-react";

import { motion } from "framer-motion";

export default function StaffDashboard() {
  const [user, setUser] = useState(null);
  const [complaints, setComplaints] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeTab, setActiveTab] = useState("all");
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [action, setAction] = useState("");
  const [note, setNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [mouse, setMouse] = useState({ x: 0, y: 0 });

  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const me = await api.get("/auth/me");
      setUser(me.data);

      const complaintsRes = await api.get("/complaints/assigned");
      const assignedComplaints = await complaintsRes.json();

      setComplaints(assignedComplaints);
    } catch (error) {
      console.error("Error loading data:", error);
    } finally {
      setLoading(false);
    }
  };

  const filteredComplaints = useMemo(() => {
    let filtered = complaints;

    if (activeTab === "pending") {
      filtered = filtered.filter((c) =>
        ["assigned", "submitted"].includes(c.status),
      );
    } else if (activeTab === "in_progress") {
      filtered = filtered.filter((c) => c.status === "in_progress");
    } else if (activeTab === "resolved") {
      filtered = filtered.filter((c) => c.status === "resolved");
    }

    if (searchTerm) {
      filtered = filtered.filter(
        (c) =>
          c.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          c.student_name?.toLowerCase().includes(searchTerm.toLowerCase()),
      );
    }

    return filtered;
  }, [complaints, searchTerm, activeTab]);

  const handleAction = async () => {
    if (!action || !selectedComplaint) return;

    const statusMap = {
      in_progress: "in_progress",
      resolve: "resolved",
      request_info: "info_requested",
    };

    const newStatus = statusMap[action];

    try {
      await fetch(`/api/complaints/${selectedComplaint.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          status: newStatus,
          resolution_note: action === "resolve" ? note : undefined,
        }),
      });

      await fetch("/api/complaint-updates", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          complaint_id: selectedComplaint.id,
          action: newStatus,
          performed_by_email: user.email,
          performed_by_name: user.full_name,
          note,
          from_status: selectedComplaint.status,
          to_status: newStatus,
        }),
      });

      await fetch("/api/notifications", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          user_email: selectedComplaint.student_email,
          title: "Complaint Update",
          message: `Your complaint "${selectedComplaint.title}" has been updated`,
          type: "status_update",
          complaint_id: selectedComplaint.id,
        }),
      });

      setSelectedComplaint(null);
      setAction("");
      setNote("");
      loadData();
    } catch (error) {
      console.error("Error updating complaint:", error);
    }
  };

  const stats = {
    total: complaints.length,
    pending: complaints.filter((c) =>
      ["assigned", "submitted"].includes(c.status),
    ).length,
    inProgress: complaints.filter((c) => c.status === "in_progress").length,
    resolved: complaints.filter((c) => c.status === "resolved").length,
  };

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
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-blue-500 shadow-lg shadow-indigo-500/30">
              <ShieldCheck className="h-5 w-5 text-white" />
            </div>

            <div>
              <h2 className="text-lg font-semibold tracking-wide text-white">
                Staff Workspace
              </h2>
              <p className="text-xs text-slate-300">
                Complaint management dashboard
              </p>
            </div>
          </div>

          <div className="hidden items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300 md:flex">
            <Activity className="h-4 w-4 text-emerald-400" />
            Live staff panel
          </div>
        </motion.div>

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
          className="relative mb-8 overflow-hidden rounded-[2rem] border border-white/10 bg-white/8 shadow-[0_20px_80px_rgba(0,0,0,0.35)] backdrop-blur-2xl"
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
                Welcome,{" "}
                <span className="bg-gradient-to-r from-white via-indigo-200 to-blue-300 bg-clip-text text-transparent">
                  {user?.profile?.name || "Staff"}
                </span>
              </h1>

              <p className="mt-5 max-w-2xl text-base leading-8 text-slate-300 sm:text-lg">
                Manage assigned complaints, update statuses, communicate
                progress, and resolve student issues through a modern staff
                operations workspace.
              </p>

              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <Button
                  onClick={() => navigate("/complaint-details")}
                  className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
                >
                  <FileText className="mr-2 h-4 w-4" />
                  Open Complaints  
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

              <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/15 to-cyan-500/10 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-emerald-300">Resolved Rate</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.total > 0
                    ? `${Math.round((stats.resolved / stats.total) * 100)}%`
                    : "0%"}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Based on completed complaints
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-slate-400">Currently Active</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.inProgress}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Complaints in progress
                </p>
              </div>
            </div>
          </div>
        </motion.div>

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
                  Total Assigned
                </p>
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.total}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  All assigned complaints
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-blue-500 shadow-lg shadow-indigo-500/30">
                <FileText className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-amber-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-amber-500/10 via-transparent to-orange-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">Pending</p>
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.pending}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  Waiting for action
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-amber-400 via-orange-500 to-yellow-500 shadow-lg shadow-amber-500/30">
                <Clock className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>

          <div className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-cyan-500/20">
            <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/10 via-transparent to-blue-500/10 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
            <div className="relative flex items-start justify-between">
              <div>
                <p className="text-sm font-medium text-slate-300">
                  In Progress
                </p>
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.inProgress}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  Under staff handling
                </p>
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
                <h3 className="mt-3 text-3xl font-bold text-white">
                  {stats.resolved}
                </h3>
                <p className="mt-2 text-xs text-slate-400">
                  Successfully completed
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-400 via-green-500 to-teal-500 shadow-lg shadow-emerald-500/30">
                <CheckCircle className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        </motion.div>

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
                placeholder="Search by complaint title or student name..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="h-12 rounded-2xl border-white/10 bg-slate-900/40 pl-11 text-white placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-indigo-500"
              />
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
              <span className="font-semibold text-white">
                {filteredComplaints.length}
              </span>{" "}
              complaints visible
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.85 }}
          className="mb-8 flex flex-wrap gap-3"
        >
          <button
            onClick={() => setActiveTab("all")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              activeTab === "all"
                ? "bg-indigo-500 text-white shadow-lg shadow-indigo-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            All
          </button>

          <button
            onClick={() => setActiveTab("pending")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              activeTab === "pending"
                ? "bg-amber-500 text-white shadow-lg shadow-amber-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            Pending
          </button>

          <button
            onClick={() => setActiveTab("in_progress")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              activeTab === "in_progress"
                ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            In Progress
          </button>

          <button
            onClick={() => setActiveTab("resolved")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              activeTab === "resolved"
                ? "bg-emerald-500 text-white shadow-lg shadow-emerald-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            Resolved
          </button>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9 }}
        >
          <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <h2 className="text-2xl font-bold text-white sm:text-3xl">
                Assigned Complaints
              </h2>
              <p className="mt-2 text-slate-400">
                Review assigned issues, update statuses, and take action.
              </p>
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
              Staff panel for{" "}
              <span className="font-semibold text-white">
                {user?.full_name || "Team Member"}
              </span>
            </div>
          </div>

          {loading ? (
            <div className="rounded-[2rem] border border-white/10 bg-white/6 p-12 text-center backdrop-blur-xl">
              <div className="mx-auto mb-5 h-10 w-10 animate-spin rounded-full border-4 border-indigo-500 border-r-transparent" />
              <p className="text-slate-300">Loading assigned complaints...</p>
            </div>
          ) : filteredComplaints.length > 0 ? (
            <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
              {filteredComplaints.map((complaint, index) => (
                <motion.div
                  key={complaint.id}
                  initial={{ opacity: 0, y: 22 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.35, delay: index * 0.05 }}
                  className="rounded-[1.75rem] border border-white/10 bg-white/8 p-4 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl"
                >
                  <ComplaintCard
                    complaint={complaint}
                    onClick={() => navigate(`/complaint/${complaint.id}`)}
                  />

                  <Button
                    onClick={() => setSelectedComplaint(complaint)}
                    className="mt-4 h-11 w-full rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 font-semibold text-white shadow-lg shadow-indigo-500/20 transition hover:scale-[1.01]"
                  >
                    <Sparkles className="mr-2 h-4 w-4" />
                    Take Action
                  </Button>
                </motion.div>
              ))}
            </div>
          ) : (
            <div className="rounded-[2rem] border border-dashed border-white/15 bg-white/6 p-12 text-center backdrop-blur-xl">
              <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-3xl bg-white/10">
                <Users className="h-8 w-8 text-slate-300" />
              </div>

              <h3 className="mb-3 text-2xl font-bold text-white">
                No complaints found
              </h3>

              <p className="mx-auto mb-8 max-w-xl leading-7 text-slate-400">
                There are no assigned complaints matching your current search or
                selected filter.
              </p>

              <Button
                onClick={loadData}
                className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 px-6 font-semibold transition-all hover:scale-[1.02]"
              >
                Refresh Dashboard
              </Button>
            </div>
          )}
        </motion.div>
      </div>

      <Dialog
        open={!!selectedComplaint}
        onOpenChange={() => {
          setSelectedComplaint(null);
          setAction("");
          setNote("");
        }}
      >
        <DialogContent className="border border-white/10 bg-slate-950 text-white shadow-2xl">
          <DialogHeader>
            <DialogTitle className="text-xl font-bold text-white">
              Take Action
            </DialogTitle>
            <DialogDescription className="text-slate-400">
              {selectedComplaint?.title}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <Select value={action} onValueChange={setAction}>
              <SelectTrigger className="border-white/10 bg-white/5 text-white">
                <SelectValue placeholder="Select action" />
              </SelectTrigger>
              <SelectContent className="border-white/10 bg-slate-900 text-white">
                <SelectItem value="in_progress">Mark In Progress</SelectItem>
                <SelectItem value="request_info">Request More Info</SelectItem>
                <SelectItem value="resolve">Resolve</SelectItem>
              </SelectContent>
            </Select>

            <Textarea
              placeholder="Add a note..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={4}
              className="border-white/10 bg-white/5 text-white placeholder:text-slate-400"
            />

            <Button
              onClick={handleAction}
              className="h-11 w-full rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 font-semibold text-white shadow-lg shadow-indigo-500/20 transition hover:scale-[1.01]"
            >
              Submit Action
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
