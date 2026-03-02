import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import moment from "moment";

import { api } from "@/services/api";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";

import {
  ArrowLeft,
  FileText,
  Clock,
  User,
  AlertTriangle,
  Loader2,
} from "lucide-react";

export default function ComplaintDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [complaint, setComplaint] = useState(null);
  const [loading, setLoading] = useState(true);

  // Load complaint
  useEffect(() => {
    loadComplaint();
  }, [id]);

  const loadComplaint = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await api.get(`/complaints/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      setComplaint(response.data);
    } catch (err) {
      console.error("Error loading complaint:", err);
    }

    setLoading(false);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
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

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Back button */}
      <Button
        variant="ghost"
        onClick={() => navigate(-1)}
        className="text-slate-500 hover:text-slate-700 -ml-2"
      >
        <ArrowLeft className="w-4 h-4 mr-2" /> Back
      </Button>

      {/* CARD */}
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <Card className="rounded-2xl shadow-sm border-slate-100">
          <div className="bg-linear-to-r from-indigo-600 to-purple-600 p-6">
            <h1 className="text-xl md:text-2xl font-bold text-white">
              {complaint.title}
            </h1>
            <p className="text-indigo-200 text-sm mt-1">
              Submitted {moment(complaint.createdAt).fromNow()}
            </p>
          </div>

          <CardContent className="p-6 space-y-6">
            <p className="text-slate-700 leading-relaxed whitespace-pre-wrap">
              {complaint.description}
            </p>

            {/* TAGS */}
            <div className="flex gap-2 mt-3">
              <Badge className="bg-indigo-600 text-white">
                {complaint.category}
              </Badge>
              <Badge className="bg-purple-600 text-white">
                Priority: {complaint.priority}
              </Badge>
              <Badge className="bg-green-600 text-white">
                {complaint.status}
              </Badge>
            </div>

            <hr className="my-6" />

            {/* METADATA GRID */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-slate-50 rounded-xl p-3">
                <p className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mb-1">
                  Category
                </p>
                <span className="text-sm font-medium text-slate-700">
                  {complaint.category}
                </span>
              </div>

              <div className="bg-slate-50 rounded-xl p-3">
                <p className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mb-1">
                  Status
                </p>
                <span className="text-sm font-medium text-slate-700">
                  {complaint.status}
                </span>
              </div>

              <div className="bg-slate-50 rounded-xl p-3">
                <p className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mb-1">
                  Assigned To
                </p>
                <div className="flex items-center gap-1.5">
                  <User className="w-4 h-4 text-indigo-500" />
                  <span className="text-sm font-medium text-slate-700">
                    {complaint.assignedTo || "Unassigned"}
                  </span>
                </div>
              </div>

              <div className="bg-slate-50 rounded-xl p-3">
                <p className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mb-1">
                  Created At
                </p>
                <div className="flex items-center gap-1.5">
                  <Clock className="w-4 h-4 text-amber-500" />
                  <span className="text-sm font-medium text-slate-700">
                    {moment(complaint.createdAt).format("DD MMM YYYY")}
                  </span>
                </div>
              </div>
            </div>

            {/* FILE ATTACHMENTS */}
            {complaint.files?.length > 0 && (
              <div className="mt-6">
                <p className="text-sm font-semibold text-slate-600 mb-2">
                  Attachments
                </p>
                <div className="flex flex-wrap gap-2">
                  {complaint.files.map((file, i) => (
                    <a
                      key={i}
                      href={file}
                      target="_blank"
                      rel="noreferrer"
                      className="flex items-center gap-2 bg-slate-50 hover:bg-slate-100 rounded-lg px-3 py-2 text-sm text-indigo-600 transition"
                    >
                      <FileText className="w-4 h-4" />
                      Attachment {i + 1}
                    </a>
                  ))}
                </div>
              </div>
            )}

            {/* TIMELINE */}
            <div>
              <h3 className="font-semibold text-slate-700 mb-3">
                Activity Timeline
              </h3>

              <div className="space-y-4">
                {complaint.timeline?.map((t, i) => (
                  <div
                    key={i}
                    className="border-l-2 border-indigo-300 pl-4 py-2 space-y-1"
                  >
                    <p className="text-sm font-semibold text-slate-700">
                      {t.action}
                    </p>
                    <p className="text-xs text-slate-500">
                      {t.fromStatus} → {t.toStatus}
                    </p>
                    <p className="text-xs text-slate-400">
                      by {t.performedBy} •{" "}
                      {moment(t.createdAt).format("DD MMM, HH:mm")}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}