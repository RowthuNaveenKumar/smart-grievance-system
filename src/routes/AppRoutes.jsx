import { BrowserRouter, Routes, Route } from "react-router-dom";
import SplashScreen from "../pages/SplashScreen";
import Home from "../pages/Home";
import Login from "../pages/Login";
import SignIn from "../pages/SignIn";
import SubmitComplaint from "@/pages/SubmitComplaint";
import ComplaintDetails from "@/pages/ComplaintDetails";
import ProtectedRoute from "./ProtectedRoute";
import StudentDashboard from "@/pages/StudentDashboard";
import StaffDashboard from "@/pages/StaffDashboard";
import AdminDashboard from "@/pages/AdminDashboard";

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

      </Routes>
    </BrowserRouter>
  );
}
