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
      const token = localStorage.getItem("token");
      const res = await api.get(`/complaints/${id}`);

      setComplaint(res.data);
    } catch (err) {
      console.error("Error loading complaint:", err);
    }
    setLoading(false);
  };

  // ----------------------
  // Loading Screen
  // ----------------------
  if (loading) {
    return (
      <div className="flex items-center justify-center h-[70vh]">
        <Loader2 className="w-10 h-10 animate-spin text-indigo-600" />
      </div>
    );
  }

  if (!complaint) {
    return (
      <div className="text-center py-20">
        <AlertTriangle className="w-12 h-12 text-slate-300 mx-auto mb-4" />
        <p className="text-slate-500">Complaint not found</p>
      </div>
    );
  }

  // ----------------------
  // UI STARTS
  // ----------------------
  return (
    <div className="max-w-5xl mx-auto px-4 py-4 space-y-6">

      {/* Back Button */}
      <Button
        variant="ghost"
        onClick={() => navigate(-1)}
        className="flex items-center text-slate-600 hover:text-slate-900"
      >
        <ArrowLeft className="w-4 h-4 mr-2" />
        Back
      </Button>

      {/* Main Card */}
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <Card className="rounded-2xl border-0 shadow-lg overflow-hidden">

          {/* Header Gradient */}
          <div className="p-8 bg-gradient-to-r from-indigo-600 to-purple-600 text-white">
            <h1 className="text-2xl font-bold">{complaint.title}</h1>
            <p className="text-indigo-200 text-sm mt-1">
              Submitted {moment(complaint.createdAt).fromNow()}
            </p>
          </div>

          <CardContent className="p-8 space-y-8">

            {/* Description */}
            <p className="text-slate-700 text-lg leading-relaxed">
              {complaint.description}
            </p>

            {/* Tags */}
            <div className="flex flex-wrap gap-3 mt-4">
              <Badge className="bg-indigo-600">{complaint.category}</Badge>
              <Badge className="bg-purple-600">Priority: {complaint.priority}</Badge>
              <Badge className="bg-green-600">{complaint.status}</Badge>
            </div>

            {/* Info Boxes */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <InfoBox title="Category" value={complaint.category} />
              <InfoBox title="Status" value={complaint.status} />
              <InfoBox
                title="Assigned To"
                value={complaint.assignedTo || "Unassigned"}
                icon={<User className="w-4 h-4 text-indigo-500" />}
              />
              <InfoBox
                title="Created At"
                value={moment(complaint.createdAt).format("DD MMM YYYY")}
                icon={<Clock className="w-4 h-4 text-amber-500" />}
              />
            </div>

            {/* Attachments */}
            {complaint.files?.length > 0 && (
              <div className="space-y-3">
                <p className="text-sm font-semibold text-slate-700">
                  Attachments
                </p>

                <div className="flex flex-col gap-3">
                  {complaint.files.map((file, i) => (
                    <a
                      key={i}
                      href={file}
                      target="_blank"
                      rel="noreferrer"
                      className="flex items-center justify-between bg-slate-50 hover:bg-slate-100 px-4 py-3 rounded-xl text-indigo-600 transition border border-slate-200"
                    >
                      <div className="flex items-center gap-3">
                        <FileText className="w-5 h-5 text-indigo-500" />
                        <span>Attachment {i + 1}</span>
                      </div>
                      <ChevronRight className="w-4 h-4 text-slate-400" />
                    </a>
                  ))}
                </div>
              </div>
            )}

            {/* Timeline */}
            <div className="mt-6">
              <h3 className="text-lg font-semibold text-slate-700 mb-3">
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
  );
}

// -----------------------------
// Small Components
// -----------------------------

function InfoBox({ title, value, icon }) {
  return (
    <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 shadow-sm">
      <p className="text-[11px] uppercase tracking-wider text-slate-400 font-semibold mb-1">
        {title}
      </p>
      <div className="flex items-center gap-2">
        {icon}
        <span className="text-sm font-medium text-slate-700">{value}</span>
      </div>
    </div>
  );
}

function TimelineItem({ item }) {
  return (
    <div className="relative pl-6 border-l-2 border-indigo-200">
      <div className="absolute -left-[6px] top-1 w-3 h-3 bg-indigo-500 rounded-full"></div>

      <p className="text-sm font-semibold text-slate-800">{item.action}</p>
      <p className="text-xs text-slate-500 mb-1">
        {item.fromStatus} → {item.toStatus}
      </p>
      <p className="text-xs text-slate-400">
        by {item.performedBy} • {moment(item.createdAt).format("DD MMM, HH:mm")}
      </p>
    </div>
  );
}