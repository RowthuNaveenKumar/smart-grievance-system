import API_BASE from "../services/api";


export const predictComplaint = async (title, description) => {
  const res = await fetch(`${API_BASE}/complaints/predict`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      title,
      complaint_text: description
    })
  });

  return res.json();
};

export const createComplaint = async (data, files, token) => {
  const formData = new FormData();

  formData.append(
    "data",
    new Blob([JSON.stringify(data)], { type: "application/json" })
  );

  files.forEach(file => {
    formData.append("files", file);
  });

  const res = await fetch(`${API_BASE}/complaints/create`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`
    },
    body: formData
  });

  return res.json();
};

export const getComplaintById = async (id) => {
  const res = await fetch(`${API_BASE}/complaints/${id}`);
  return res.json();
};