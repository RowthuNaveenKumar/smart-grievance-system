import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "../pages/Home";
import Login from "../pages/Login";
import SignIn from "../pages/SignIn";
import SubmitComplaint from "@/pages/SubmitComplaint";
import ComplaintDetails from "@/pages/ComplaintDetails";
import ProtectedRoute from "./ProtectedRoute";
import StudentDashboard from "@/pages/StudentDashboard";
import StaffDashboard from "@/pages/StaffDashboard";
import AdminDashboard from "@/pages/AdminDashboard";
import SplashScreen from "@/pages/SplashScreen";
import StudentsManagement from "@/pages/admin/StudentsManagement";
import StaffManagement from "@/pages/admin/StaffManagement";
import ComplaintsManagement from "@/pages/admin/ComplaintsManagement";
import DepartmentsManagement from "@/pages/admin/DepartmentsManagement";
import CategoriesManagement from "@/pages/admin/CategoriesManagement";
import WorkflowsManagement from "@/pages/admin/WorkflowsManagement";

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SplashScreen />} />
        <Route path="/home" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signin" element={<SignIn />} />

        <Route
          path="/student-dashboard"
          element={
            <ProtectedRoute role="STUDENT">
              <StudentDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/submit"
          element={
            <ProtectedRoute role="STUDENT">
              <SubmitComplaint />
            </ProtectedRoute>
          }
        />

        <Route
          path="/complaint/:id"
          element={
            <ProtectedRoute>
              <ComplaintDetails />
            </ProtectedRoute>
          }
        />

        <Route
          path="/staff-dashboard"
          element={
            <ProtectedRoute role="STAFF">
              <StaffDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin-dashboard"
          element={
            <ProtectedRoute role="ADMIN">
              <AdminDashboard />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/complaints"
          element={
            <ProtectedRoute role="ADMIN">
              <ComplaintsManagement />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/students"
          element={
            <ProtectedRoute role="ADMIN">
              <StudentsManagement />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/staff"
          element={
            <ProtectedRoute role="ADMIN">
              <StaffManagement />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/departments"
          element={
            <ProtectedRoute role="ADMIN">
              <DepartmentsManagement />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/categories"
          element={
            <ProtectedRoute role="ADMIN">
              <CategoriesManagement />
            </ProtectedRoute>
          }
        />

        <Route
          path="/admin/workflows"
          element={
            <ProtectedRoute role="ADMIN">
              <WorkflowsManagement />
            </ProtectedRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
}
