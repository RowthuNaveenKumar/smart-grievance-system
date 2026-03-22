import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
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
} from "lucide-react";

export default function ComplaintDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);

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
          className:
            "bg-cyan-500/15 text-cyan-300 border border-cyan-400/20",
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
          className:
            "bg-rose-500/15 text-rose-300 border border-rose-400/20",
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
            onClick={() => navigate(-1)}
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
                Complaint Overview
              </div>

              <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <h1 className="text-3xl font-bold text-white sm:text-4xl">
                    {complaint.title}
                  </h1>
                  <p className="mt-3 text-sm text-slate-300">
                    Submitted {moment(complaint.createdAt).fromNow()}
                  </p>
                </div>

                <div className="flex flex-wrap gap-3">
                  <Badge className="border border-indigo-400/20 bg-indigo-500/15 px-3 py-1 text-indigo-200">
                    {complaint.category}
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

              {/* Info boxes */}
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                <InfoBox
                  title="Category"
                  value={complaint.category}
                  icon={<FolderOpen className="h-4 w-4 text-indigo-300" />}
                />
                <InfoBox
                  title="Status"
                  value={complaint.status}
                  icon={<ShieldCheck className="h-4 w-4 text-emerald-300" />}
                />
                <InfoBox
                  title="Assigned To"
                  value={complaint.assignedTo || "Unassigned"}
                  icon={<User className="h-4 w-4 text-cyan-300" />}
                />
                <InfoBox
                  title="Created At"
                  value={moment(complaint.createdAt).format("DD MMM YYYY")}
                  icon={<CalendarDays className="h-4 w-4 text-amber-300" />}
                />
              </div>

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
                        href={file}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center justify-between rounded-2xl border border-white/10 bg-slate-900/30 px-4 py-4 text-indigo-200 transition hover:bg-white/10"
                      >
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-500/15">
                            <FileText className="h-5 w-5 text-indigo-300" />
                          </div>
                          <span className="font-medium">Attachment {i + 1}</span>
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
        <p className="text-xs text-slate-400">
          by {item.performedBy} • {moment(item.createdAt).format("DD MMM, HH:mm")}
        </p>
      </div>
    </div>
  );
}