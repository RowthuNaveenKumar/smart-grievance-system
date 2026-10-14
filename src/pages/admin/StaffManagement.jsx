import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Search,
  Edit,
  UserX,
  UserCheck,
  Users,
  Plus,
  Award,
} from "lucide-react";
import BackButton from "@/components/ui/BackButton";

import {
  getAllStaff,
  disableStaff,
  enableStaff,
  getStaffById,
} from "@/services/adminStaffApi";

import { Button } from "@/components/ui/button";
import StaffModal from "@/components/admin/StaffModal";

import { toast } from "sonner";

export default function StaffManagement() {
  const navigate = useNavigate();
  const [staff, setStaff] = useState([]);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL"); // ALL | ACTIVE | INACTIVE
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
      setStaff(data || []);
    } catch (error) {
      console.error(error);
      toast.error("Failed to load staff list");
    } finally {
      setLoading(false);
    }
  };

  const handleDisableStaff = async (member) => {
    const confirmed = window.confirm(
      `Are you sure you want to deactivate staff account for "${member.name}"?\n\nThey will be immediately blocked from logging in, and any active sessions will be terminated.`
    );

    if (!confirmed) return;

    try {
      await disableStaff(member.staffId);
      toast.success(`Staff account for "${member.name}" has been deactivated`);
      await loadStaff();
    } catch (error) {
      toast.error(
        error.response?.data?.message || "Failed to deactivate staff account"
      );
    }
  };

  const handleEnableStaff = async (member) => {
    const confirmed = window.confirm(
      `Are you sure you want to activate staff account for "${member.name}"?\n\nThey will regain normal platform login and authorization.`
    );

    if (!confirmed) return;

    try {
      await enableStaff(member.staffId);
      toast.success(`Staff account for "${member.name}" has been activated`);
      await loadStaff();
    } catch (error) {
      toast.error(
        error.response?.data?.message || "Failed to activate staff account"
      );
    }
  };

  const handleEditStaff = async (id) => {
    try {
      const data = await getStaffById(id);
      setEditStaff(data);
      setShowEditModal(true);
    } catch (error) {
      toast.error("Failed to load staff member details");
    }
  };

  const activeCount = staff.filter((m) => m.enabled).length;
  const inactiveCount = staff.filter((m) => !m.enabled).length;

  const filteredStaff = staff.filter((member) => {
    const matchesSearch =
      member.name?.toLowerCase().includes(search.toLowerCase()) ||
      member.email?.toLowerCase().includes(search.toLowerCase()) ||
      member.phone?.toLowerCase().includes(search.toLowerCase()) ||
      member.department?.toLowerCase().includes(search.toLowerCase()) ||
      member.roles?.some((r) => r.toLowerCase().includes(search.toLowerCase()));

    if (!matchesSearch) return false;

    if (statusFilter === "ACTIVE") return member.enabled;
    if (statusFilter === "INACTIVE") return !member.enabled;
    return true;
  });

  return (
    <div className="min-h-screen bg-slate-950 text-white p-6">
      <BackButton fallback="/admin-dashboard" />

      {/* Header */}
      <div className="mb-8 rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-4xl font-black">Staff Management</h1>
            <p className="text-slate-400 mt-2">
              Manage university staff accounts, designations, and organizational scopes
            </p>
          </div>

          <div className="flex items-center gap-4 flex-wrap">
            <div className="bg-indigo-500/20 border border-indigo-500/30 px-4 py-2 rounded-xl">
              <span className="text-indigo-300 font-semibold">
                {staff.length} Total Staff
              </span>
            </div>

            <Button
              variant="outline"
              onClick={() => navigate("/admin/designations")}
              className="border-white/15 bg-white/5 hover:bg-white/10 text-white"
            >
              <Award className="h-4 w-4 mr-2 text-indigo-400" />
              Manage Designations
            </Button>

            <Button
              onClick={() => {
                setEditStaff(null);
                setShowModal(true);
              }}
              className="bg-gradient-to-r from-indigo-500 to-blue-500 shadow-lg shadow-indigo-500/25"
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Staff
            </Button>
          </div>
        </div>
      </div>

      {/* Search & Status Filters */}
      <div className="mb-6 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-4 top-3.5 h-5 w-5 text-slate-400" />
          <input
            type="text"
            placeholder="Search by name, email, phone, department, or designation..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-2xl bg-white/5 border border-white/10 pl-12 pr-4 py-3 text-white outline-none focus:border-indigo-500"
          />
        </div>

        {/* Status Filter Tabs */}
        <div className="flex items-center gap-2 bg-white/5 p-1.5 rounded-2xl border border-white/10">
          <button
            type="button"
            onClick={() => setStatusFilter("ALL")}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
              statusFilter === "ALL"
                ? "bg-indigo-600 text-white shadow-md shadow-indigo-500/30"
                : "text-slate-400 hover:text-white hover:bg-white/5"
            }`}
          >
            All ({staff.length})
          </button>
          <button
            type="button"
            onClick={() => setStatusFilter("ACTIVE")}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
              statusFilter === "ACTIVE"
                ? "bg-emerald-600 text-white shadow-md shadow-emerald-500/30"
                : "text-slate-400 hover:text-white hover:bg-white/5"
            }`}
          >
            Active ({activeCount})
          </button>
          <button
            type="button"
            onClick={() => setStatusFilter("INACTIVE")}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
              statusFilter === "INACTIVE"
                ? "bg-rose-600 text-white shadow-md shadow-rose-500/30"
                : "text-slate-400 hover:text-white hover:bg-white/5"
            }`}
          >
            Inactive ({inactiveCount})
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl overflow-hidden shadow-xl">
        {loading ? (
          <div className="p-12 text-center text-slate-400">
            <div className="inline-block animate-spin h-8 w-8 border-4 border-indigo-500 border-t-transparent rounded-full mb-3" />
            <p>Loading Staff Directory...</p>
          </div>
        ) : filteredStaff.length === 0 ? (
          <div className="p-16 text-center">
            <Users className="h-14 w-14 mx-auto mb-4 text-slate-500" />
            <h3 className="text-xl font-semibold">No Staff Found</h3>
            <p className="text-slate-400 mt-2">
              {search || statusFilter !== "ALL"
                ? "Try clearing filters or search terms"
                : "Get started by adding staff members"}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-white/5 border-b border-white/10">
                <tr className="text-left text-xs uppercase tracking-wider text-slate-400">
                  <th className="p-4">ID</th>
                  <th className="p-4">Name & Email</th>
                  <th className="p-4">Phone</th>
                  <th className="p-4">Department</th>
                  <th className="p-4">Division</th>
                  <th className="p-4">Hostel Floor</th>
                  <th className="p-4">Designations</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 text-center">Actions</th>
                </tr>
              </thead>

              <tbody className="divide-y divide-white/5">
                {filteredStaff.map((member) => (
                  <tr
                    key={member.staffId}
                    className="hover:bg-white/5 transition"
                  >
                    <td className="p-4 text-slate-400 font-mono text-sm">
                      #{member.staffId}
                    </td>

                    <td className="p-4">
                      <div className="font-medium text-white">{member.name}</div>
                      <div className="text-xs text-slate-400">{member.email}</div>
                    </td>

                    <td className="p-4 text-sm text-slate-300">
                      {member.phone || "—"}
                    </td>

                    <td className="p-4 text-sm">
                      {member.department ? (
                        <span className="text-slate-200">{member.department}</span>
                      ) : (
                        <span className="text-slate-500 italic">Unassigned</span>
                      )}
                    </td>

                    <td className="p-4 text-sm">
                      {member.division ? (
                        <span className="text-slate-200">{member.division}</span>
                      ) : (
                        <span className="text-slate-500 italic">Unassigned</span>
                      )}
                    </td>

                    <td className="p-4 text-sm">
                      {member.floor ? (
                        <span className="text-slate-200">{member.floor}</span>
                      ) : (
                        <span className="text-slate-500 italic">Unassigned</span>
                      )}
                    </td>

                    <td className="p-4">
                      {member.roles && member.roles.length > 0 ? (
                        <div className="flex flex-wrap gap-1">
                          {member.roles.map((r, idx) => (
                            <span
                              key={idx}
                              className="px-2 py-0.5 rounded-md text-xs bg-indigo-500/15 text-indigo-300 border border-indigo-500/25"
                            >
                              {r}
                            </span>
                          ))}
                        </div>
                      ) : (
                        <span className="text-slate-500 italic text-sm">None</span>
                      )}
                    </td>

                    <td className="p-4">
                      {member.enabled ? (
                        <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                          ACTIVE
                        </span>
                      ) : (
                        <span className="px-3 py-1 rounded-full text-xs font-semibold bg-rose-500/20 text-rose-400 border border-rose-500/30">
                          INACTIVE
                        </span>
                      )}
                    </td>

                    <td className="p-4">
                      <div className="flex justify-center items-center gap-2">
                        <button
                          onClick={() => handleEditStaff(member.staffId)}
                          title="Edit Staff Member"
                          className="p-2 rounded-xl bg-cyan-500/15 text-cyan-400 hover:bg-cyan-500/25 border border-cyan-500/20 transition"
                        >
                          <Edit className="h-4 w-4" />
                        </button>

                        {member.enabled ? (
                          <button
                            onClick={() => handleDisableStaff(member)}
                            title="Deactivate Staff Account"
                            className="p-2 rounded-xl bg-rose-500/15 text-rose-400 hover:bg-rose-500/25 border border-rose-500/20 transition"
                          >
                            <UserX className="h-4 w-4" />
                          </button>
                        ) : (
                          <button
                            onClick={() => handleEnableStaff(member)}
                            title="Activate Staff Account"
                            className="p-2 rounded-xl bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25 border border-emerald-500/20 transition"
                          >
                            <UserCheck className="h-4 w-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
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