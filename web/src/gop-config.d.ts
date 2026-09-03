export interface GopRuntimeConfig {
  apiBaseUrl?: string;
}

declare global {
  interface Window {
    __GOP_CONFIG__?: GopRuntimeConfig;
  }
}

export {};
