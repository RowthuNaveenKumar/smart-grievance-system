import { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/ui/card";

import {
  GraduationCap,
  Mail,
  Lock,
  AlertCircle,
  ArrowLeft,
  Building2,
} from "lucide-react";
import { motion } from "framer-motion";
import { useUser } from "@/context/UserContext";

export default function Login() {
  const navigate = useNavigate();

  const { loadUser } = useUser();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();

    setError("");
    setLoading(true);

    try {
      const res = await axios.post("http://localhost:8080/auth/login", {
        email,
        password,
      });

      const { token } = res.data;

      localStorage.setItem("token", token);

      await loadUser();

      const user=JSON.parse(localStorage.getItem("user"));

      if (user.accountType === "STUDENT") {
        navigate("/student-dashboard");
      } else if (user.role === "ADMIN") {
        navigate("/admin-dashboard");
      } else {
        navigate("/staff-dashboard");
      }
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || "Login failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />

      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

      <div className="relative z-10 min-h-screen flex items-center justify-center px-4 py-12">
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          className="w-full max-w-md"
        >
          <div className="mb-6">
            <button
              onClick={() => navigate("/")}
              className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-slate-300 backdrop-blur-xl hover:bg-white/10 transition-all"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to Home
            </button>
          </div>

          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center h-16 w-16 bg-gradient-to-br from-indigo-500 via-blue-500 to-cyan-500 rounded-2xl mb-4 shadow-xl shadow-indigo-500/30">
              <GraduationCap className="h-8 w-8 text-white" />
            </div>

            <h1 className="text-3xl font-bold text-white">Welcome Back</h1>

            <p className="text-slate-300 mt-2">Login to your SGMS account</p>
          </div>

          <Card className="border border-white/10 bg-white/8 backdrop-blur-2xl shadow-[0_20px_80px_rgba(0,0,0,0.35)] rounded-[1.75rem] text-white">
            <CardHeader>
              <CardTitle className="text-2xl font-bold text-white">
                Login
              </CardTitle>
              <CardDescription className="text-slate-300">
                Enter your university credentials
              </CardDescription>
            </CardHeader>

            <CardContent>
              <form onSubmit={submit} className="space-y-4">
                {error && (
                  <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-3 flex gap-2">
                    <AlertCircle className="h-5 w-5 text-red-400 mt-0.5" />
                    <p className="text-sm text-red-200">{error}</p>
                  </div>
                )}

                <div className="space-y-2">
                  <Label className="text-slate-200 font-medium">Email</Label>

                  <div className="relative">
                    <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />

                    <Input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="your.email@university.edu"
                      className="pl-10 h-12 rounded-xl border-white/10 bg-slate-900/40 text-white placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-indigo-500"
                      required
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label className="text-slate-200 font-medium">Password</Label>

                  <div className="relative">
                    <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-slate-400" />

                    <Input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Enter password"
                      className="pl-10 h-12 rounded-xl border-white/10 bg-slate-900/40 text-white placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-indigo-500"
                      required
                    />
                  </div>
                </div>

                <Button
                  type="submit"
                  disabled={loading}
                  className="w-full h-12 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 text-white font-semibold shadow-lg shadow-indigo-500/30 hover:scale-[1.01] transition-all"
                >
                  {loading ? "Logging in..." : "Login"}
                </Button>
              </form>

              <div className="mt-6 text-center">
                <p className="text-sm text-slate-300">
                  Don't have an account?{" "}
                  <a
                    href="/signin"
                    className="text-indigo-300 hover:text-white font-medium transition-colors"
                  >
                    Create an account
                  </a>
                </p>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      </div>
    </div>
  );
}
