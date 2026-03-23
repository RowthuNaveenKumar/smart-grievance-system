import { Navigate } from "react-router-dom";
import { useUser } from "@/context/UserContext";

const ProtectedRoute = ({ children, role }) => {
  const { user, loading } = useUser();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!user) {
    return <Navigate to="/login" />;
  }

  if (role === "STUDENT" && user.accountType !== "STUDENT") {
    return <Navigate to="/unauthorized" />;
  }

  if (role === "STAFF" && user.accountType !== "STAFF") {
    return <Navigate to="/unauthorized" />;
  }

  if (role === "ADMIN" && user.role !== "ADMIN") {
    return <Navigate to="/unauthorized" />;
  }

  return children;
};

export default ProtectedRoute;
