import { Navigate } from "react-router-dom";

const ProtectedRoute = ({ children, role }) => {
  const userRole = localStorage.getItem("role");
  const userType = localStorage.getItem("userType");

  if (!userRole && !userType) {
    return <Navigate to="/login" />;
  }

  if (role) {
    if (role === "STAFF" && userType !== "STAFF") {
      return <Navigate to="/unauthorized" />;
    }

    if (role === "STUDENT" && userType !== "STUDENT") {
      return <Navigate to="/unauthorized" />;
    }

    if (role === "ADMIN" && userRole !== "ADMIN") {
      return <Navigate to="/unauthorized" />;
    }
  }

  return children;
};

export default ProtectedRoute;