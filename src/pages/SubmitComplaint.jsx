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
  ArrowLeft,
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    try {
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
    } catch (err) {
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleFileUpload = (e) => {
    const newFiles = Array.from(e.target.files);
    setFiles((prev) => [...prev, ...newFiles]);
  };

  const removeFile = (index) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white overflow-hidden relative">
      <div className="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950" />
      <div className="absolute top-0 left-0 -z-10 h-72 w-72 rounded-full bg-indigo-500/20 blur-3xl" />
      <div className="absolute bottom-0 right-0 -z-10 h-72 w-72 rounded-full bg-blue-500/20 blur-3xl" />

      <div className="max-w-3xl mx-auto px-4 py-10">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <div className="mb-6">
            <button
              onClick={() => navigate("/student-dashboard")}
              className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-slate-300 backdrop-blur-xl hover:bg-white/10 transition"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to Dashboard
            </button>
          </div>

          <Card className="border border-white/10 bg-white/10 backdrop-blur-xl shadow-xl rounded-3xl overflow-hidden text-white">
            <CardHeader className="bg-gradient-to-r from-indigo-500 to-blue-500 p-8">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-white/20 flex items-center justify-center">
                  <FileText className="w-7 h-7 text-white" />
                </div>

                <div>
                  <CardTitle className="text-2xl font-bold text-white">
                    Submit a Complaint
                  </CardTitle>

                  <p className="text-indigo-100 text-sm mt-1">
                    Describe your issue and we will route it to the right team
                  </p>
                </div>
              </div>
            </CardHeader>

            <CardContent className="p-8 space-y-6">
              <form onSubmit={handleSubmit} className="space-y-6">
                <div className="space-y-2">
                  <Label className="font-medium text-slate-200">
                    Complaint Title *
                  </Label>

                  <Input
                    placeholder="Example: Fan not working"
                    value={form.title}
                    onChange={(e) => setForm({ ...form, title: e.target.value })}
                    className="rounded-xl h-12 border-white/10 bg-white/5 text-white placeholder:text-slate-400"
                  />
                </div>

                <div className="space-y-2">
                  <Label className="font-medium text-slate-200">
                    Detailed Description *
                  </Label>

                  <Textarea
                    placeholder="Explain your issue..."
                    value={form.description}
                    onChange={(e) =>
                      setForm({ ...form, description: e.target.value })
                    }
                    className="rounded-xl min-h-[130px] border-white/10 bg-white/5 text-white placeholder:text-slate-400"
                  />
                </div>

                <Button
                  type="button"
                  variant="outline"
                  onClick={predictCategory}
                  disabled={predicting}
                  className="w-full rounded-xl h-12 border-white/10 bg-white/5 text-indigo-200 hover:bg-white/10 hover:text-white"
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

                {mlPrediction && (
                  <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    className="bg-indigo-500/10 border border-indigo-400/20 rounded-2xl p-4"
                  >
                    <div className="flex items-center mb-2 gap-2">
                      <CheckCircle2 className="w-4 h-4 text-indigo-300" />

                      <span className="font-semibold text-indigo-200 text-sm">
                        AI Prediction
                      </span>

                      <span className="text-xs text-indigo-300 ml-auto">
                        {Math.round((mlPrediction.confidence || 0.8) * 100)}%
                      </span>
                    </div>

                    <p className="text-sm text-slate-200">
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

                <div className="space-y-2">
                  <Label className="font-medium text-slate-200">
                    Evidence / Attachments
                  </Label>

                  <label className="flex flex-col items-center justify-center border-2 border-dashed border-white/10 rounded-2xl p-6 cursor-pointer bg-white/5 hover:bg-white/10 transition">
                    <Upload className="w-8 h-8 text-slate-400 mb-2" />

                    <p className="text-sm text-slate-300">
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
                          className="flex items-center gap-2 bg-white/10 border border-white/10 rounded-xl px-3 py-2 text-sm"
                        >
                          <FileText className="w-3.5 h-3.5 text-slate-300" />

                          <span className="max-w-[180px] truncate text-slate-200">
                            {file.name}
                          </span>

                          <button
                            type="button"
                            onClick={() => removeFile(index)}
                            className="text-slate-400 hover:text-red-400"
                          >
                            <X className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <Button
                  type="submit"
                  disabled={submitting}
                  className="w-full h-14 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 text-white font-semibold shadow-lg hover:scale-[1.02] transition"
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
    </div>
  );
}