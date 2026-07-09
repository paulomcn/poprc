const DEFAULT_API_PORT = "8085";

const runtimeHostname = () => {
  if (typeof window !== "undefined" && window.location?.hostname) {
    return window.location.hostname;
  }
  return "localhost";
};

export const API_ORIGIN =
  import.meta.env.VITE_API_ORIGIN ||
  `http://${runtimeHostname()}:${import.meta.env.VITE_API_PORT || DEFAULT_API_PORT}`;

export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || `${API_ORIGIN}/api`;

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
  return `${API_ORIGIN}${path}`;
};
