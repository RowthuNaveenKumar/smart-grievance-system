import { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

export default function SignIn() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    try {
      await axios.post("http://localhost:8080/auth/signin", {
        email,
        password,
      });

      alert("Password set successfully ✅");
      navigate("/login");
    } catch (err) {
      alert("Sign-in failed ❌");
    }
  };

  return (
    <div className="h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white p-8 rounded-xl shadow-lg w-96">
        <h1 className="text-2xl font-bold mb-6 text-center">
          First-time Sign-In
        </h1>

        <form onSubmit={submit}>
          <input
            className="w-full border p-2 mb-4 rounded"
            placeholder="College Email"
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <input
            className="w-full border p-2 mb-4 rounded"
            placeholder="New Password"
            type="password"
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <button
            type="submit"
            className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg transition"
          >
            Sign In
          </button>
        </form>
      </div>
    </div>
  );
}
