import React, { useEffect } from "react";
import { motion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { ShieldCheck } from "lucide-react";

export default function SplashScreen() {
  const navigate = useNavigate();

  useEffect(() => {
    const timer = setTimeout(() => {
      navigate("/home"); 
    }, 4000);

    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-950 text-white">
      {/* Dashboard theme background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.18),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(59,130,246,0.14),transparent_30%),linear-gradient(to_bottom_right,#020617,#0f172a,#111827)]" />

      {/* Glow blobs */}
      <motion.div
        initial={{ opacity: 0, scale: 0.6 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 1.2 }}
        className="absolute -top-24 -left-20 h-96 w-96 rounded-full bg-indigo-500/20 blur-3xl"
      />

      <motion.div
        initial={{ opacity: 0, scale: 0.6 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 1.4 }}
        className="absolute top-1/3 -right-20 h-[28rem] w-[28rem] rounded-full bg-blue-500/20 blur-3xl"
      />

      <motion.div
        initial={{ opacity: 0, scale: 0.5 }}
        animate={{ opacity: 1, scale: 1.1 }}
        transition={{ duration: 1.6 }}
        className="absolute bottom-0 left-1/3 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl"
      />

      {/* Netflix-like light sweep but dashboard colors */}
      <motion.div
        initial={{ x: "-120%", opacity: 0 }}
        animate={{ x: "120%", opacity: [0, 1, 0] }}
        transition={{ duration: 1.6, ease: "easeInOut", delay: 0.5 }}
        className="absolute h-[140%] w-32 rotate-12 bg-gradient-to-r from-transparent via-white/20 to-transparent blur-2xl"
      />

      {/* Main content */}
      <div className="relative z-10 flex flex-col items-center justify-center px-6 text-center">
        {/* Pulse behind icon */}
        <div className="absolute h-40 w-40 rounded-full bg-indigo-500/20 blur-3xl animate-pulse" />

        {/* Icon-based logo */}
        <motion.div
          initial={{ opacity: 0, scale: 0.4, rotate: -20 }}
          animate={{ opacity: 1, scale: 1, rotate: 0 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className="relative mb-7 flex h-24 w-24 items-center justify-center rounded-[2rem] border border-white/10 bg-gradient-to-br from-indigo-500 to-cyan-500 shadow-[0_0_40px_rgba(99,102,241,0.6)]"
        >
          <ShieldCheck className="h-12 w-12 text-white" />
        </motion.div>

        {/* SmartGriev animated text */}
        <div className="flex flex-wrap justify-center">
          {"SmartGriev".split("").map((char, index) => (
            <motion.span
              key={index}
              initial={{ opacity: 0, y: 50, scale: 0.8 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              transition={{
                delay: 0.2 + index * 0.07,
                duration: 0.4,
                ease: "easeOut",
              }}
              className="bg-gradient-to-r from-white via-indigo-200 to-cyan-300 bg-clip-text text-4xl font-black text-transparent sm:text-6xl md:text-7xl"
            >
              {char}
            </motion.span>
          ))}
        </div>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.2, duration: 0.7 }}
          className="mt-4 text-sm uppercase tracking-[0.25em] text-slate-300 sm:text-base"
        >
          Smart Grievance Management System
        </motion.p>

        {/* Animated underline */}
        <motion.div
          initial={{ width: 0, opacity: 0 }}
          animate={{ width: "220px", opacity: 1 }}
          transition={{ delay: 1.4, duration: 0.8 }}
          className="mt-6 h-[3px] rounded-full bg-gradient-to-r from-indigo-500 via-blue-400 to-cyan-400 shadow-[0_0_20px_rgba(59,130,246,0.6)]"
        />

        {/* Loading dots */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1.7 }}
          className="mt-8 flex items-center gap-2"
        >
          <span className="h-3 w-3 animate-bounce rounded-full bg-indigo-400 [animation-delay:0ms]" />
          <span className="h-3 w-3 animate-bounce rounded-full bg-blue-400 [animation-delay:150ms]" />
          <span className="h-3 w-3 animate-bounce rounded-full bg-cyan-400 [animation-delay:300ms]" />
        </motion.div>
      </div>

      {/* End fade */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: [0, 0, 0.15, 0.35] }}
        transition={{ duration: 3.2 }}
        className="pointer-events-none absolute inset-0 bg-slate-950"
      />
    </div>
  );
}