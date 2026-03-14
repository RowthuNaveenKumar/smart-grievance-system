import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

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

import { Shield, Mail, Lock, AlertCircle } from "lucide-react";
import { motion } from "framer-motion";

export default function Login() {
  const navigate = useNavigate();

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

      const { token, role } = res.data;

      localStorage.setItem("token", token);
      localStorage.setItem("role", role);

      // Role-based navigation
      if (role === "STUDENT") navigate("/student");
      else if (role === "MFT") navigate("/mft");
      else if (role === "HOD") navigate("/hod");
      else if (role === "DEAN") navigate("/dean");
      else if (role === "PRESIDENT") navigate("/president");

    } catch (err) {
      setError("Invalid credentials ❌");
    }

    setLoading(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12 bg-gradient-to-br from-blue-50 to-indigo-50">

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >

        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center h-16 w-16 
                          bg-gradient-to-br from-indigo-600 to-blue-700 
                          rounded-2xl mb-4 shadow-xl">
            <Shield className="h-8 w-8 text-white" />
          </div>

          <h1 className="text-3xl font-bold text-slate-900 mb-2">
            Welcome Back
          </h1>

          <p className="text-slate-600">
            Login to your account
          </p>
        </div>

        {/* Card */}
        <Card className="border-slate-200 shadow-xl rounded-2xl">
          <CardHeader>

            <CardTitle className="text-2xl font-bold text-gray-900">
              Login
            </CardTitle>

            <CardDescription className="text-gray-600 text-sm">
              Enter your credentials to continue
            </CardDescription>

          </CardHeader>

          <CardContent>

            <form onSubmit={submit} className="space-y-4">

              {error && (
                <div className="flex items-center gap-2 bg-red-50 border border-red-200 p-3 rounded">
                  <AlertCircle className="h-5 w-5 text-red-600" />
                  <p className="text-sm text-red-800">{error}</p>
                </div>
              )}

              {/* Email */}
              <div className="space-y-2">

                <Label className="font-medium text-gray-800">
                  Email
                </Label>

                <div className="relative">

                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 
                                   h-5 w-5 text-slate-400" />

                  <Input
                    type="email"
                    placeholder="your.email@university.edu"
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    className="pl-10"
                  />

                </div>
              </div>

              {/* Password */}
              <div className="space-y-2">

                <Label className="font-medium text-gray-800">
                  Password
                </Label>

                <div className="relative">

                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 
                                   h-5 w-5 text-slate-400" />

                  <Input
                    type="password"
                    placeholder="Enter your password"
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className="pl-10"
                  />

                </div>
              </div>

              {/* Submit */}
              <Button
                type="submit"
                disabled={loading}
                className="w-full bg-linear-to-r from-indigo-600 to-blue-600 
                           hover:from-indigo-700 hover:to-blue-700"
              >
                {loading ? "Logging in..." : "Login"}
              </Button>

            </form>

            {/* Register Link */}
            <div className="mt-6 text-center">

              <p className="text-sm text-slate-600">
                Doesn't have an account?{" "}
                <a
                  href="/signin"
                  className="text-indigo-600 hover:text-indigo-700 font-medium"
                >
                  Create an account
                </a>
              </p>

            </div>

          </CardContent>
        </Card>

      </motion.div>
    </div>
  );
}