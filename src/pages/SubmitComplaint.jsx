import React, { useState } from "react";
import { api } from "@/services/api";
import { useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { ChevronDown } from "lucide-react";
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
  BrainCircuit,
  AlertTriangle,
  Stars,
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

  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("");

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const res = await api.get("/complaint-categories");
        setCategories(res.data);
      } catch (err) {
        console.error("Failed to load categories", err);
      }
    };

    loadCategories();
  }, []);

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

      const match = categories.find(
        (c) =>
          c.name.toLowerCase() === prediction.predictedDepartment.toLowerCase(),
      );

      if (match) {
        setSelectedCategory(match.categoryId);
      }
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
        categoryId: selectedCategory,
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

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />
      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[26rem] w-[26rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

      <div className="relative z-10 mx-auto max-w-4xl px-4 py-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -18 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8 flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-5 py-4 backdrop-blur-xl shadow-2xl"
        >
          <div>
            <h2 className="text-lg font-semibold text-white">
              Submit Complaint
            </h2>
            <p className="text-xs text-slate-300">
              Smart grievance submission with AI routing
            </p>
          </div>

          <button
            onClick={() => navigate("/student-dashboard")}
            className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300 hover:bg-white/10"
          >
            <ArrowLeft className="w-4 h-4" />
            Back
          </button>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 26 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-[2rem] border border-white/10 bg-white/8 backdrop-blur-2xl shadow-xl"
        >
          <Card className="border-0 bg-transparent text-white shadow-none">
            <CardHeader className="border-b border-white/10 p-8 ">
              <CardTitle className="text-3xl font-bold">
                Submit a Complaint
              </CardTitle>
              <p className="text-sm text-slate-300">
                Describe your issue clearly so the right team can help you.
              </p>
            </CardHeader>

            <CardContent className="p-8">
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* Title */}
                <div className="space-y-2">
                  <Label className="text-slate-200/font-bold ">
                    Complaint Title *
                  </Label>

                  <Input
                    placeholder="Example: Fan not working"
                    value={form.title}
                    onChange={(e) =>
                      setForm({ ...form, title: e.target.value })
                    }
                    className="h-12 rounded-xl border-white/10 bg-slate-900/40 text-white"
                  />
                </div>

                {/* Description */}
                <div className="space-y-2">
                  <Label className="text-slate-200/font-bold">
                    Detailed Description *
                  </Label>

                  <Textarea
                    placeholder="Explain your issue..."
                    value={form.description}
                    onChange={(e) =>
                      setForm({ ...form, description: e.target.value })
                    }
                    className="min-h-[140px] rounded-xl border-white/10 bg-slate-900/40 text-white"
                  />
                </div>

                {/* AI Auto Classify */}
                <div className="rounded-2xl border border-indigo-200/20 bg-gradient-to-br from-indigo-300/15 via-blue-500/10 to-cyan-500/10 p-5 shadow-lg shadow-indigo-500/20">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 via-blue-500 to-cyan-500">
                        <Sparkles className="w-5 h-5 text-white animate-pulse" />
                      </div>

                      <div>
                        <p className="text-sm font-semibold text-indigo-100">
                          AI Auto Classification
                        </p>
                        <p className="text-xs text-slate-300">
                          Predict category and priority automatically
                        </p>
                      </div>
                    </div>

                    <span className="text-xs bg-cyan-500/20 text-cyan-300 px-3 py-1 rounded-full">
                      AI Powered
                    </span>
                  </div>

                  <Button
                    type="button"
                    onClick={predictCategory}
                    disabled={predicting}
                    className="h-14 w-full rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 font-semibold text-white"
                  >
                    {predicting ? (
                      <>
                        <Loader2 className="mr-2 w-5 h-5 animate-spin" />
                        Analyzing...
                      </>
                    ) : (
                      <>
                        <Sparkles className="mr-2 w-5 h-5 animate-pulse" />
                        Auto-Classify with AI
                      </>
                    )}
                  </Button>

                  {mlPrediction && (
                    <motion.div
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="mt-4 rounded-xl border border-indigo-400/20 bg-indigo-500/10 p-4"
                    >
                      <div className="flex items-center gap-2 mb-2">
                        <Stars className="w-4 h-4 text-indigo-300" />
                        <span className="text-sm font-semibold text-indigo-200">
                          AI Prediction
                        </span>
                        <span className="ml-auto text-xs text-indigo-300">
                          {Math.round((mlPrediction.confidence || 0.8) * 100)}%
                        </span>
                      </div>

                      <p className="text-sm text-slate-200">
                        Category:
                        <b className="ml-1 uppercase">
                          {mlPrediction.predictedDepartment}
                        </b>
                        <br />
                        Priority:
                        <b
                          className={`ml-1 px-2 py-1 rounded ${getPriorityStyles(
                            mlPrediction.predictedPriority,
                          )}`}
                        >
                          {mlPrediction.predictedPriority}
                        </b>
                      </p>
                    </motion.div>
                  )}
                </div>
                <div className="space-y-2">
                  <Label className="font-medium text-slate-200">
                    Category *
                  </Label>
                <div className ="relative">
                  <select
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value)}
                    className="w-full h-12 rounded-2xl border border-white/10 bg-slate-900/40 backdrop-blur-xl text-white px-4 shadow-lg transition-all duration-300 hover:-translate-y-0.5 hover:shadow-2xl hover:shadow-indigo-500/20 hover:bg-slate-900/50 focus:outline-none focus:ring-2 focus:ring-indigo-500 appearance-none"
                  >
                    <option value="">Select category</option>

                    {categories.map((c) => (
                      <option 
                        key={c.categoryId} 
                        value={c.categoryId}
                        className="bg-slate-900/40 text-white">
                        {c.name}
                      </option>
                    ))}
                  </select>
                  <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" />
                  </div>
                </div>

                {/* Upload */}
                <div className="space-y-2">
                  <Label className="text-slate-200">
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

                {/* Submit */}
                <Button
                  type="submit"
                  disabled={submitting}
                  className="w-full h-14 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 font-semibold text-white shadow-lg hover:scale-[1.02]"
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