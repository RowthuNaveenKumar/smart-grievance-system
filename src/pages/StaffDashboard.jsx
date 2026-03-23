import React, { useState, useEffect, useMemo } from "react";
import { api } from "@/services/api";
import { useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import ComplaintCard from "@/components/complaints/ComplaintCard";

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
} from "lucide-react";

import { motion } from "framer-motion";
import { useUser } from "@/context/UserContext";

export default function StaffDashboard() {
  const navigate = useNavigate();
  const { user } = useUser();

  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);

  const [searchTerm, setSearchTerm] = useState("");
  const [activeTab, setActiveTab] = useState("all");

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const res = await api.get("/complaints/assigned");
      setComplaints(res.data);
    } catch (err) {
      console.error("Error loading dashboard", err);
    } finally {
      setLoading(false);
    }
  };

  const filteredComplaints = useMemo(() => {
    let list = complaints;

    if (activeTab === "pending") {
      list = list.filter((c) => c.status === "OPEN");
    }

    if (activeTab === "in_progress") {
      list = list.filter((c) => c.status === "IN_PROGRESS");
    }

    if (activeTab === "resolved") {
      list = list.filter((c) => c.status === "RESOLVED");
    }

    if (searchTerm) {
      list = list.filter((c) =>
        c.title?.toLowerCase().includes(searchTerm.toLowerCase()),
      );
    }

    return list;
  }, [complaints, searchTerm, activeTab]);

  const stats = {
    total: complaints.length,
    pending: complaints.filter((c) => c.status === "OPEN").length,
    inProgress: complaints.filter((c) => c.status === "IN_PROGRESS").length,
    resolved: complaints.filter((c) => c.status === "RESOLVED").length,
  };

  return (
    <div className="relative min-h-screen bg-slate-950 text-white">
      <div className="relative z-10 max-w-7xl mx-auto px-4 py-8">
        {/* Header */}

        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 flex items-center justify-between rounded-2xl bg-white/5 p-5 border border-white/10"
        >
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 flex items-center justify-center rounded-xl bg-indigo-500">
              <ShieldCheck className="h-5 w-5" />
            </div>

            <div>
              <h2 className="text-lg font-semibold">Staff Dashboard</h2>
              <p className="text-sm text-slate-400">
                Manage assigned complaints
              </p>
            </div>
          </div>

          <div className="text-sm text-slate-300">
            {user?.staff?.name || "Staff"}
          </div>
        </motion.div>

        {/* Stats */}

        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <StatCard icon={<FileText />} title="Total" value={stats.total} />
          <StatCard icon={<Clock />} title="Pending" value={stats.pending} />
          <StatCard
            icon={<AlertCircle />}
            title="In Progress"
            value={stats.inProgress}
          />
          <StatCard
            icon={<CheckCircle />}
            title="Resolved"
            value={stats.resolved}
          />
        </div>

        {/* Search */}

        <div className="mb-6 flex items-center gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />

            <Input
              placeholder="Search complaints..."
              className="pl-9 bg-slate-900 border-white/10"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <Button onClick={loadData}>
            Refresh
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        </div>

        {/* Tabs */}

        <div className="flex gap-3 mb-6">
          <TabButton
            label="All"
            value="all"
            active={activeTab}
            setActive={setActiveTab}
          />
          <TabButton
            label="Pending"
            value="pending"
            active={activeTab}
            setActive={setActiveTab}
          />
          <TabButton
            label="In Progress"
            value="in_progress"
            active={activeTab}
            setActive={setActiveTab}
          />
          <TabButton
            label="Resolved"
            value="resolved"
            active={activeTab}
            setActive={setActiveTab}
          />
        </div>

        {/* Complaints */}

        {loading ? (
          <div className="text-center py-20">Loading complaints...</div>
        ) : filteredComplaints.length > 0 ? (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {filteredComplaints.map((complaint) => (
              <ComplaintCard
                key={complaint.complaintId}
                complaint={complaint}
                onClick={() => navigate(`/complaint/${complaint.complaintId}`)}
              />
            ))}
          </div>
        ) : (
          <div className="text-center py-20 text-slate-400">
            No assigned complaints
          </div>
        )}
      </div>
    </div>
  );
}

/* ---------- Components ---------- */

function StatCard({ icon, title, value }) {
  return (
    <div className="rounded-xl bg-white/5 border border-white/10 p-6 flex justify-between">
      <div>
        <p className="text-sm text-slate-400">{title}</p>
        <h3 className="text-2xl font-bold">{value}</h3>
      </div>

      <div className="opacity-60">{icon}</div>
    </div>
  );
}

function TabButton({ label, value, active, setActive }) {
  return (
    <button
      onClick={() => setActive(value)}
      className={`px-4 py-2 rounded-lg text-sm ${
        active === value
          ? "bg-indigo-500 text-white"
          : "bg-white/5 text-slate-300"
      }`}
    >
      {label}
    </button>
  );
}
