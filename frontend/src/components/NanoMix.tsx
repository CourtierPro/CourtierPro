import React, { useState } from "react";
import axios from "axios";
import "../styles/utilities.css";

type Payload = {
  a_viscosity: number;
  a_density: number;
  b_viscosity: number;
  b_density: number;
  target_volume: number;
};

export default function NanoMix() {
  const [aViscosity, setAViscosity] = useState<string>("");
  const [aDensity, setADensity] = useState<string>("");
  const [bViscosity, setBViscosity] = useState<string>("");
  const [bDensity, setBDensity] = useState<string>("");
  const [targetVolume, setTargetVolume] = useState<string>("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<unknown | null>(null);

  const parseNumber = (v: string) => {
    const n = Number(v);
    return Number.isFinite(n) ? n : null;
  };

  const validate = () => {
    setError(null);
    const fields = [
      ["a_viscosity", aViscosity],
      ["a_density", aDensity],
      ["b_viscosity", bViscosity],
      ["b_density", bDensity],
      ["target_volume", targetVolume],
    ];

    for (const [name, val] of fields) {
      if (val === "" || val === null) {
        setError(`${name} is required`);
        return false;
      }
      const n = parseNumber(val as string);
      if (n === null || n < 0) {
        setError(`${name} must be a non-negative number`);
        return false;
      }
    }
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setResult(null);
    if (!validate()) return;

    const payload: Payload = {
      a_viscosity: parseNumber(aViscosity) as number,
      a_density: parseNumber(aDensity) as number,
      b_viscosity: parseNumber(bViscosity) as number,
      b_density: parseNumber(bDensity) as number,
      target_volume: parseNumber(targetVolume) as number,
    };

    setLoading(true);
    setError(null);
    try {
      const resp = await axios.post("http://localhost:8000/api/analyze", payload, {
        headers: { "Content-Type": "application/json" },
      });
      setResult(resp.data as unknown);
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        if (err.response?.data) setError(JSON.stringify(err.response.data));
        else setError(err.message || "Unknown error");
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError(String(err));
      }
    } finally {
      setLoading(false);
    }
  };

  // Use minimal utility class defined in src/styles/utilities.css
  const inputClass = "input";

  return (
    <div className="max-w-xl mx-auto p-4 bg-white shadow rounded">
      <h2 className="text-lg font-semibold mb-3">NanoMix Analyzer</h2>
      <form onSubmit={handleSubmit} className="space-y-3">
        <div className="grid grid-cols-2 gap-3">
          <label className="flex flex-col">
            <span className="text-sm mb-1">A Viscosity</span>
            <input
              type="number"
              step="any"
              className={inputClass}
              value={aViscosity}
              onChange={(e) => setAViscosity(e.target.value)}
              placeholder="e.g. 12.5"
              aria-label="a_viscosity"
            />
          </label>

          <label className="flex flex-col">
            <span className="text-sm mb-1">A Density</span>
            <input
              type="number"
              step="any"
              className={inputClass}
              value={aDensity}
              onChange={(e) => setADensity(e.target.value)}
              placeholder="e.g. 0.98"
              aria-label="a_density"
            />
          </label>

          <label className="flex flex-col">
            <span className="text-sm mb-1">B Viscosity</span>
            <input
              type="number"
              step="any"
              className={inputClass}
              value={bViscosity}
              onChange={(e) => setBViscosity(e.target.value)}
              placeholder="e.g. 8.3"
              aria-label="b_viscosity"
            />
          </label>

          <label className="flex flex-col">
            <span className="text-sm mb-1">B Density</span>
            <input
              type="number"
              step="any"
              className={inputClass}
              value={bDensity}
              onChange={(e) => setBDensity(e.target.value)}
              placeholder="e.g. 1.02"
              aria-label="b_density"
            />
          </label>
        </div>

        <label className="flex flex-col">
          <span className="text-sm mb-1">Target Volume</span>
          <input
            type="number"
            step="any"
            className={inputClass}
            value={targetVolume}
            onChange={(e) => setTargetVolume(e.target.value)}
            placeholder="e.g. 150"
            aria-label="target_volume"
          />
        </label>

        <div className="flex items-center space-x-2">
          <button
            type="submit"
            disabled={loading}
            className="btn"
          >
            {loading ? "Analyzing..." : "Analyze"}
          </button>
          <button
            type="button"
            onClick={() => {
              setAViscosity("");
              setADensity("");
              setBViscosity("");
              setBDensity("");
              setTargetVolume("");
              setResult(null);
              setError(null);
            }}
            className="btn"
          >
            Reset
          </button>
        </div>
      </form>

      <div className="mt-4">
        {error && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-800 rounded text-sm">{error}</div>
        )}

        {result && (
          <div className="result-box">
            <div className="flex items-center justify-between mb-2">
              <strong>Result</strong>
              <button onClick={() => navigator.clipboard?.writeText(JSON.stringify(result, null, 2))} className="btn">
                Copy
              </button>
            </div>
            <pre className="text-xs overflow-auto whitespace-pre-wrap font-mono">{JSON.stringify(result, null, 2)}</pre>
          </div>
        )}
      </div>
    </div>
  );
}
