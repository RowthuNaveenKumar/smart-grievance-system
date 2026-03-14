import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { getComplaintById } from "../../services/complaintService";

export default function ComplaintDetails() {
  const { id } = useParams();
  const [complaint, setComplaint] = useState(null);

  useEffect(() => {
    loadComplaint();
  }, []);

  const loadComplaint = async () => {
    const data = await getComplaintById(id);
    setComplaint(data);
  };

  if (!complaint) return <div className="p-6">Loading...</div>;

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 text-white p-6 rounded-xl">
        <h2 className="text-xl font-semibold">{complaint.title}</h2>
        <p className="text-sm">{complaint.status}</p>
      </div>

      <div className="mt-6 space-y-4">

        <div className="bg-white shadow rounded-xl p-4">
          <p><strong>Description:</strong></p>
          <p>{complaint.description}</p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="bg-gray-50 p-4 rounded-lg">
            <p><strong>Department:</strong> {complaint.category}</p>
          </div>

          <div className="bg-gray-50 p-4 rounded-lg">
            <p><strong>Assigned To:</strong> {complaint.assignedTo?.name || "Unassigned"}</p>
          </div>

          <div className="bg-gray-50 p-4 rounded-lg">
            <p><strong>Priority:</strong> {complaint.priority}</p>
          </div>

          <div className="bg-gray-50 p-4 rounded-lg">
            <p><strong>Current Level:</strong> {complaint.currentLevel}</p>
          </div>
        </div>

        <div className="bg-white shadow rounded-xl p-4">
          <p><strong>Created At:</strong> {complaint.createdAt}</p>
        </div>
      </div>
    </div>
  );
}