import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { predictComplaint, createComplaint } from "../../services/complaintService";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { Upload, Send, Loader2, Sparkles, FileText } from "lucide-react";
import { motion } from "framer-motion";

export default function SubmitComplaint() {
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [files, setFiles] = useState([]);
  const [prediction, setPrediction] = useState(null);
  const [loading, setLoading] = useState(false);
  const [predicting, setPredicting] = useState(false);

  const studentId = 1;
  const token = localStorage.getItem("token");

  const handlePredict = async () => {
    if (!title && !description) return;
    setPredicting(true);
    const result = await predictComplaint(title, description);
    setPrediction(result);
    setPredicting(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    const complaintData = {
      studentId,
      title,
      description
    };

    const response = await createComplaint(
      complaintData,
      files,
      token
    );

    setLoading(false);
    navigate(`/complaints/${response.complaintId}`);
  };

  return (
    <div className="max-w-3xl mx-auto">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <Card className="rounded-2xl shadow-md overflow-hidden">

          {/* Header */}
          <CardHeader className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white p-8">
            <div className="flex items-center gap-3">
              <FileText className="w-6 h-6" />
              <div>
                <CardTitle>Submit a Complaint</CardTitle>
                <p className="text-indigo-200 text-sm">
                  Describe your grievance and we'll route it to the right team
                </p>
              </div>
            </div>
          </CardHeader>

          <CardContent className="p-8">
            <form onSubmit={handleSubmit} className="space-y-6">

              {/* Title */}
              <div className="space-y-2">
                <Label>Complaint Title *</Label>
                <Input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                />
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label>Detailed Description *</Label>
                <Textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  required
                />
              </div>

              {/* AI Predict */}
              <Button
                type="button"
                variant="outline"
                onClick={handlePredict}
                disabled={predicting}
                className="w-full"
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

              {/* Prediction Result */}
              {prediction && (
                <div className="bg-indigo-50 border rounded-xl p-4">
                  <p><strong>Predicted Department:</strong> {prediction.predictedDepartment}</p>
                  <p><strong>Predicted Priority:</strong> {prediction.predictedPriority}</p>
                </div>
              )}

              {/* File Upload */}
              <div className="space-y-2">
                <Label>Evidence / Attachments</Label>
                <label className="flex flex-col items-center justify-center border-2 border-dashed rounded-xl p-6 cursor-pointer">
                  <Upload className="w-6 h-6 mb-2 text-gray-400" />
                  <span className="text-sm text-gray-500">
                    Click to upload files
                  </span>
                  <input
                    type="file"
                    multiple
                    hidden
                    onChange={(e) => setFiles([...e.target.files])}
                  />
                </label>
              </div>

              {/* Submit */}
              <Button
                type="submit"
                disabled={loading}
                className="w-full bg-gradient-to-r from-indigo-600 to-purple-600 text-white"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Submitting...
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4 mr-2" />
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