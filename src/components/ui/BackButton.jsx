import React from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";

/**
 * Standardized BackButton component for SGMS secondary pages.
 *
 * @param {string} [fallback="/"] - Destination route when browser history is unavailable
 * @param {string} [label="Back"] - Accessible text label
 * @param {string} [className="mb-6"] - Container styling/spacing (defaults to top-left with mb-6)
 * @param {function} [onClick] - Custom click handler when explicit navigation semantics are required
 */
export default function BackButton({
  fallback = "/",
  label = "Back",
  className = "mb-6",
  onClick,
}) {
  const navigate = useNavigate();

  const handleBack = (e) => {
    if (onClick) {
      onClick(e);
      return;
    }

    // If there is prior in-app history in this session, use browser history
    if (window.history.state && window.history.state.idx > 0) {
      navigate(-1);
    } else {
      navigate(fallback);
    }
  };

  return (
    <div className={className}>
      <button
        type="button"
        onClick={handleBack}
        aria-label={label}
        className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-slate-300 backdrop-blur-xl transition-all hover:bg-white/10 hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/50"
      >
        <ArrowLeft className="h-4 w-4" />
        <span>{label}</span>
      </button>
    </div>
  );
}
