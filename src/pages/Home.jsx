import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Sparkles, ShieldCheck, TimerReset, Eye, GraduationCap } from "lucide-react";

export default function Home() {
  const navigate = useNavigate();

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 text-white">
      {/* Background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />

      <div className="absolute -top-28 -left-24 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />
      <div className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl animate-pulse" />

      <div className="relative z-10">
        {/* NAVBAR */}
        <motion.div
          initial={{ opacity: 0, y: -18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="max-w-7xl mx-auto px-6 lg:px-8 pt-6"
        >
          <div className="flex justify-between items-center rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl px-6 py-4 shadow-2xl">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-blue-500 shadow-lg shadow-indigo-500/30">
                <GraduationCap className="h-5 w-5 text-white" />
              </div>

              <div>
                <h1 className="text-2xl font-bold tracking-wide text-white">
                  Smart<span className="text-indigo-300">Griev</span>
                </h1>
                <p className="text-xs text-slate-400">
                  Smart grievance management platform
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => navigate("/login")}
                className="px-5 py-2.5 rounded-xl border border-white/10 bg-white/5 text-slate-200 font-medium hover:bg-white/10 transition-all"
              >
                Login
              </button>

              <button
                onClick={() => navigate("/signin")}
                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 text-white font-semibold shadow-lg shadow-indigo-500/30 hover:scale-[1.02] transition-all"
              >
                Register
              </button>
            </div>
          </div>
        </motion.div>

        {/* HERO SECTION */}
        <div className="max-w-7xl mx-auto px-6 lg:px-8 pt-16 pb-10">
          <div className="grid lg:grid-cols-2 items-center gap-14">
            <motion.div
              initial={{ opacity: 0, y: 28 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6 }}
            >

              <h1 className="text-5xl md:text-6xl xl:text-7xl font-black leading-tight mb-6">
                A Smarter Way to{" "}
                <span className="bg-gradient-to-r from-white via-indigo-200 to-blue-300 bg-clip-text text-transparent">
                  Manage Grievances
                </span>
              </h1>

              <p className="text-slate-300 text-lg leading-8 mb-10 max-w-2xl">
                SmartGriev helps universities streamline grievance submission,
                tracking, and resolution through a transparent, efficient, and
                premium digital workflow.
              </p>

              <div className="flex flex-col sm:flex-row gap-4">
                <button
                  onClick={() => navigate("/login")}
                  className="px-8 py-3.5 rounded-xl bg-gradient-to-r from-indigo-500 via-blue-500 to-cyan-500 text-white font-semibold shadow-lg shadow-indigo-500/30 hover:scale-[1.02] transition-all flex items-center justify-center"
                >
                  Get Started
                  <ArrowRight className="ml-2 h-4 w-4" />
                </button>

                <button
                  onClick={() => navigate("/signin")}
                  className="px-8 py-3.5 rounded-xl border border-white/10 bg-white/5 text-white font-medium hover:bg-white/10 transition-all"
                >
                  Create Account
                </button>
              </div>
            </motion.div>

            {/* IMAGE */}
            <motion.div
              initial={{ opacity: 0, y: 28 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7 }}
              className="relative"
            >
              <div className="absolute -inset-4 rounded-[2rem] bg-gradient-to-r from-indigo-500/20 to-cyan-500/20 blur-2xl" />
              <div className="relative rounded-[2rem] border border-white/10 bg-white/5 p-3 backdrop-blur-2xl shadow-[0_20px_80px_rgba(0,0,0,0.35)]">
                <img
                  src="https://images.unsplash.com/photo-1523240795612-9a054b0db644"
                  alt="students collaboration"
                  className="rounded-[1.4rem] shadow-2xl w-full h-[430px] object-cover"
                />
              </div>
            </motion.div>
          </div>
        </div>

        {/* FEATURES */}
        <div className="max-w-7xl mx-auto px-6 lg:px-8 pb-16 pt-6">
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="mb-10"
          >
            <p className="text-sm font-medium text-indigo-300 mb-2">
              Core Features
            </p>
            <h2 className="text-3xl md:text-4xl font-bold text-white">
              Built for clarity, speed, and trust
            </h2>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8">
            <motion.div
              whileHover={{ y: -6 }}
              className="rounded-[1.75rem] border border-white/10 bg-white/8 backdrop-blur-xl p-7 shadow-xl"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-indigo-500 to-blue-500 mb-5 shadow-lg shadow-indigo-500/20">
                <ShieldCheck className="h-6 w-6 text-white" />
              </div>

              <h3 className="text-xl font-semibold mb-3 text-white">
                Easy Complaint Submission
              </h3>

              <p className="text-slate-300 leading-7">
                Students can submit grievances quickly using structured forms
                with a clean and guided user experience.
              </p>
            </motion.div>

            <motion.div
              whileHover={{ y: -6 }}
              className="rounded-[1.75rem] border border-white/10 bg-white/8 backdrop-blur-xl p-7 shadow-xl"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 mb-5 shadow-lg shadow-cyan-500/20">
                <TimerReset className="h-6 w-6 text-white" />
              </div>

              <h3 className="text-xl font-semibold mb-3 text-white">
                Real-Time Tracking
              </h3>

              <p className="text-slate-300 leading-7">
                Track complaint status in real time, from submission to review,
                action, and final resolution.
              </p>
            </motion.div>

            <motion.div
              whileHover={{ y: -6 }}
              className="rounded-[1.75rem] border border-white/10 bg-white/8 backdrop-blur-xl p-7 shadow-xl"
            >
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-cyan-500 mb-5 shadow-lg shadow-emerald-500/20">
                <Eye className="h-6 w-6 text-white" />
              </div>

              <h3 className="text-xl font-semibold mb-3 text-white">
                Transparent Resolution
              </h3>

              <p className="text-slate-300 leading-7">
                Authorities manage grievances efficiently with accountability,
                visibility, and a transparent digital workflow.
              </p>
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
}