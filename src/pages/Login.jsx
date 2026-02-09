import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

export default function Login() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(
        "http://localhost:8080/auth/login",
        { email, password }
      );

      const { token, role } = res.data;

      localStorage.setItem("token", token);
      localStorage.setItem("role", role);

      // Role-based navigation
      if (role === "STUDENT") navigate("/student");
      else if (role === "MFT") navigate("/mft");
      else if (role === "HOD") navigate("/hod");
      else if (role === "DEAN") navigate("/dean");
      else if (role === "PRESIDENT") navigate("/president");

    } catch (err) {
      alert("Invalid credentials ❌");
    }
  };

  return (
    <div className="h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white p-8 shadow-xl rounded-xl w-96">
        <h1 className="text-3xl font-semibold mb-6 text-center">Login</h1>

        <form onSubmit={submit}>
          <input
            className="w-full border p-2 mb-4 rounded"
            placeholder="Email"
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <input
            className="w-full border p-2 mb-4 rounded"
            placeholder="Password"
            type="password"
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <button
            type="submit"
            className="w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded-lg transition"
          >
            Login
          </button>
        </form>
      </div>
    </div>
  );
}
