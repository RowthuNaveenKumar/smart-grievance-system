import React, { useState, useEffect } from "react";
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

  const [user, setUser] = useState(null);
  const [form, setForm] = useState({ title: "", description: "" });
  const [files, setFiles] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  const [mlPrediction, setMlPrediction] = useState(null);
  const [predicting, setPredicting] = useState(false);

  // Load user from localStorage
  useEffect(() => {
    const userData = JSON.parse(localStorage.getItem("user"));
    setUser(userData);
  }, []);

  // -------------------------------
  // FILE UPLOAD
  // -------------------------------
  const handleFileUpload = (e) => {
    const newFiles = Array.from(e.target.files);
    setFiles((prev) => [...prev, ...newFiles]);
  };

  const removeFile = (index) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  // -------------------------------
  // ML PREDICT (/complaints/predict)
  // -------------------------------
  const predictCategory = async () => {
    if (!form.title || !form.description) return;

    try {
      setPredicting(true);

      const response = await api.post("/complaints/predict", {
        title: form.title,
        complaint_text: form.description,
      });

      setMlPrediction(response.data);
    } catch (err) {
      console.error("Prediction failed:", err);
    }

    setPredicting(false);
  };

  // -------------------------------
  // SUBMIT COMPLAINT (/complaints/create)
  // -------------------------------
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title || !form.description) return;

    setSubmitting(true);

    try {
      const token = localStorage.getItem("token");

      const formData = new FormData();
      formData.append(
        "data",
        new Blob(
          [
            JSON.stringify({
              studentId: user?.studentId,
              title: form.title,
              description: form.description,

              // ⭐ FIX: ADD CATEGORY + PRIORITY ⭐
              category: mlPrediction?.predictedDepartment || "GENERAL",
              priority: mlPrediction?.predictedPriority || "LOW",
            }),
          ],
          { type: "application/json" },
        ),
      );
      console.log("Sending:", {
        studentId: user?.studentId,
        title: form.title,
        description: form.description,
        category: mlPrediction?.predictedDepartment || "GENERAL",
        priority: mlPrediction?.predictedPriority || "LOW",
      });

      files.forEach((file) => formData.append("files", file));

      const response = await api.post("/complaints/create", formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      });

      navigate(`/complaint/${response.data.complaintId}`);
    } catch (err) {
      console.error("Submit error:", err);
    }

    setSubmitting(false);
  };

  // -------------------------------
  // UI
  // -------------------------------
  return (
    <div className="max-w-3xl mx-auto">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <Card className="rounded-2xl border-slate-100 shadow-sm overflow-hidden">
          <CardHeader className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white p-8">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-white/20 flex items-center justify-center">
                <FileText className="w-6 h-6" />
              </div>
              <div>
                <CardTitle className="text-xl">Submit a Complaint</CardTitle>
                <p className="text-indigo-200 text-sm mt-1">
                  Describe your grievance and we’ll route it to the right team
                </p>
              </div>
            </div>
          </CardHeader>

          <CardContent className="p-8">
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Title */}
              <div className="space-y-2">
                <Label className="text-sm font-semibold text-slate-700">
                  Complaint Title *
                </Label>
                <Input
                  placeholder="Enter your complaint title..."
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  className="rounded-xl h-12 border-slate-200"
                  required
                />
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label className="text-sm font-semibold text-slate-700">
                  Detailed Description *
                </Label>
                <Textarea
                  placeholder="Explain your issue..."
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                  className="rounded-xl min-h-[140px] border-slate-200"
                  required
                />
              </div>

              {/* AI Predict */}
              <Button
                type="button"
                variant="outline"
                onClick={predictCategory}
                disabled={predicting}
                className="w-full rounded-xl h-12 border-indigo-200 text-indigo-600"
              >
                {predicting ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" /> Analyzing
                    with AI...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4 mr-2" /> Auto-classify with AI
                  </>
                )}
              </Button>

              {/* ML RESULT */}
              {mlPrediction && (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  className="bg-indigo-50 border border-indigo-100 rounded-xl p-4"
                >
                  <div className="flex items-center gap-2 mb-2">
                    <CheckCircle2 className="w-4 h-4 text-indigo-600" />
                    <span className="text-sm font-semibold text-indigo-700">
                      AI Prediction
                    </span>
                    <span className="text-xs text-indigo-500 ml-auto">
                      {Math.round((mlPrediction.confidence || 0.8) * 100)}%
                      confidence
                    </span>
                  </div>

                  <p className="text-xs text-indigo-600">
                    Category:&nbsp;
                    <b>{mlPrediction.predictedDepartment || "General"}</b>
                    <br />
                    Priority:&nbsp;
                    <b>{mlPrediction.predictedPriority}</b>
                  </p>
                </motion.div>
              )}

              {/* FILE UPLOAD */}
              <div className="space-y-2">
                <Label className="text-sm font-semibold text-slate-700">
                  Evidence / Attachments
                </Label>

                <label className="flex flex-col items-center justify-center border-2 border-dashed border-slate-200 rounded-xl p-6 cursor-pointer hover:border-indigo-300">
                  <Upload className="w-8 h-8 text-slate-300 mb-2" />
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
                    {files.map((file, i) => (
                      <div
                        key={i}
                        className="flex items-center gap-2 bg-slate-50 rounded-lg px-3 py-2 text-sm"
                      >
                        <FileText className="w-3.5 h-3.5 text-slate-400" />
                        <span className="text-slate-600 truncate max-w-[200px]">
                          {file.name}
                        </span>
                        <button
                          type="button"
                          onClick={() => removeFile(i)}
                          className="text-slate-400 hover:text-rose-500"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* SUBMIT */}
              <Button
                type="submit"
                disabled={submitting}
                className="w-full h-14 rounded-xl bg-gradient-to-r from-indigo-600 to-purple-600 text-white text-base font-semibold"
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
