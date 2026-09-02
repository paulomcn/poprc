const buildEnv = import.meta.env || {};

export const APP_VERSION_LABEL = buildEnv.VITE_APP_VERSION_LABEL || "v0.1.0+dev";
