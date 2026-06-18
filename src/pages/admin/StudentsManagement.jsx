import { useEffect, useState } from "react";
import { Search, Edit, UserX, Users, Plus } from "lucide-react";
import {
  getAllStudents,
  disableStudent,
  getStudentById,
} from "@/services/adminStudentApi";
import { Button } from "@/components/ui/button";
import StudentModal from "@/components/admin/StudentModal";

import { toast } from "sonner";

export default function StudentsManagement() {
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  const [editStudent, setEditStudent] = useState(null);
  const [showEditModal, setShowEditModal] = useState(false);

  useEffect(() => {
    loadStudents();
  }, []);

  const loadStudents = async () => {
    try {
      const data = await getAllStudents();
      setStudents(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleDisableStudent = async (id) => {
    const confirmed = window.confirm(
      "Are you sure you want to disable this student?",
    );

    if (!confirmed) return;

    try {
      await disableStudent(id);

      toast.success("Student disabled successfully");

      loadStudents();
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to disable student");
    }
  };

  const handleEditStudent = async (id) => {
    try {
      const data = await getStudentById(id);

      setEditStudent(data);

      setShowEditModal(true);
    } catch (error) {
      toast.error("Failed to load student");
    }
  };
  const filteredStudents = students.filter(
    (student) =>
      student.name?.toLowerCase().includes(search.toLowerCase()) ||
      student.email?.toLowerCase().includes(search.toLowerCase()) ||
      student.enrollmentNo?.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <div className="min-h-screen bg-slate-950 text-white p-6">
      {/* Header */}

      <div className="mb-8 rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-4xl font-black">Student Management</h1>

            <p className="text-slate-400 mt-2">
              Manage student accounts and academic details
            </p>
          </div>

          <div className="flex items-center gap-4">
            <div className="bg-indigo-500/20 border border-indigo-500/30 px-4 py-2 rounded-xl">
              <span className="text-indigo-300 font-semibold">
                {students.length} Students
              </span>
            </div>

            <Button
              onClick={() => {
                setEditStudent(null);
                setShowModal(true);
              }}
              className="bg-gradient-to-r from-indigo-500 to-blue-500"
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Student
            </Button>
          </div>
        </div>
      </div>

      {/* Search */}

      <div className="mb-6 relative">
        <Search className="absolute left-4 top-3.5 h-5 w-5 text-slate-400" />

        <input
          type="text"
          placeholder="Search by name, email or enrollment..."
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

      {/* Table Card */}

      <div className="rounded-3xl border border-white/10 bg-white/5 backdrop-blur-xl overflow-hidden">
        {loading ? (
          <div className="p-10 text-center">Loading Students...</div>
        ) : filteredStudents.length === 0 ? (
          <div className="p-16 text-center">
            <Users className="h-14 w-14 mx-auto mb-4 text-slate-500" />

            <h3 className="text-xl font-semibold">No Students Found</h3>

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
                <th className="p-4">Enrollment</th>
                <th className="p-4">Year</th>
                <th className="p-4">Division</th>
                <th className="p-4">Room</th>
                <th className="p-4">Status</th>
                <th className="p-4 text-center">Actions</th>
              </tr>
            </thead>

            <tbody>
              {filteredStudents.map((student) => (
                <tr
                  key={student.studentId}
                  className="
                    border-b
                    border-white/5
                    hover:bg-white/5
                    transition
                  "
                >
                  <td className="p-4">{student.studentId}</td>

                  <td className="p-4 font-medium">{student.name}</td>

                  <td className="p-4 text-slate-300">{student.email}</td>

                  <td className="p-4">{student.enrollmentNo}</td>

                  <td className="p-4">{student.year}</td>

                  <td className="p-4">{student.division}</td>

                  <td className="p-4">{student.room}</td>

                  <td>
                    {student.enabled ? (
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
                        onClick={() => handleEditStudent(student.studentId)}
                        className="p-2 rounded-xl bg-cyan-500/20 hover:bg-cyan-500/30"
                      >
                        <Edit className="h-4 w-4" />
                      </button>

                      <button
                        disabled={!student.enabled}
                        onClick={() => handleDisableStudent(student.studentId)}
                        className="p-2 rounded-xl bg-red-500/20 hover:bg-red-500/30  "
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
      <StudentModal
        isOpen={showModal || showEditModal}
        onClose={() => {
          setShowModal(false);
          setShowEditModal(false);
          setEditStudent(null);
        }}
        onSuccess={loadStudents}
        editStudent={editStudent}
      />
    </div>
  );
}
