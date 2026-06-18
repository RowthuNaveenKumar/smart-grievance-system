import React, { useEffect, useMemo, useRef, useState } from "react";
import { api } from "@/services/api";
import { useNavigate } from "react-router-dom";
import { useUser } from "@/context/UserContext";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import ComplaintCard from "@/components/complaints/ComplaintCard";

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
} from "lucide-react";

import { motion } from "framer-motion";

export default function StaffDashboard() {
  const { user } = useUser();
  const [complaints, setComplaints] = useState([]);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [mouse, setMouse] = useState({ x: 0, y: 0 });

  const navigate = useNavigate();
  const complaintsSectionRef = useRef(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const res = await api.get("/complaints/assigned");
      setComplaints(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const filtered = useMemo(() => {
    let list = complaints;

    if (filter === "PENDING") {
      list = list.filter((c) => c.status === "OPEN");
    }

    if (filter === "IN_PROGRESS") {
      list = list.filter((c) => c.status === "IN_PROGRESS");
    }

    if (filter === "RESOLVED") {
      list = list.filter((c) => c.status === "RESOLVED");
    }

    if (search) {
      list = list.filter((c) =>
        c.title?.toLowerCase().includes(search.toLowerCase())
      );
    }

    return list;
  }, [complaints, search, filter]);

  const stats = {
    total: complaints.length,
    pending: complaints.filter((c) => c.status === "OPEN").length,
    inProgress: complaints.filter((c) => c.status === "IN_PROGRESS").length,
    resolved: complaints.filter((c) => c.status === "RESOLVED").length,
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
                  {user?.staff?.name || "Staff"}
                </span>
              </h1>

              <p className="mt-5 max-w-2xl text-base leading-8 text-slate-300 sm:text-lg">
                Manage assigned grievances, review live progress, and handle
                complaint resolution through a modern and intelligent staff
                workspace.
              </p>

              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <Button
                  onClick={loadData}
                  className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
                >
                  <ArrowRight className="mr-2 h-4 w-4" />
                  Refresh Complaints
                </Button>

                <Button
                  variant="outline"
                  onClick={() =>
                    complaintsSectionRef.current?.scrollIntoView({
                      behavior: "smooth",
                      block: "start",
                    })
                  }
                  className="h-12 rounded-xl border-white/15 bg-white/5 text-white hover:bg-white/10"
                >
                  View Assigned Complaints
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

              <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-amber-500/15 to-orange-500/10 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-amber-300">Pending Rate</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.total > 0
                    ? `${Math.round((stats.pending / stats.total) * 100)}%`
                    : "0%"}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Based on open assigned grievances
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
                  All complaints assigned
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 via-violet-500 to-blue-500 shadow-lg shadow-indigo-500/30">
                <Layers className="h-6 w-6 text-white" />
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
                  Currently under process
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
                  Successfully resolved
                </p>
              </div>
              <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-400 via-green-500 to-teal-500 shadow-lg shadow-emerald-500/30">
                <BadgeCheck className="h-6 w-6 text-white" />
              </div>
            </div>
          </div>
        </motion.div>

        {/* Search */}
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
                placeholder="Search assigned complaints by title..."
                className="h-12 rounded-2xl border-white/10 bg-slate-900/40 pl-11 text-white placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-indigo-500"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>

            <Button
              onClick={loadData}
              className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
            >
              <ArrowRight className="mr-2 h-4 w-4" />
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
            onClick={() => setFilter("PENDING")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              filter === "PENDING"
                ? "bg-amber-500 text-white shadow-lg shadow-amber-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            Pending
          </button>

          <button
            onClick={() => setFilter("IN_PROGRESS")}
            className={`rounded-xl px-5 py-2 text-sm font-medium transition ${
              filter === "IN_PROGRESS"
                ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/30"
                : "bg-white/5 text-slate-300 hover:bg-white/10"
            }`}
          >
            In Progress
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
        </motion.div>

        {/* Complaints */}
        <motion.div
          ref={complaintsSectionRef}
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
                Review complaint history, current progress, and assigned records.
              </p>
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
              Showing{" "}
              <span className="font-semibold text-white">{filtered.length}</span>{" "}
              complaint{filtered.length !== 1 ? "s" : ""}
            </div>
          </div>

          {filtered.length > 0 ? (
            <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
              {filtered.map((c, index) => (
                <motion.div
                  key={c.complaintId}
                  initial={{ opacity: 0, y: 22 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.35, delay: index * 0.05 }}
                >
                  <ComplaintCard
                    complaint={c}
                    onClick={() => navigate(`/complaint/${c.complaintId}`)}
                  />
                </motion.div>
              ))}
            </div>
          ) : (
            <div className="rounded-[2rem] border border-dashed border-white/15 bg-white/6 p-12 text-center backdrop-blur-xl">
              <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-3xl bg-white/10">
                <FileText className="h-8 w-8 text-slate-300" />
              </div>

              <h3 className="mb-3 text-2xl font-bold text-white">
                No complaints found
              </h3>

              <p className="mx-auto mb-8 max-w-xl leading-7 text-slate-400">
                There are no assigned complaints matching your current search or
                filter. Try another keyword or refresh the grievance list.
              </p>

              <Button
                onClick={loadData}
                className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 px-6 font-semibold transition-all hover:scale-[1.02]"
              >
                <ArrowRight className="mr-2 h-4 w-4" />
                Refresh Complaints
              </Button>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}