export default function StatsCard({ title, value, icon: Icon }) {
  return (
    <div className="bg-white rounded-xl shadow p-6 flex items-center gap-4">
      <Icon className="w-8 h-8 text-indigo-600" />

      <div>
        <p className="text-gray-500 text-sm">{title}</p>

        <p className="text-2xl font-bold">{value}</p>
      </div>
    </div>
  );
}
