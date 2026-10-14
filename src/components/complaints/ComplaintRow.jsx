import React from "react";
import moment from "moment";
import { ChevronRight, Clock, Building2, User, AlertTriangle } from "lucide-react";

/**
 * Status Badge Style Resolver
 */
export const getStatusBadgeStyle = (status) => {
  switch ((status || "").toUpperCase()) {
    case "OPEN":
      return "bg-amber-500/15 text-amber-300 border-amber-500/30";
    case "IN_PROGRESS":
      return "bg-cyan-500/15 text-cyan-300 border-cyan-500/30";
    case "ESCALATED":
      return "bg-orange-500/15 text-orange-300 border-orange-500/30";
    case "RESOLVED":
      return "bg-emerald-500/15 text-emerald-300 border-emerald-500/30";
    case "CLOSED":
      return "bg-slate-500/15 text-slate-300 border-slate-500/30";
    default:
      return "bg-indigo-500/15 text-indigo-300 border-indigo-500/30";
  }
};

/**
 * Priority Badge Style Resolver
 */
export const getPriorityBadgeStyle = (priority) => {
  switch ((priority || "").toUpperCase()) {
    case "CRITICAL":
    case "HIGH":
      return "bg-rose-500/15 text-rose-300 border-rose-500/30";
    case "MEDIUM":
      return "bg-yellow-500/15 text-yellow-300 border-yellow-500/30";
    case "LOW":
      return "bg-cyan-500/15 text-cyan-300 border-cyan-500/30";
    default:
      return "bg-slate-500/15 text-slate-300 border-slate-500/30";
  }
};

/**
 * Reusable compact ComplaintRow component for Student and Staff dashboards.
 * Replaces large, space-consuming cards with a high-density, responsive row.
 *
 * @param {object} complaint - Complaint data object
 * @param {function} onClick - Click callback
 * @param {boolean} isStaff - True if rendering on staff dashboard (shows student name)
 */
export default function ComplaintRow({ complaint, onClick, isStaff = false }) {
  const statusStyle = getStatusBadgeStyle(complaint.status);
  const priorityStyle = getPriorityBadgeStyle(complaint.priority);

  const formattedTime = complaint.createdAt
    ? moment(complaint.createdAt).fromNow()
    : "Recently";

  const isEscalated = complaint.status === "ESCALATED";

  return (
    <div
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick && onClick();
        }
      }}
      className={`group relative flex flex-col md:flex-row md:items-center justify-between gap-3 rounded-2xl border p-4 backdrop-blur-xl transition-all duration-200 cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/50 ${
        isEscalated
          ? "border-orange-500/30 bg-orange-500/5 hover:bg-orange-500/10 hover:border-orange-500/40"
          : "border-white/10 bg-white/5 hover:bg-white/10 hover:border-white/20"
      }`}
    >
      {/* Left section: ID, Title, Contextual Pills */}
      <div className="flex items-start md:items-center gap-3.5 min-w-0 flex-1">
        {/* Monospace Complaint ID Badge */}
        <span className="shrink-0 rounded-lg border border-white/10 bg-slate-900/60 px-2.5 py-1 text-xs font-mono font-semibold text-indigo-300">
          #{complaint.complaintId}
        </span>

        {/* Title & Metadata Pills */}
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-sm sm:text-base font-semibold text-white truncate max-w-md group-hover:text-indigo-200 transition-colors">
              {complaint.title}
            </h3>

            {isEscalated && (
              <span className="inline-flex items-center gap-1 rounded-full border border-orange-500/40 bg-orange-500/20 px-2 py-0.5 text-[11px] font-bold text-orange-300 animate-pulse">
                <AlertTriangle className="h-3 w-3" />
                Escalated
              </span>
            )}
          </div>

          {/* Subtitle pills: Category, Department, Student (staff view) */}
          <div className="flex flex-wrap items-center gap-2 mt-1 text-xs text-slate-400">
            {complaint.category && (
              <span className="rounded-md border border-white/5 bg-white/5 px-2 py-0.5 text-slate-300">
                {complaint.category}
              </span>
            )}

            {complaint.department && (
              <span className="hidden sm:inline-flex items-center gap-1 text-slate-400">
                <Building2 className="h-3 w-3 text-slate-500" />
                <span>{complaint.department}</span>
              </span>
            )}

            {isStaff && complaint.studentName && (
              <span className="inline-flex items-center gap-1 text-indigo-300/90 font-medium">
                <User className="h-3 w-3 text-indigo-400" />
                <span>{complaint.studentName}</span>
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Right section: Priority, Status, Timestamp & Action */}
      <div className="flex items-center justify-between md:justify-end gap-3 shrink-0 pt-2 md:pt-0 border-t md:border-t-0 border-white/5">
        <div className="flex items-center gap-2">
          {/* Priority Badge */}
          <span
            className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider ${priorityStyle}`}
          >
            {complaint.priority || "MEDIUM"}
          </span>

          {/* Status Badge */}
          <span
            className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider ${statusStyle}`}
          >
            {complaint.status}
          </span>
        </div>

        <div className="flex items-center gap-2">
          {/* Relative Timestamp */}
          <span className="flex items-center gap-1 text-xs text-slate-400 min-w-20 text-right">
            <Clock className="h-3 w-3 text-slate-500" />
            <span>{formattedTime}</span>
          </span>

          {/* Chevron Navigation Indicator */}
          <ChevronRight className="h-4 w-4 text-slate-400 group-hover:text-white group-hover:translate-x-0.5 transition-all" />
        </div>
      </div>
    </div>
  );
}
