import http from "./http.js";

export async function getMyResumes() {
  const { data } = await http.get("/resumes/mine");
  return data.data;
}

export async function createResume(formData) {
  const { data } = await http.post("/resumes", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data.data;
}

export async function deleteResume(id) {
  const { data } = await http.delete(`/resumes/${id}`);
  return data.data;
}

export async function downloadResume(id) {
  const response = await http.get(`/resumes/${id}/download`, {
    responseType: "blob",
  });

  const filename = extractFilename(response.headers["content-disposition"]) || `resume-${id}`;
  const objectUrl = window.URL.createObjectURL(response.data);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(objectUrl);
  return filename;
}

function extractFilename(contentDisposition = "") {
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }

  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
  return plainMatch?.[1] || "";
}
