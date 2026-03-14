import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "../pages/Home";
import Login from "../pages/Login";
import SignIn from "../pages/SignIn";
import Dashboard from "../pages/Dashboard";
import SubmitComplaint from "../pages/student/SubmitComplaint";
import ComplaintDetails from "../pages/student/ComplaintDetails";

function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>

        <Route path="/" element={<Home />} />

        <Route path="/login" element={<Login />} />

        <Route path="/signin" element={<SignIn />} />

        <Route path="/dashboard" element={<Dashboard />} />

        <Route path="/submit-complaint" element={<SubmitComplaint />} />

        <Route path="/complaint/:id" element={<ComplaintDetails />} />

      </Routes>
    </BrowserRouter>
  );
}

export default AppRoutes;