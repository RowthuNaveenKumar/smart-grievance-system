import { BrowserRouter, Routes, Route } from "react-router-dom";

import Login from "../pages/Login";
import SignIn from "../pages/SignIn";
import Dashboard from "../pages/Dashboard";
import SubmitComplaint from "@/pages/SubmitComplaint";
import ComplaintDetails from "@/pages/ComplaintDetails";

export default function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/signin" element={<SignIn />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/submit" element={<SubmitComplaint/> } />
        <Route path="/complaint/:id" element={<ComplaintDetails />} />
      </Routes>
    </BrowserRouter>
  );
}