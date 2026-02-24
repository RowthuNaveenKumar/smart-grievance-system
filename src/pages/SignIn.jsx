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

import { Shield, Mail, Lock, AlertCircle, CheckCircle } from "lucide-react";
import { motion } from "framer-motion";

export default function SignIn() {
  const navigate = useNavigate();

  // backend logic states
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  // UI States
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  // Combined handler
  const handleSignIn = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    // Frontend validations
    if (password !== confirmPassword) {
      setError("Passwords do not match");
      setLoading(false);
      return;
    }

    if (password.length < 6) {
      setError("Password must be at least 6 characters");
      setLoading(false);
      return;
    }

    try {
      // Your real API call
      await axios.post("http://localhost:8080/auth/signin", {
        email,
        password,
      });

      setSuccess(true);

      setTimeout(() => {
        navigate("/login");
      }, 2000);
    } catch (err) {
      setError("Sign-in failed ❌");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12 
                    bg-linear-to-br from-blue-50 to-indigo-50">
      <motion.div
        initial={{ opacity: 0, y: 15 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >
        {/* Top Icon + Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center h-16 w-16 
                          bg-linear-to-br from-indigo-600 to-blue-700 
                          rounded-2xl mb-4 shadow-xl">
            <Shield className="h-8 w-8 text-white" />
          </div>

          <h1 className="text-3xl font-bold text-slate-900">
            First Time Sign In
          </h1>
          <p className="text-slate-600">
            Set your password to get started
          </p>
        </div>

        {/* Main Card */}
        <Card className="border-slate-200 shadow-xl rounded-2xl">
          <CardHeader>
            <CardTitle>Create Password</CardTitle>
            <CardDescription>Your email must be registered by admin</CardDescription>
          </CardHeader>

          <CardContent>
            {success ? (
              <motion.div
                initial={{ scale: 0.9, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                className="text-center py-8"
              >
                <div className="h-16 w-16 bg-green-100 rounded-full 
                                flex items-center justify-center mx-auto mb-4">
                  <CheckCircle className="h-8 w-8 text-green-600" />
                </div>

                <h3 className="text-xl font-semibold text-slate-900 mb-2">
                  Password Set Successfully!
                </h3>
                <p className="text-slate-600">Redirecting to login...</p>
              </motion.div>
            ) : (
              <form onSubmit={handleSignIn} className="space-y-4">

                {/* Error Box */}
                {error && (
                  <div className="bg-red-50 border border-red-200 rounded-lg p-3 
                                  flex items-start gap-2">
                    <AlertCircle className="h-5 w-5 text-red-600 mt-0.5" />
                    <p className="text-sm text-red-800">{error}</p>
                  </div>
                )}

                {/* Email */}
                <div className="space-y-2">
                  <Label htmlFor="email">University Email</Label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 
                                     h-5 w-5 text-slate-400" />

                    <Input
                      id="email"
                      type="email"
                      placeholder="your.email@university.edu"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      className="pl-10"
                      required
                    />
                  </div>
                  <p className="text-xs text-slate-500">
                    Use your registered university email
                  </p>
                </div>

                {/* Password */}
                <div className="space-y-2">
                  <Label htmlFor="password">Create Password</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 
                                     h-5 w-5 text-slate-400" />
                    <Input
                      id="password"
                      type="password"
                      placeholder="Minimum 6 characters"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="pl-10"
                      required
                    />
                  </div>
                </div>

                {/* Confirm Password */}
                <div className="space-y-2">
                  <Label htmlFor="confirmPassword">Confirm Password</Label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 
                                     h-5 w-5 text-slate-400" />
                    <Input
                      id="confirmPassword"
                      type="password"
                      placeholder="Re-enter password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      className="pl-10"
                      required
                    />
                  </div>
                </div>

                {/* Submit */}
                <Button
                  type="submit"
                  className="w-full bg-linear-to-r from-indigo-600 to-blue-600 
                             hover:from-indigo-700 hover:to-blue-700"
                  disabled={loading}
                >
                  {loading ? "Setting Password..." : "Set Password & Continue"}
                </Button>
              </form>
            )}

            {!success && (
              <div className="mt-6 text-center">
                <p className="text-sm text-slate-600">
                  Already have a password?{" "}
                  <a
                    href="/login"
                    className="text-indigo-600 hover:text-indigo-700 font-medium"
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
  );
}