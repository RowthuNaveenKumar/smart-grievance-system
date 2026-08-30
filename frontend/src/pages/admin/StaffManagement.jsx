import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Edit, UserX, Users, Plus, ArrowLeft } from "lucide-react";

import {
  getAllStaff,
  disableStaff,
  getStaffById,
} from "@/services/adminStaffApi";

import { Button } from "@/components/ui/button";
import StaffModal from "@/components/admin/StaffModal";

import { toast } from "sonner";

export default function StaffManagement() {
  const navigate = useNavigate();
  const [staff, setStaff] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  const [showModal, setShowModal] = useState(false);

  const [editStaff, setEditStaff] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);

  useEffect(() => {
    loadStaff();
  }, []);

  const loadStaff = async () => {
    try {
      const data = await getAllStaff();
      setStaff(data);
    } catch (error) {
      console.error(error);
      toast.error("Failed to load staff");
    } finally {
      setLoading(false);
    }
  };

  const handleDisableStaff = async (id) => {
    const confirmed = window.confirm(
      "Are you sure you want to disable this staff member?"
    );

    if (!confirmed) return;

    try {
      await disableStaff(id);

      toast.success("Staff disabled successfully");

      loadStaff();
    } catch (error) {
      toast.error(
        error.response?.data?.message ||
          "Failed to disable staff"
      );
    }
  };

  const handleEditStaff = async (id) => {
    try {
      const data = await getStaffById(id);

      setEditStaff(data);

      setShowEditModal(true);
    } catch (error) {
      toast.error("Failed to load staff");
    }
  };

  const filteredStaff = staff.filter(
    (member) =>
      member.name?.toLowerCase().includes(search.toLowerCase()) ||
      member.email?.toLowerCase().includes(search.toLowerCase()) ||
      member.phone?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="min-h-screen bg-slate-950 text-white p-6">

      {/* Header */}

      <div className="mb-8 rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">

          <div>
            <h1 className="text-4xl font-black">
              Staff Management
            </h1>

            <p className="text-slate-400 mt-2">
              Manage staff accounts and assignments
            </p>
          </div>

          <div className="flex items-center gap-4">

            <button
              onClick={() => navigate("/admin-dashboard")}
              className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-300 hover:bg-white/10 transition-all"
            >
              <ArrowLeft className="w-4 h-4" />
              Back
            </button>

            <div className="bg-indigo-500/20 border border-indigo-500/30 px-4 py-2 rounded-xl">
              <span className="text-indigo-300 font-semibold">
                {staff.length} Staff
              </span>
            </div>

            <Button
              onClick={() => {
                setEditStaff(null);
                setShowModal(true);
              }}
              className="bg-gradient-to-r from-indigo-500 to-blue-500"
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Staff
            </Button>

          </div>

        </div>
      </div>

      {/* Search */}

      <div className="mb-6 relative">

        <Search className="absolute left-4 top-3.5 h-5 w-5 text-slate-400" />

        <input
          type="text"
          placeholder="Search by name, email or phone..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="
            w-full
            rounded-2xl
            bg-white/5
            border
            border-white/10
            pl-12
            pr-4
            py-3
            text-white
            outline-none
            focus:border-indigo-500
          "
        />
      </div>

      {/* Table */}

      <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl overflow-hidden">

        {loading ? (
          <div className="p-10 text-center">
            Loading Staff...
          </div>
        ) : filteredStaff.length === 0 ? (
          <div className="p-16 text-center">

            <Users className="h-14 w-14 mx-auto mb-4 text-slate-500" />

            <h3 className="text-xl font-semibold">
              No Staff Found
            </h3>

            <p className="text-slate-400 mt-2">
              Try changing your search keyword
            </p>

          </div>
        ) : (
          <table className="w-full">

            <thead className="bg-white/5 border-b border-white/10">
              <tr className="text-left">

                <th className="p-4">ID</th>
                <th className="p-4">Name</th>
                <th className="p-4">Email</th>
                <th className="p-4">Phone</th>
                <th className="p-4">Department</th>
                <th className="p-4">Division</th>
                <th className="p-4">Floor</th>
                <th className="p-4">Roles</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-center">
                  Actions
                </th>

              </tr>
            </thead>

            <tbody>

              {filteredStaff.map((member) => (
                <tr
                  key={member.staffId}
                  className="
                    border-b
                    border-white/5
                    hover:bg-white/5
                    transition
                  "
                >

                  <td className="p-4">
                    {member.staffId}
                  </td>

                  <td className="p-4 font-medium">
                    {member.name}
                  </td>

                  <td className="p-4 text-slate-300">
                    {member.email}
                  </td>

                  <td className="p-4">
                    {member.phone}
                  </td>

                  <td className="p-4">
                    {member.department || "-"}
                  </td>

                  <td className="p-4">
                    {member.division || "-"}
                  </td>

                  <td className="p-4">
                    {member.floor || "-"}
                  </td>

                  <td className="p-4">
                    {member.roles?.join(", ") || "-"}
                  </td>

                  <td className="p-4">

                    {member.enabled ? (
                      <span className="px-3 py-1 rounded-full text-xs bg-green-500/20 text-green-400">
                        ACTIVE
                      </span>
                    ) : (
                      <span className="px-3 py-1 rounded-full text-xs bg-red-500/20 text-red-400">
                        DISABLED
                      </span>
                    )}

                  </td>

                  <td className="p-4">

                    <div className="flex justify-center gap-2">

                      <button
                        onClick={() =>
                          handleEditStaff(member.staffId)
                        }
                        className="p-2 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30"
                      >
                        <Edit className="h-4 w-4" />
                      </button>

                      <button
                        disabled={!member.enabled}
                        onClick={() =>
                          handleDisableStaff(member.staffId)
                        }
                        className="p-2 rounded-xl bg-red-500/20 hover:bg-red-500/30"
                      >
                        <UserX className="h-4 w-4" />
                      </button>

                    </div>

                  </td>

                </tr>
              ))}

            </tbody>

          </table>
        )}

      </div>

      <StaffModal
        isOpen={showModal || showEditModal}
        onClose={() => {
          setShowModal(false);
          setShowEditModal(false);
          setEditStaff(null);
        }}
        onSuccess={loadStaff}
        editStaff={editStaff}
      />

    </div>
  );
}