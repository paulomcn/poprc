export const API_ORIGIN = import.meta.env.VITE_API_ORIGIN || "";

export const API_BASE_URL = "/api";

export const buildApiFileUrl = (path) => {
  if (!path) return "";
  if (
    path.startsWith("data:") ||
    path.startsWith("http://") ||
    path.startsWith("https://") ||
    path.startsWith("blob:")
  ) {
    return path;
  }
  return path;
};
