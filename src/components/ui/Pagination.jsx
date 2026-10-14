import React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

/**
 * Reusable accessible Pagination component for SGMS tables and lists.
 *
 * @param {number} currentPage - Current active page (1-indexed)
 * @param {number} totalPages - Total number of available pages
 * @param {function} onPageChange - Callback when a page is selected
 * @param {number} totalItems - Total count of matching items
 * @param {number} pageSize - Number of items per page (default 10)
 */
export default function Pagination({
  currentPage = 1,
  totalPages = 1,
  onPageChange,
  totalItems = 0,
  pageSize = 10,
}) {
  if (totalItems === 0) return null;

  const startItem = (currentPage - 1) * pageSize + 1;
  const endItem = Math.min(currentPage * pageSize, totalItems);

  // Generate page numbers with smart ellipsis for larger sets
  const getPageNumbers = () => {
    const pages = [];
    if (totalPages <= 5) {
      for (let i = 1; i <= totalPages; i++) pages.push(i);
    } else {
      pages.push(1);
      if (currentPage > 3) {
        pages.push("...");
      }

      const start = Math.max(2, currentPage - 1);
      const end = Math.min(totalPages - 1, currentPage + 1);

      for (let i = start; i <= end; i++) {
        if (!pages.includes(i)) pages.push(i);
      }

      if (currentPage < totalPages - 2) {
        pages.push("...");
      }
      if (!pages.includes(totalPages)) {
        pages.push(totalPages);
      }
    }
    return pages;
  };

  const pages = getPageNumbers();

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-white/10 text-sm text-slate-300">
      {/* Counter summary */}
      <div className="text-xs sm:text-sm text-slate-400">
        Showing <span className="font-semibold text-white">{startItem}</span>–
        <span className="font-semibold text-white">{endItem}</span> of{" "}
        <span className="font-semibold text-indigo-300">{totalItems}</span> complaints
      </div>

      {/* Pagination controls */}
      <div className="flex items-center gap-1.5">
        {/* Previous */}
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 1}
          aria-label="Previous Page"
          className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-medium text-slate-300 backdrop-blur-xl transition-all hover:bg-white/10 hover:text-white disabled:pointer-events-none disabled:opacity-40 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/50"
        >
          <ChevronLeft className="h-4 w-4" />
          <span className="hidden sm:inline">Previous</span>
        </button>

        {/* Page buttons */}
        <div className="flex items-center gap-1">
          {pages.map((p, idx) => {
            if (p === "...") {
              return (
                <span
                  key={`ellipsis-${idx}`}
                  className="px-2 py-1 text-xs text-slate-500 select-none"
                >
                  ...
                </span>
              );
            }

            const isCurrent = p === currentPage;
            return (
              <button
                key={p}
                type="button"
                onClick={() => onPageChange(p)}
                aria-current={isCurrent ? "page" : undefined}
                className={`min-w-8 h-8 rounded-xl px-2.5 text-xs font-semibold transition-all focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/50 ${
                  isCurrent
                    ? "bg-indigo-600 text-white shadow-lg shadow-indigo-600/30 font-bold border border-indigo-400/30"
                    : "border border-white/10 bg-white/5 text-slate-300 hover:bg-white/10 hover:text-white"
                }`}
              >
                {p}
              </button>
            );
          })}
        </div>

        {/* Next */}
        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= totalPages}
          aria-label="Next Page"
          className="inline-flex items-center gap-1 rounded-xl border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-medium text-slate-300 backdrop-blur-xl transition-all hover:bg-white/10 hover:text-white disabled:pointer-events-none disabled:opacity-40 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/50"
        >
          <span className="hidden sm:inline">Next</span>
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
