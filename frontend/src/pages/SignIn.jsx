import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";

import {
  GraduationCap,
  Mail,
  Lock,
  AlertCircle,
  CheckCircle,
  ArrowLeft,
  Sparkles,
} from "lucide-react";
import { motion } from "framer-motion";

export default function SignIn() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSignIn = async (e) => {
    e.preventDefault();

    setError("");

    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    if (password.length < 6) {
      setError("Password must be at least 6 characters");
      return;
    }

    setLoading(true);

    try {
      localStorage.clear();

      await axios.post("http://localhost:8080/auth/signin", {
        email,
        password,
      });

      setSuccess(true);

      setTimeout(() => {
        navigate("/login");
      }, 2000);
    } catch (err) {
      console.log(err.response?.data);
      setError(err.response?.data?.message || "Sign-in failed ❌");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-white overflow-hidden relative">
      <div className="absolute inset-0 -z-10 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950" />
      <div className="absolute top-0 left-0 -z-10 h-72 w-72 rounded-full bg-indigo-500/20 blur-3xl" />
      <div className="absolute bottom-0 right-0 -z-10 h-72 w-72 rounded-full bg-blue-500/20 blur-3xl" />

      <div className="min-h-screen flex items-center justify-center px-4 py-12">
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          className="w-full max-w-md"
        >
          <div className="mb-6">
            <button
              onClick={() => navigate("/")}
              className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-slate-300 backdrop-blur-xl hover:bg-white/10 transition"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to Home
            </button>
          </div>

          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center h-16 w-16 bg-gradient-to-br from-indigo-500 to-blue-500 rounded-2xl mb-4 shadow-xl">
              <GraduationCap className="h-8 w-8 text-white" />
            </div>

            <h1 className="text-3xl font-bold text-white">
              SignIn to SGMS
            </h1>
            <p className="text-slate-300 mt-2">
              Enter your details to set up your SGMS account
            </p>
          </div>

          <Card className="border border-white/10 bg-white/10 backdrop-blur-xl shadow-xl rounded-3xl text-white">
            <CardHeader>
              <CardTitle className="text-2xl font-bold text-white">
                Create an Account
              </CardTitle>
              <CardDescription className="text-slate-300">
                Your email must be registered by admin
              </CardDescription>
            </CardHeader>

            <CardContent>
              {success ? (
                <motion.div
                  initial={{ scale: 0.9, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  className="text-center py-8"
                >
                  <div className="h-16 w-16 bg-green-500/15 rounded-full flex items-center justify-center mx-auto mb-4 border border-green-500/20">
                    <CheckCircle className="h-8 w-8 text-green-400" />
                  </div>

                  <h3 className="text-xl font-semibold text-white mb-2">
                    Password Set Successfully!
                  </h3>
                  <p className="text-slate-300">Redirecting to login...</p>
                </motion.div>
              ) : (
                <form onSubmit={handleSignIn} className="space-y-4">
                  {error && (
                    <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-3 flex items-start gap-2">
                      <AlertCircle className="h-5 w-5 text-red-400 mt-0.5" />
                      <p className="text-sm text-red-200">{error}</p>
                    </div>
                  )}

                  <div className="space-y-2">
                    <Label htmlFor="email" className="text-slate-200 font-medium">
                      University Email
                    </Label>
                    <div className="relative">
                      <Mail
                        className="absolute left-3 top-1/2 -translate-y-1/2 
                                   h-5 w-5 text-slate-400"
                      />

                      <Input
                        id="email"
                        type="email"
                        placeholder="your.email@university.edu"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="pl-10 h-12 rounded-xl border-white/10 bg-white/5 text-white placeholder:text-slate-400"
                        required
                      />
                    </div>
                    <p className="text-xs text-slate-400">
                      Use your registered university email
                    </p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="password" className="text-slate-200 font-medium">
                      Create Password
                    </Label>
                    <div className="relative">
                      <Lock
                        className="absolute left-3 top-1/2 -translate-y-1/2 
                                   h-5 w-5 text-slate-400"
                      />
                      <Input
                        id="password"
                        type="password"
                        placeholder="Minimum 6 characters"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="pl-10 h-12 rounded-xl border-white/10 bg-white/5 text-white placeholder:text-slate-400"
                        required
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <Label
                      htmlFor="confirmPassword"
                      className="text-slate-200 font-medium"
                    >
                      Confirm Password
                    </Label>
                    <div className="relative">
                      <Lock
                        className="absolute left-3 top-1/2 -translate-y-1/2 
                                   h-5 w-5 text-slate-400"
                      />
                      <Input
                        id="confirmPassword"
                        type="password"
                        placeholder="Re-enter password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        className="pl-10 h-12 rounded-xl border-white/10 bg-white/5 text-white placeholder:text-slate-400"
                        required
                      />
                    </div>
                  </div>

                  <Button
                    type="submit"
                    className="w-full h-12 rounded-xl bg-gradient-to-r from-indigo-500 to-blue-500 text-white font-semibold shadow-lg hover:scale-[1.02] transition"
                    disabled={loading}
                  >
                    {loading ? "Setting Password..." : "Set Password & Continue"}
                  </Button>
                </form>
              )}

              {!success && (
                <div className="mt-6 text-center">
                  <p className="text-sm text-slate-300">
                    Already have an account?{" "}
                    <a
                      href="/login"
                      className="text-indigo-300 hover:text-white font-medium transition"
                    >
                      Login here
                    </a>
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}