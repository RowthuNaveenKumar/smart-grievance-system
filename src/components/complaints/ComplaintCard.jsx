export default function ComplaintCard({ complaint, onClick }) {
  return (
    <div
      onClick={onClick}
      className="cursor-pointer rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl p-6 hover:bg-white/10 transition-all duration-300"
    >
      <div className="flex justify-between items-start mb-4">

        <h3 className="text-lg font-semibold text-white">
          {complaint.title}
        </h3>

        <span className="text-xs px-3 py-1 rounded-full bg-yellow-400/20 text-yellow-300">
          {complaint.priority || "Medium"}
        </span>

      </div>

      <p className="text-sm text-slate-300 mb-4">
        {complaint.description}
      </p>

      <div className="flex items-center justify-between">

        <div className="flex gap-2">

          <span className="text-xs px-3 py-1 rounded-full bg-indigo-500/20 text-indigo-300">
            {complaint.category}
          </span>

          <span className="text-xs px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300">
            {complaint.status}
          </span>

        </div>

        <span className="text-sm text-slate-400 hover:text-slate-200 transition-colors duration-300">
          View Details →
        </span>

      </div>

    </div>
  );
}