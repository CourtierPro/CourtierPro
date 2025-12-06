import axios from "axios";

export type AnalyzePayload = {
  a_viscosity: number;
  a_density: number;
  b_viscosity: number;
  b_density: number;
  target_volume: number;
};

export class ApiError extends Error {
  status?: number;
  data?: any;

  constructor(message: string, status?: number, data?: any) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

// Dedicated axios instance for NanoMix prototype
const instance = axios.create({
  baseURL: "http://localhost:8000",
  headers: { "Content-Type": "application/json" },
  // do not send cookies by default for this prototype, remove if needed
  withCredentials: false,
});

/**
 * Sends payload to POST /api/analyze and returns the backend response body.
 * Throws ApiError on network or API errors so callers can handle them.
 */
export async function analyzeProcess(payload: AnalyzePayload): Promise<any> {
  try {
    const resp = await instance.post("/api/analyze", payload);
    return resp.data;
  } catch (err: any) {
    // Axios error handling
    if (axios.isAxiosError(err)) {
      const status = err.response?.status;
      const data = err.response?.data;
      const message = data?.message || err.message || "Request failed";
      throw new ApiError(message, status, data);
    }

    // Non-Axios errors
    throw new ApiError(err?.message ?? String(err));
  }
}

export default analyzeProcess;
