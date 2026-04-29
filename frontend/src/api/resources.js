import http from "./http.js";

const RESOURCE_UPLOAD_SESSION_PREFIX = "one-stop-future-resource-upload:";
const DEFAULT_RESOURCE_CHUNK_SIZE = 5 * 1024 * 1024;

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

export async function createResourceUpload(formData, options = {}) {
  const file = formData.get("file");

  if (!file) {
    throw new Error("file is required");
  }

  const metadata = resourceUploadMetadataOf(formData, file);
  const resumeKey = resourceUploadResumeKey(metadata, file);
  let session = await findResumableResourceUploadSession(resumeKey, metadata);

  if (!session) {
    session = await initiateResourceChunkUpload(metadata);
    rememberResourceUploadSession(resumeKey, session.uploadId);
  }

  emitUploadProgress(options.onProgress, "uploading", session);

  let uploadedChunks = new Set(session.uploadedChunks || []);
  for (let chunkIndex = 0; chunkIndex < session.totalChunks; chunkIndex += 1) {
    if (uploadedChunks.has(chunkIndex)) {
      continue;
    }

    const start = chunkIndex * session.chunkSize;
    const end = Math.min(start + session.chunkSize, file.size);
    const chunkForm = new FormData();
    chunkForm.append("chunk", file.slice(start, end), `${file.name}.part${chunkIndex}`);

    session = await uploadResourceChunk(session.uploadId, chunkIndex, chunkForm);
    uploadedChunks = new Set(session.uploadedChunks || []);
    emitUploadProgress(options.onProgress, "uploading", session);
  }

  emitUploadProgress(options.onProgress, "completing", session);
  const resource = await completeResourceChunkUpload(session.uploadId);
  forgetResourceUploadSession(resumeKey);
  emitUploadProgress(options.onProgress, "complete", {
    ...session,
    uploadedBytes: session.fileSize,
    complete: true,
  });
  return resource;
}

export async function initiateResourceChunkUpload(metadata) {
  const { data } = await http.post("/resources/chunk-uploads", metadata);
  return data.data;
}

export async function getResourceChunkUploadStatus(uploadId) {
  const { data } = await http.get(`/resources/chunk-uploads/${uploadId}`);
  return data.data;
}

export async function uploadResourceChunk(uploadId, chunkIndex, formData) {
  const { data } = await http.post(`/resources/chunk-uploads/${uploadId}/chunks/${chunkIndex}`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    timeout: 30000,
  });
  return data.data;
}

export async function completeResourceChunkUpload(uploadId) {
  const { data } = await http.post(`/resources/chunk-uploads/${uploadId}/complete`, null, {
    timeout: 60000,
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

export async function previewResource(id) {
  const response = await http.get(`/resources/${id}/preview`, {
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

export async function previewZipResource(id) {
  const { data } = await http.get(`/resources/${id}/preview-zip`);
  return data.data;
}

function extractFilename(contentDisposition = "") {
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]);
  }

  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i);
  return plainMatch?.[1] || "";
}

function resourceUploadMetadataOf(formData, file) {
  return {
    title: String(formData.get("title") || ""),
    category: String(formData.get("category") || ""),
    summary: String(formData.get("summary") || ""),
    description: String(formData.get("description") || ""),
    fileName: file.name,
    contentType: file.type || "application/octet-stream",
    fileSize: file.size,
    chunkSize: DEFAULT_RESOURCE_CHUNK_SIZE,
  };
}

async function findResumableResourceUploadSession(resumeKey, metadata) {
  const uploadId = readRememberedResourceUploadSession(resumeKey);

  if (!uploadId) {
    return null;
  }

  try {
    const session = await getResourceChunkUploadStatus(uploadId);
    if (session.fileName === metadata.fileName && session.fileSize === metadata.fileSize) {
      return session;
    }
  } catch {
    // Stale upload sessions are discarded and recreated below.
  }

  forgetResourceUploadSession(resumeKey);
  return null;
}

function emitUploadProgress(callback, phase, session) {
  if (!callback) {
    return;
  }

  const uploadedBytes = Number(session.uploadedBytes || 0);
  const fileSize = Number(session.fileSize || 0);
  callback({
    phase,
    uploadId: session.uploadId,
    uploadedChunks: session.uploadedChunks?.length || 0,
    totalChunks: session.totalChunks || 0,
    uploadedBytes,
    fileSize,
    percent: fileSize > 0 ? Math.min(100, Math.round((uploadedBytes / fileSize) * 100)) : 0,
  });
}

function resourceUploadResumeKey(metadata, file) {
  const fingerprint = [
    metadata.title,
    metadata.category,
    metadata.summary,
    metadata.description,
    metadata.fileName,
    metadata.fileSize,
    file.lastModified || 0,
  ].join("|");
  return `${RESOURCE_UPLOAD_SESSION_PREFIX}${hashString(fingerprint)}`;
}

function hashString(value) {
  let hash = 0;

  for (let index = 0; index < value.length; index += 1) {
    hash = ((hash << 5) - hash + value.charCodeAt(index)) | 0;
  }

  return Math.abs(hash).toString(36);
}

function readRememberedResourceUploadSession(resumeKey) {
  try {
    return window.localStorage.getItem(resumeKey);
  } catch {
    return null;
  }
}

function rememberResourceUploadSession(resumeKey, uploadId) {
  try {
    window.localStorage.setItem(resumeKey, uploadId);
  } catch {
    // Browser storage may be unavailable; upload still works without resume memory.
  }
}

function forgetResourceUploadSession(resumeKey) {
  try {
    window.localStorage.removeItem(resumeKey);
  } catch {
    // Browser storage may be unavailable; nothing else to clean up.
  }
}
