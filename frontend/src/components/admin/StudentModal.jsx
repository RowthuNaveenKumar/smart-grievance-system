import { useEffect, useState } from "react";
import {
  createStudent,
  updateStudent,
  getDivisions,
  getRooms,
} from "@/services/adminStudentApi";

import { toast } from "sonner";

const YEARS = [
  "FIRST_YEAR",
  "SECOND_YEAR",
  "THIRD_YEAR",
  "FOURTH_YEAR",
];

export default function StudentModal({
  isOpen,
  onClose,
  onSuccess,
  editStudent = null,
}) {
  const [divisions, setDivisions] = useState([]);
  const [rooms, setRooms] = useState([]);

  const isEdit = !!editStudent;

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    enrollmentNo: "",
    year: "",
    divisionId: "",
    roomId: "",
  });

  useEffect(() => {
    if (isOpen) {
      loadDropdowns();
    }
  }, [isOpen]);

  useEffect(() => {
    if (editStudent) {
      setFormData({
        name: editStudent.name || "",
        email: editStudent.email || "",
        enrollmentNo: editStudent.enrollmentNo || "",
        year: editStudent.year || "",
        divisionId: editStudent.divisionId || "",
        roomId: editStudent.roomId || "",
      });
    } else {
      setFormData({
        name: "",
        email: "",
        enrollmentNo: "",
        year: "",
        divisionId: "",
        roomId: "",
      });
    }
  }, [editStudent]);

  const loadDropdowns = async () => {
    try {
      const [divisionData, roomData] = await Promise.all([
        getDivisions(),
        getRooms(),
      ]);

      setDivisions(divisionData);
      setRooms(roomData);
    } catch (error) {
      toast.error("Failed to load dropdown data");
    }
  };

  const handleChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      if (isEdit) {
        await updateStudent(editStudent.studentId, {
          name: formData.name,
          year: formData.year,
          divisionId: Number(formData.divisionId),
          roomId: formData.roomId
            ? Number(formData.roomId)
            : null,
        });

        toast.success(
          "Student updated successfully"
        );
      } else {
        await createStudent({
          ...formData,
          divisionId: Number(formData.divisionId),
          roomId: formData.roomId
            ? Number(formData.roomId)
            : null,
        });

        toast.success(
          "Student created successfully"
        );
      }

      onSuccess();
      onClose();
    } catch (error) {
      toast.error(
        error.response?.data?.message ||
          (isEdit
            ? "Failed to update student"
            : "Failed to create student")
      );
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">

      <div className="w-full max-w-2xl rounded-3xl border border-white/10 bg-slate-900 p-6 shadow-2xl">

        <h2 className="mb-6 text-2xl font-bold text-white">
          {isEdit
            ? "Edit Student"
            : "Add Student"}
        </h2>

        <form
          onSubmit={handleSubmit}
          className="grid grid-cols-2 gap-4"
        >
          <input
            name="name"
            placeholder="Student Name"
            value={formData.name}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3 text-white"
            required
          />

          <input
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
            disabled={isEdit}
            className="rounded-xl bg-slate-800 p-3 text-white disabled:opacity-50"
            required
          />

          <input
            name="enrollmentNo"
            placeholder="Enrollment Number"
            value={formData.enrollmentNo}
            onChange={handleChange}
            disabled={isEdit}
            className="rounded-xl bg-slate-800 p-3 text-white disabled:opacity-50"
            required
          />

          <select
            name="year"
            value={formData.year}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3 text-white"
            required
          >
            <option value="">
              Select Year
            </option>

            {YEARS.map((year) => (
              <option
                key={year}
                value={year}
              >
                {year}
              </option>
            ))}
          </select>

          <select
            name="divisionId"
            value={formData.divisionId}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3 text-white"
            required
          >
            <option value="">
              Select Division
            </option>

            {divisions.map((division) => (
              <option
                key={division.divisionId}
                value={division.divisionId}
              >
                {division.name}
              </option>
            ))}
          </select>

          <select
            name="roomId"
            value={formData.roomId}
            onChange={handleChange}
            className="rounded-xl bg-slate-800 p-3 text-white"
          >
            <option value="">
              Select Room
            </option>

            {rooms.map((room) => (
              <option
                key={room.roomId}
                value={room.roomId}
              >
                {room.roomNumber}
              </option>
            ))}
          </select>

          <div className="col-span-2 mt-4 flex justify-end gap-3">

            <button
              type="button"
              onClick={onClose}
              className="rounded-xl bg-slate-700 px-5 py-2"
            >
              Cancel
            </button>

            <button
              type="submit"
              className="rounded-xl bg-indigo-600 px-5 py-2 hover:bg-indigo-700"
            >
              {isEdit
                ? "Update Student"
                : "Create Student"}
            </button>

          </div>

        </form>

      </div>

    </div>
  );
}