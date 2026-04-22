import http from "./http.js";

export async function getMyApplications() {
  const { data } = await http.get("/applications/mine");
  return data.data;
}

export async function downloadMyApplicationResume(id) {
  const response = await http.get(`/applications/${id}/resume/download`, {
    responseType: "blob",
  });

  const filename = extractFilename(response.headers["content-disposition"]) || `application-resume-${id}`;
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

export async function previewMyApplicationResume(id) {
  const response = await http.get(`/applications/${id}/resume/preview`, {
    responseType: "blob",
  });

  const objectUrl = window.URL.createObjectURL(response.data);
  const previewWindow = window.open(objectUrl, "_blank", "noopener");

  if (!previewWindow) {
    window.location.assign(objectUrl);
  }

  window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 60_000);
  return objectUrl;
}

function extractFilename(contentDisposition = "") {
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }

  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
  return plainMatch?.[1] || "";
}
