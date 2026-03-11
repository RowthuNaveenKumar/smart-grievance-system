import React, { useEffect, useState } from "react";
import { api } from "@/services/api";
import { useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

import StatsCard from "@/components/dashboard/StatsCard";
import ComplaintCard from "@/components/complaints/ComplaintCard";

import {
  FileText,
  Clock,
  CheckCircle,
  XCircle,
  Plus,
  Search,
} from "lucide-react";

export default function StudentDashboard() {
  const [user, setUser] = useState(null);
  const [complaints, setComplaints] = useState([]);
  const [search, setSearch] = useState("");

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

  const filtered = complaints.filter((c) =>
    c.title.toLowerCase().includes(search.toLowerCase()),
  );

  const stats = {
    total: complaints.length,

    active: complaints.filter((c) => c.status === "OPEN").length,

    resolved: complaints.filter((c) => c.status === "RESOLVED").length,

    closed: complaints.filter((c) => c.status === "CLOSED").length,
  };

  return (
    <div className="min-h-screen bg-slate-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}

        <h1 className="text-3xl font-bold mb-2">
          Welcome back, {user?.profile?.name || "Student"}
        </h1>

        <p className="text-gray-500 mb-6">Track and manage your grievances</p>

        {/* Stats */}

        <div className="grid grid-cols-4 gap-6 mb-6">
          <StatsCard
            title="Total Complaints"
            value={stats.total}
            icon={FileText}
          />
          <StatsCard title="Active" value={stats.active} icon={Clock} />
          <StatsCard
            title="Resolved"
            value={stats.resolved}
            icon={CheckCircle}
          />
          <StatsCard title="Closed" value={stats.closed} icon={XCircle} />
        </div>

        {/* Search + Button */}

        <div className="flex gap-4 mb-6">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-3 w-4 h-4 text-gray-400" />

            <Input
              placeholder="Search complaints..."
              className="pl-10"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          <Button onClick={() => navigate("/submit")} className="bg-indigo-600">
            <Plus className="mr-2 w-4 h-4" />
            Submit New Complaint
          </Button>
        </div>

        {/* Complaints */}

        <div className="grid grid-cols-2 gap-6">
          {filtered.map((c) => (
            <ComplaintCard
              key={c.complaintId}
              complaint={c}
              onClick={() => navigate(`/complaint/${c.complaintId}`)}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
