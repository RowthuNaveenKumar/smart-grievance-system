import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import moment from "moment";
import { motion } from "framer-motion";

import { api } from "@/services/api";

import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

import { useUser } from "@/context/UserContext";

import {
  ArrowLeft,
  Clock,
  User,
  FileText,
  AlertTriangle,
  Loader2,
  ChevronRight,
  FolderOpen,
  ShieldCheck,
} from "lucide-react";

export default function ComplaintDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);

  const { user } = useUser();

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

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 text-white flex items-center justify-center relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950" />
        <Loader2 className="w-10 h-10 animate-spin text-indigo-400" />
      </div>
    );
  }

  if (!complaint) {
    return (
      <div className="min-h-screen bg-slate-950 text-white flex items-center justify-center relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950" />
        <div className="text-center py-20">
          <AlertTriangle className="w-12 h-12 text-slate-400 mx-auto mb-4" />
          <p className="text-slate-300">Complaint not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-white overflow-hidden relative">
      <div className="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950" />
      <div className="absolute top-0 left-0 -z-10 h-72 w-72 rounded-full bg-indigo-500/20 blur-3xl" />
      <div className="absolute bottom-0 right-0 -z-10 h-72 w-72 rounded-full bg-blue-500/20 blur-3xl" />

      <div className="max-w-5xl mx-auto px-4 py-6 space-y-6">
        <Button
          variant="ghost"
          onClick={() => navigate(-1)}
          className="flex items-center text-slate-300 hover:text-white hover:bg-white/10"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back
        </Button>

        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <Card className="rounded-3xl border border-white/10 bg-white/10 backdrop-blur-xl shadow-xl overflow-hidden text-white">
            <div className="p-8 bg-gradient-to-r from-indigo-500 to-blue-500 text-white">
              <h1 className="text-2xl font-bold">{complaint.title}</h1>
              <p className="text-indigo-100 text-sm mt-1">
                Submitted {moment(complaint.createdAt).fromNow()}
              </p>
            </div>

            <CardContent className="p-8 space-y-8">
              <p className="text-slate-200 text-lg leading-relaxed">
                {complaint.description}
              </p>

              <div className="flex flex-wrap gap-3 mt-4">
                <Badge className="bg-indigo-500 text-white border-0">
                  {complaint.category}
                </Badge>
                <Badge className="bg-purple-500 text-white border-0">
                  Priority: {complaint.priority}
                </Badge>
                <Badge className="bg-emerald-500 text-white border-0">
                  {complaint.status}
                </Badge>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <InfoBox
                  title="Category"
                  value={complaint.category}
                  icon={<FolderOpen className="w-4 h-4 text-indigo-300" />}
                />
                <InfoBox
                  title="Status"
                  value={complaint.status}
                  icon={<ShieldCheck className="w-4 h-4 text-emerald-300" />}
                />
                <InfoBox
                  title="Assigned To"
                  value={complaint.assignedTo || "Unassigned"}
                  icon={<User className="w-4 h-4 text-indigo-300" />}
                />
                <InfoBox
                  title="Created At"
                  value={moment(complaint.createdAt).format("DD MMM YYYY")}
                  icon={<Clock className="w-4 h-4 text-amber-300" />}
                />
              </div>

              {complaint.files?.length > 0 && (
                <div className="space-y-3">
                  <p className="text-sm font-semibold text-slate-200">
                    Attachments
                  </p>

                  <div className="flex flex-col gap-3">
                    {complaint.files.map((file, i) => (
                      <a
                        key={i}
                        href={file}
                        target="_blank"
                        rel="noreferrer"
                        className="flex items-center justify-between bg-white/5 hover:bg-white/10 px-4 py-3 rounded-2xl text-indigo-200 transition border border-white/10"
                      >
                        <div className="flex items-center gap-3">
                          <FileText className="w-5 h-5 text-indigo-300" />
                          <span>Attachment {i + 1}</span>
                        </div>
                        <ChevronRight className="w-4 h-4 text-slate-400" />
                      </a>
                    ))}
                  </div>
                </div>
              )}

              <div className="mt-6">
                <h3 className="text-lg font-semibold text-white mb-3">
                  Activity Timeline
                </h3>

                <div className="space-y-5">
                  {complaint.timeline?.map((t, index) => (
                    <TimelineItem key={index} item={t} />
                  ))}
                </div>
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
    <div className="bg-white/5 rounded-2xl p-4 border border-white/10 shadow-sm">
      <p className="text-[11px] uppercase tracking-wider text-slate-400 font-semibold mb-1">
        {title}
      </p>
      <div className="flex items-center gap-2">
        {icon}
        <span className="text-sm font-medium text-slate-200">{value}</span>
      </div>
    </div>
  );
}

function TimelineItem({ item }) {
  return (
    <div className="relative pl-6 border-l-2 border-indigo-400/30">
      <div className="absolute -left-[6px] top-1 w-3 h-3 bg-indigo-400 rounded-full"></div>

      <p className="text-sm font-semibold text-white">{item.action}</p>
      <p className="text-xs text-slate-300 mb-1">
        {item.fromStatus} → {item.toStatus}
      </p>
      <p className="text-xs text-slate-400">
        by {item.performedBy} • {moment(item.createdAt).format("DD MMM, HH:mm")}
      </p>
    </div>
  );
}