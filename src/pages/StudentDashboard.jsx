import React, { useEffect, useMemo, useState } from "react";
import { api } from "@/services/api";
import { useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import StatsCard from "@/components/dashboard/StatsCard";
import ComplaintCard from "@/components/complaints/ComplaintCard";

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
} from "lucide-react";

import { motion } from "framer-motion";

export default function StudentDashboard() {
  const [user, setUser] = useState(null);
  const [complaints, setComplaints] = useState([]);
  const [search, setSearch] = useState("");
  const [mouse, setMouse] = useState({ x: 0, y: 0 });

  const navigate = useNavigate();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const me = await api.get("/auth/me");
      setUser(me.data);

      const res = await api.get("/complaints/my");
      setComplaints(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const filtered = useMemo(() => {
    return complaints.filter((c) =>
      c.title.toLowerCase().includes(search.toLowerCase()),
    );
  }, [complaints, search]);

  const stats = {
    total: complaints.length,
    active: complaints.filter((c) => c.status === "OPEN").length,
    resolved: complaints.filter((c) => c.status === "RESOLVED").length,
    closed: complaints.filter((c) => c.status === "CLOSED").length,
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Background layers */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />

      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Top navbar-like strip */}
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

        {/* Hero section with mouse-follow spotlight */}
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

          <div className="relative grid grid-cols-1 lg:grid-cols-3 gap-8 p-8 sm:p-10">
            <div className="lg:col-span-2">
              <h1 className="text-4xl sm:text-5xl xl:text-6xl font-black tracking-tight leading-tight">
                Welcome back,{" "}
                <span className="bg-gradient-to-r from-white via-indigo-200 to-blue-300 bg-clip-text text-transparent">
                  {user?.profile?.name || "Student"}
                </span>
              </h1>

              <p className="mt-5 max-w-2xl text-base sm:text-lg leading-8 text-slate-300">
                Monitor your grievances, check live progress, and submit new
                complaints through a modern and intelligent complaint management
                workspace.
              </p>

              <div className="mt-8 flex flex-col sm:flex-row gap-4">
                <Button
                  onClick={() => navigate("/submit")}
                  className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 text-white font-semibold shadow-lg shadow-indigo-500/30 hover:scale-[1.02] transition-all"
                >
                  <Plus className="mr-2 h-4 w-4" />
                  Submit New Complaint
                </Button>

                <Button
                  variant="outline"
                  onClick={() => navigate("/my-complaints")}
                  className="h-12 rounded-xl border-white/15 bg-white/5 text-white hover:bg-white/10"
                >
                  View All Complaints
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </div>
            </div>

            <div className="flex flex-col justify-between gap-4">
              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="text-sm text-slate-400 mb-2">Quick Summary</p>
                <h3 className="text-3xl font-bold text-white">{stats.total}</h3>
                <p className="text-sm text-slate-300 mt-1">
                  Total complaints submitted
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/15 to-cyan-500/10 p-5 backdrop-blur-xl">
                <p className="text-sm text-emerald-300 mb-2">Resolved Rate</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.total > 0
                    ? `${Math.round((stats.resolved / stats.total) * 100)}%`
                    : "0%"}
                </h3>
                <p className="text-sm text-slate-300 mt-1">
                  Based on completed grievances
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="text-sm text-slate-400 mb-2">Active Right Now</p>
                <h3 className="text-3xl font-bold text-white">{stats.active}</h3>
                <p className="text-sm text-slate-300 mt-1">
                  Complaints currently in progress
                </p>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Stats */}
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
          className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-6 mb-8"
        >
          <div className="rounded-3xl border border-white/10 bg-white/8 backdrop-blur-xl p-1 hover:-translate-y-1 transition-all duration-300">
            <StatsCard
              title="Total Complaints"
              value={stats.total}
              icon={Layers}
            />
          </div>

          <div className="rounded-3xl border border-white/10 bg-white/8 backdrop-blur-xl p-1 hover:-translate-y-1 transition-all duration-300">
            <StatsCard
              title="Active Complaints"
              value={stats.active}
              icon={Activity}
            />
          </div>

          <div className="rounded-3xl border border-white/10 bg-white/8 backdrop-blur-xl p-1 hover:-translate-y-1 transition-all duration-300">
            <StatsCard
              title="Resolved Complaints"
              value={stats.resolved}
              icon={BadgeCheck}
            />
          </div>

          <div className="rounded-3xl border border-white/10 bg-white/8 backdrop-blur-xl p-1 hover:-translate-y-1 transition-all duration-300">
            <StatsCard
              title="Closed Complaints"
              value={stats.closed}
              icon={Lock}
            />
          </div>
        </motion.div>

        {/* Search and info panel */}
        <motion.div
          initial={{ opacity: 0, y: 22 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="mb-8 rounded-[1.75rem] border border-white/10 bg-white/8 backdrop-blur-2xl shadow-xl"
        >
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-5 p-5 sm:p-6">
            <div className="flex-1 relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <Input
                placeholder="Search complaints by title..."
                className="h-12 rounded-2xl border-white/10 bg-slate-900/40 pl-11 text-white placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-indigo-500"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            <div className="flex items-center gap-3">
              <div className="rounded-2xl border border-white/10 bg-slate-900/30 px-4 py-3">
                <p className="text-xs uppercase tracking-wider text-slate-400">
                  Results
                </p>
                <p className="text-lg font-bold text-white">
                  {filtered.length}
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-slate-900/30 px-4 py-3">
                <p className="text-xs uppercase tracking-wider text-slate-400">
                  Search
                </p>
                <p className="text-sm font-medium text-slate-200">
                  {search ? "Active" : "All complaints"}
                </p>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Complaints section */}
        <motion.div
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9 }}
        >
          <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-6">
            <div>
              <h2 className="text-2xl sm:text-3xl font-bold text-white">
                Your Complaints
              </h2>
              <p className="mt-2 text-slate-400">
                Explore complaint history, status updates, and detailed records.
              </p>
            </div>
          </div>

          {filtered.length > 0 ? (
            <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
              {filtered.map((c, index) => (
                <motion.div
                  key={c.complaintId}
                  initial={{ opacity: 0, y: 22 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.35, delay: index * 0.05 }}
                  className="group rounded-[1.75rem] border border-white/10 bg-white/8 p-1 backdrop-blur-xl shadow-lg hover:-translate-y-1 hover:shadow-2xl transition-all duration-300"
                >
                  <ComplaintCard
                    complaint={c}
                    onClick={() => navigate(`/complaint/${c.complaintId}`)}
                  />
                </motion.div>
              ))}
            </div>
          ) : (
            <div className="rounded-[2rem] border border-dashed border-white/15 bg-white/6 backdrop-blur-xl p-12 text-center">
              <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-3xl bg-white/10">
                <FileText className="h-8 w-8 text-slate-300" />
              </div>

              <h3 className="text-2xl font-bold text-white mb-3">
                No complaints found
              </h3>

              <p className="max-w-xl mx-auto text-slate-400 leading-7 mb-8">
                There are no complaints matching your current search. Try a
                different title keyword or submit a new grievance to get started.
              </p>

              <Button
                onClick={() => navigate("/submit")}
                className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 px-6 font-semibold hover:scale-[1.02] transition-all"
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