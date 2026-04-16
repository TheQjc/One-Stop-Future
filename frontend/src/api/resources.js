import http from "./http.js";

export async function getResources(params = {}) {
  const { data } = await http.get("/resources", { params });
  return data.data;
}

export async function getMyResources() {
  const { data } = await http.get("/resources/mine");
  return data.data;
}

export async function getResourceDetail(id) {
  const { data } = await http.get(`/resources/${id}`);
  return data.data;
}

export async function createResourceUpload(formData) {
  const { data } = await http.post("/resources", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data.data;
}

export async function updateResource(id, formData) {
  const { data } = await http.put(`/resources/${id}`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return data.data;
}

export async function favoriteResource(id) {
  const { data } = await http.post(`/resources/${id}/favorite`);
  return data.data;
}

export async function unfavoriteResource(id) {
  const { data } = await http.delete(`/resources/${id}/favorite`);
  return data.data;
}

export async function getMyResourceFavorites() {
  const { data } = await http.get("/users/me/favorites", {
    params: { type: "RESOURCE" },
  });
  return data.data;
}

export async function downloadResource(id) {
  const response = await http.get(`/resources/${id}/download`, {
    responseType: "blob",
  });

  const filename = extractFilename(response.headers["content-disposition"]) || `resource-${id}`;
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
