import React, { useState } from "react";
import { api } from "@/services/api";
import { useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import {
  Upload,
  X,
  Send,
  Sparkles,
  FileText,
  Loader2,
  CheckCircle2,
} from "lucide-react";

import { motion } from "framer-motion";

export default function SubmitComplaint() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    title: "",
    description: "",
  });

  const [files, setFiles] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  const [mlPrediction, setMlPrediction] = useState(null);
  const [predicting, setPredicting] = useState(false);

  // =============================
  // 🔮 AI Prediction
  // =============================

  const predictCategory = async () => {
    if (!form.title || !form.description) return;

    try {
      setPredicting(true);

      const res = await api.post("/complaints/predict", {
        title: form.title,
        complaint_text: form.description,
      });

      setMlPrediction({
        predictedDepartment: res.data.predicted_department,
        predictedPriority: res.data.predicted_priority,
        confidence: res.data.confidence,
      });
    } catch (err) {
      console.error("ML prediction error:", err);
    }

    setPredicting(false);
  };

  // =============================
  // 📤 Submit Complaint
  // =============================

  const handleSubmit = async (e) => {
    e.preventDefault();

    const storedUser = JSON.parse(localStorage.getItem("user"));
    const token = localStorage.getItem("token");

    const fd = new FormData();

    const complaintData = {
      title: form.title,
      description: form.description,
      category: (mlPrediction?.predictedDepartment || "GENERAL").toUpperCase(),
      priority: (mlPrediction?.predictedPriority || "LOW").toUpperCase(),
    };

    fd.append(
      "request",
      new Blob([JSON.stringify(complaintData)], {
        type: "application/json",
      }),
    );

    files.forEach((file) => {
      fd.append("files", file);
    });

    const res = await api.post("/complaints", fd, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });

    navigate(`/complaint/${res.data.complaintId}`);
  };

  // =============================
  // 📁 File Upload
  // =============================

  const handleFileUpload = (e) => {
    const newFiles = Array.from(e.target.files);
    setFiles((prev) => [...prev, ...newFiles]);
  };

  const removeFile = (index) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  // =============================
  // UI
  // =============================

  return (
    <div className="max-w-3xl mx-auto px-4 py-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <Card className="rounded-2xl shadow-sm border-slate-200 overflow-hidden">
          {/* HEADER */}
          <CardHeader className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white p-8">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center">
                <FileText className="w-7 h-7" />
              </div>

              <div>
                <CardTitle className="text-xl">Submit a Complaint</CardTitle>

                <p className="text-indigo-200 text-sm mt-1">
                  Describe your issue and we will route it to the right team
                </p>
              </div>
            </div>
          </CardHeader>

          {/* CONTENT */}
          <CardContent className="p-8 space-y-6">
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Title */}
              <div className="space-y-2">
                <Label className="font-medium text-slate-700">
                  Complaint Title *
                </Label>

                <Input
                  placeholder="Example: Fan not working"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  className="rounded-xl h-12 border-slate-300"
                />
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label className="font-medium text-slate-700">
                  Detailed Description *
                </Label>

                <Textarea
                  placeholder="Explain your issue..."
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                  className="rounded-xl min-h-[130px] border-slate-300"
                />
              </div>

              {/* AI Button */}
              <Button
                type="button"
                variant="outline"
                onClick={predictCategory}
                disabled={predicting}
                className="w-full rounded-xl h-12 border-indigo-300 text-indigo-600 hover:bg-indigo-50"
              >
                {predicting ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Analyzing...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4 mr-2" />
                    Auto-classify with AI
                  </>
                )}
              </Button>

              {/* AI Prediction */}
              {mlPrediction && (
                <motion.div
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="bg-indigo-50 border border-indigo-100 rounded-xl p-4"
                >
                  <div className="flex items-center mb-2 gap-2">
                    <CheckCircle2 className="w-4 h-4 text-indigo-600" />

                    <span className="font-semibold text-indigo-700 text-sm">
                      AI Prediction
                    </span>

                    <span className="text-xs text-indigo-500 ml-auto">
                      {Math.round((mlPrediction.confidence || 0.8) * 100)}%
                    </span>
                  </div>

                  <p className="text-xs text-indigo-600">
                    Category:
                    <b className="uppercase ml-1">
                      {mlPrediction.predictedDepartment}
                    </b>
                    <br />
                    Priority:
                    <b className="capitalize ml-1">
                      {mlPrediction.predictedPriority}
                    </b>
                  </p>
                </motion.div>
              )}

              {/* File Upload */}
              <div className="space-y-2">
                <Label className="font-medium text-slate-700">
                  Evidence / Attachments
                </Label>

                <label className="flex flex-col items-center justify-center border-2 border-dashed border-slate-300 rounded-xl p-6 cursor-pointer hover:border-indigo-400">
                  <Upload className="w-8 h-8 text-slate-400 mb-2" />

                  <p className="text-sm text-slate-500">
                    Click to upload files
                  </p>

                  <input
                    type="file"
                    multiple
                    className="hidden"
                    onChange={handleFileUpload}
                  />
                </label>

                {files.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-3">
                    {files.map((file, index) => (
                      <div
                        key={index}
                        className="flex items-center gap-2 bg-slate-100 rounded-lg px-3 py-2 text-sm"
                      >
                        <FileText className="w-3.5 h-3.5 text-slate-500" />

                        <span className="max-w-[180px] truncate text-slate-700">
                          {file.name}
                        </span>

                        <button
                          type="button"
                          onClick={() => removeFile(index)}
                          className="text-slate-500 hover:text-red-500"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Submit */}
              <Button
                type="submit"
                disabled={submitting}
                className="w-full h-14 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 text-white"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                    Submitting...
                  </>
                ) : (
                  <>
                    <Send className="w-5 h-5 mr-2" />
                    Submit Complaint
                  </>
                )}
              </Button>
            </form>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}
