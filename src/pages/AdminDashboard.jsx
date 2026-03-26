import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/services/api";

import { Button } from "@/components/ui/button";
import {
  Users,
  FileText,
  Building2,
  TrendingUp,
  Clock,
  CheckCircle,
  Activity,
  ShieldCheck,
  Sparkles,
  ArrowRight,
  BarChart3,
} from "lucide-react";

import { motion } from "framer-motion";

export default function AdminDashboard() {
  const [stats, setStats] = useState({
    totalComplaints: 0,
    activeComplaints: 0,
    resolvedComplaints: 0,
    totalDepartments: 0,
    totalStaff: 0,
    avgResolutionTime: "0h",
  });

  const [recentComplaints, setRecentComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [mouse, setMouse] = useState({ x: 0, y: 0 });
  const [user, setUser] = useState(null);

  const navigate = useNavigate();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      const me = await api.get("/auth/me");
      setUser(me.data);

      const complaintsRes = await api.get("/complaints");
      const departmentsRes = await api.get("/departments");
      const staffRes = await api.get("/staff");

      const complaints = complaintsRes.data || [];
      const departments = departmentsRes.data || [];
      const staff = staffRes.data || [];

      const activeComplaints = complaints.filter((c) =>
        ["submitted", "assigned", "in_progress", "info_requested"].includes(c.status)
      );

      const resolvedComplaints = complaints.filter(
        (c) => c.status === "resolved" || c.status === "closed"
      );

      setStats({
        totalComplaints: complaints.length,
        activeComplaints: activeComplaints.length,
        resolvedComplaints: resolvedComplaints.length,
        totalDepartments: departments.length,
        totalStaff: staff.length,
        avgResolutionTime: "48h",
      });

      setRecentComplaints(complaints.slice(0, 5));
    } catch (error) {
      console.error("Error loading dashboard:", error);
    } finally {
      setLoading(false);
    }
  };

  const quickActions = [
    {
      title: "Manage Departments",
      description: "Add, edit, or remove departments",
      icon: Building2,
      action: () => navigate("/manage-departments"),
      color: "from-indigo-500 via-blue-500 to-cyan-500",
    },
    {
      title: "View All Complaints",
      description: "Browse and manage complaints",
      icon: FileText,
      action: () => navigate("/all-complaints"),
      color: "from-purple-500 via-indigo-500 to-blue-500",
    },
    {
      title: "Analytics",
      description: "View detailed reports",
      icon: TrendingUp,
      action: () => navigate("/analytics"),
      color: "from-emerald-500 via-green-500 to-teal-500",
    },
    {
      title: "Manage Users",
      description: "Staff and student management",
      icon: Users,
      action: () => navigate("/manage-users"),
      color: "from-amber-400 via-orange-500 to-yellow-500",
    },
  ];

  const getStatusStyle = (status) => {
    if (status === "closed" || status === "resolved") {
      return "bg-emerald-500/15 text-emerald-300 border border-emerald-400/20";
    }
    if (status === "in_progress") {
      return "bg-cyan-500/15 text-cyan-300 border border-cyan-400/20";
    }
    return "bg-amber-500/15 text-amber-300 border border-amber-400/20";
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
                Admin Workspace
              </h2>
              <p className="text-xs text-slate-300">
                System overview and management dashboard
              </p>
            </div>
          </div>

          <div className="hidden items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300 md:flex">
            <Activity className="h-4 w-4 text-emerald-400" />
            Live admin panel
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
                   {user?.profile?.name || "Admin"}
                </span>
              </h1>

              <p className="mt-5 max-w-2xl text-base leading-8 text-slate-300 sm:text-lg">
                Oversee complaints, departments, staff operations, and platform
                activity through a modern admin control center.
              </p>

              <div className="mt-8 flex flex-col gap-4 sm:flex-row">
                <Button
                  onClick={() => navigate("/all-complaints")}
                  className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-6 font-semibold text-white shadow-lg shadow-indigo-500/30 transition-all hover:scale-[1.02]"
                >
                  <FileText className="mr-2 h-4 w-4" />
                  Open Complaints
                </Button>

                <Button
                  onClick={() => navigate("/analytics")}
                  variant="outline"
                  className="h-12 rounded-xl border-white/15 bg-white/5 px-6 font-semibold text-white hover:bg-white/10"
                >
                  <BarChart3 className="mr-2 h-4 w-4" />
                  View Analytics
                </Button>
              </div>
            </div>

            <div className="flex flex-col justify-between gap-4">
              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-slate-400">Quick Summary</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.totalComplaints}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Total complaints in system
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-emerald-500/15 to-cyan-500/10 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-emerald-300">Resolved Rate</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.totalComplaints > 0
                    ? `${Math.round(
                        (stats.resolvedComplaints / stats.totalComplaints) * 100
                      )}%`
                    : "0%"}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Based on completed complaints
                </p>
              </div>

              <div className="rounded-2xl border border-white/10 bg-white/8 p-5 backdrop-blur-xl">
                <p className="mb-2 text-sm text-slate-400">Currently Active</p>
                <h3 className="text-3xl font-bold text-white">
                  {stats.activeComplaints}
                </h3>
                <p className="mt-1 text-sm text-slate-300">
                  Complaints needing attention
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
          {[
            {
              label: "Total Complaints",
              value: stats.totalComplaints,
              sub: "All records",
              icon: FileText,
              color:
                "from-indigo-500 via-violet-500 to-blue-500 shadow-indigo-500/30",
            },
            {
              label: "Active Cases",
              value: stats.activeComplaints,
              sub: "Pending and in progress",
              icon: Clock,
              color:
                "from-amber-400 via-orange-500 to-yellow-500 shadow-amber-500/30",
            },
            {
              label: "Resolved Cases",
              value: stats.resolvedComplaints,
              sub: "Completed successfully",
              icon: CheckCircle,
              color:
                "from-emerald-400 via-green-500 to-teal-500 shadow-emerald-500/30",
            },
            {
              label: "Departments",
              value: stats.totalDepartments,
              sub: "Total departments",
              icon: Building2,
              color:
                "from-cyan-400 via-sky-500 to-blue-500 shadow-cyan-500/30",
            },
          ].map((item, index) => (
            <div
              key={index}
              className="group relative overflow-hidden rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-white/5 via-transparent to-white/0 opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
              <div className="relative flex items-start justify-between">
                <div>
                  <p className="text-sm font-medium text-slate-300">{item.label}</p>
                  <h3 className="mt-3 text-3xl font-bold text-white">
                    {item.value}
                  </h3>
                  <p className="mt-2 text-xs text-slate-400">{item.sub}</p>
                </div>
                <div
                  className={`flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br ${item.color} shadow-lg`}
                >
                  <item.icon className="h-6 w-6 text-white" />
                </div>
              </div>
            </div>
          ))}
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 22 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="mb-8"
        >
          <div className="mb-6">
            <h2 className="text-2xl font-bold text-white sm:text-3xl">
              Quick Actions
            </h2>
            <p className="mt-2 text-slate-400">
              Access important administrative tools and workflows.
            </p>
          </div>

          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
            {quickActions.map((action, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 22 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.35, delay: index * 0.05 }}
                className="group cursor-pointer rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl"
                onClick={action.action}
              >
                <div
                  className={`mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br ${action.color} shadow-lg`}
                >
                  <action.icon className="h-6 w-6 text-white" />
                </div>

                <h3 className="text-lg font-semibold text-white">{action.title}</h3>
                <p className="mt-2 text-sm leading-6 text-slate-400">
                  {action.description}
                </p>

                <div className="mt-5 flex items-center text-sm font-medium text-cyan-300">
                  Open <ArrowRight className="ml-2 h-4 w-4" />
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9 }}
        >
          <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <h2 className="text-2xl font-bold text-white sm:text-3xl">
                Recent Complaints
              </h2>
              <p className="mt-2 text-slate-400">
                Latest complaint activity across the platform.
              </p>
            </div>

            <div className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
              Total Staff{" "}
              <span className="font-semibold text-white">{stats.totalStaff}</span>
            </div>
          </div>

          {loading ? (
            <div className="rounded-[2rem] border border-white/10 bg-white/6 p-12 text-center backdrop-blur-xl">
              <div className="mx-auto mb-5 h-10 w-10 animate-spin rounded-full border-4 border-indigo-500 border-r-transparent" />
              <p className="text-slate-300">Loading dashboard data...</p>
            </div>
          ) : recentComplaints.length > 0 ? (
            <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
              {recentComplaints.map((complaint, index) => (
                <motion.div
                  key={complaint.id}
                  initial={{ opacity: 0, y: 22 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.35, delay: index * 0.05 }}
                  className="rounded-[1.75rem] border border-white/10 bg-white/8 p-6 shadow-lg backdrop-blur-xl transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <h3 className="text-lg font-semibold text-white">
                        {complaint.title}
                      </h3>
                      <p className="mt-2 text-sm text-slate-400">
                        {complaint.student_name || "Unknown Student"}
                      </p>
                    </div>

                    <span
                      className={`rounded-full px-3 py-1 text-xs font-medium capitalize ${getStatusStyle(
                        complaint.status
                      )}`}
                    >
                      {complaint.status?.replace("_", " ")}
                    </span>
                  </div>

                  <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-300">
                    {complaint.description || "No description available"}
                  </p>

                  <Button
                    onClick={() => navigate(`/complaint/${complaint.id}`)}
                    className="mt-5 h-11 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 px-5 font-semibold text-white shadow-lg shadow-indigo-500/20 transition hover:scale-[1.01]"
                  >
                    <Sparkles className="mr-2 h-4 w-4" />
                    View Details
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
                There are no complaints available in the system right now.
              </p>

              <Button
                onClick={loadDashboardData}
                className="h-12 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 px-6 font-semibold transition-all hover:scale-[1.02]"
              >
                Refresh Dashboard
              </Button>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}