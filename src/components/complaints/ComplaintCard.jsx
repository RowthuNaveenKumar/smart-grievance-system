export default function ComplaintCard({ complaint, onClick }) {
  return (
    <div
      onClick={onClick}
      className="bg-white p-6 rounded-xl shadow hover:shadow-lg cursor-pointer"
    >
      <h3 className="text-lg font-semibold">{complaint.title}</h3>

      <p className="text-gray-500 text-sm mt-2">{complaint.description}</p>

      <div className="mt-4 flex gap-2">
        <span className="text-xs bg-indigo-100 px-2 py-1 rounded">
          {complaint.category}
        </span>

        <span className="text-xs bg-green-100 px-2 py-1 rounded">
          {complaint.status}
        </span>
      </div>
    </div>
  );
}
