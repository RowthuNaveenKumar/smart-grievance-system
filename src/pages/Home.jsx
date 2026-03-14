import { useNavigate } from "react-router-dom";
export default function Home() {

  const navigate = useNavigate();

  return (

    <div className="min-h-screen bg-white">

      {/* NAVBAR */}

      <div className="flex justify-between items-center px-12 py-6">
        <h1 className="text-2xl font-bold text-indigo-600">
          Smart<span className="text-black">Griev</span>
        </h1>

        <div className="flex gap-6">

          <button
            onClick={() => navigate("/login")}
            className="px-6 py-2 text-indigo-600 font-medium"
          >
            Login
          </button>

          <button
            onClick={() => navigate("/signin")}
            className="px-6 py-2 bg-indigo-600 text-white rounded-lg"
          >
            Register
          </button>

        </div>

      </div>


      {/* HERO SECTION */}

      <div className="grid md:grid-cols-2 items-center px-12 pt-20">

        <div>

          <h1 className="text-6xl font-extrabold leading-tight mb-6">

            A Smarter Way to  
            <span className="text-indigo-600"> Manage Grievances</span>

          </h1>

          <p className="text-gray-600 text-lg mb-8 max-w-xl">

            SmartGriev helps universities streamline grievance
            submission, tracking, and resolution through a
            transparent and efficient platform.

          </p>

          <div className="flex gap-6">

            <button
              onClick={() => navigate("/login")}
              className="px-8 py-3 bg-indigo-600 text-white rounded-lg
              hover:bg-indigo-700 shadow-lg"
            >
              Get Started
            </button>

            <button
              onClick={() => navigate("/signin")}
              className="px-8 py-3 border border-gray-300 rounded-lg
              hover:bg-gray-100"
            >
              Create Account
            </button>

          </div>

        </div>


        {/* IMAGE */}

        <div className="flex justify-center">

          <img
            src="https://images.unsplash.com/photo-1523240795612-9a054b0db644"
            alt="students collaboration"
            className="rounded-xl shadow-2xl w-[500px]"
          />

        </div>

      </div>


      {/* FEATURES */}

      <div className="grid md:grid-cols-3 gap-10 px-12 mt-28">

        <div className="p-6 rounded-xl shadow-lg">

          <h3 className="text-xl font-semibold mb-2">
            Easy Complaint Submission
          </h3>

          <p className="text-gray-600">
            Students can submit grievances quickly with
            structured forms and documentation.
          </p>

        </div>


        <div className="p-6 rounded-xl shadow-lg">

          <h3 className="text-xl font-semibold mb-2">
            Real-Time Tracking
          </h3>

          <p className="text-gray-600">
            Track complaint status from submission
            to resolution in real time.
          </p>

        </div>


        <div className="p-6 rounded-xl shadow-lg">

          <h3 className="text-xl font-semibold mb-2">
            Transparent Resolution
          </h3>

          <p className="text-gray-600">
            Authorities manage grievances efficiently
            ensuring transparency and accountability.
          </p>

        </div>

      </div>

    </div>

  );

}