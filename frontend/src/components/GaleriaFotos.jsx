import { useState } from "react";
import { X, ChevronLeft, ChevronRight, ImageOff } from "lucide-react";

const API_FILE_BASE_URL = "http://localhost:8085";

export default function GaleriaFotos({ fotos = [] }) {
  const [indiceAberto, setIndiceAberto] = useState(null);

  if (!fotos || fotos.length === 0) {
    return (
      <div className="flex items-center gap-2 text-xs text-slate-400 py-2">
        <ImageOff size={14} /> Nenhuma evidência fotográfica registrada
      </div>
    );
  }

  const urlCompleta = (caminho) => `${API_FILE_BASE_URL}${caminho}`;

  const irParaProxima = () => setIndiceAberto((i) => (i + 1) % fotos.length);
  const irParaAnterior = () =>
    setIndiceAberto((i) => (i - 1 + fotos.length) % fotos.length);

  return (
    <>
      <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2">
        {fotos.map((foto, index) => (
          <button
            key={index}
            type="button"
            onClick={() => setIndiceAberto(index)}
            className="aspect-square rounded-lg overflow-hidden border border-slate-200 hover:opacity-80 transition"
          >
            <img
              src={urlCompleta(foto)}
              alt={`Evidência ${index + 1}`}
              className="w-full h-full object-cover"
            />
          </button>
        ))}
      </div>

      {indiceAberto !== null && (
        <div
          className="fixed inset-0 bg-black/90 flex items-center justify-center z-[60] p-4"
          onClick={() => setIndiceAberto(null)}
        >
          <button
            onClick={(e) => {
              e.stopPropagation();
              setIndiceAberto(null);
            }}
            className="absolute top-4 right-4 text-white/80 hover:text-white"
          >
            <X size={28} />
          </button>

          {fotos.length > 1 && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                irParaAnterior();
              }}
              className="absolute left-4 text-white/80 hover:text-white"
            >
              <ChevronLeft size={36} />
            </button>
          )}

          <img
            src={urlCompleta(fotos[indiceAberto])}
            alt={`Evidência ${indiceAberto + 1}`}
            onClick={(e) => e.stopPropagation()}
            className="max-w-full max-h-[85vh] object-contain rounded-lg"
          />

          {fotos.length > 1 && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                irParaProxima();
              }}
              className="absolute right-4 text-white/80 hover:text-white"
            >
              <ChevronRight size={36} />
            </button>
          )}

          <span className="absolute bottom-4 text-white/70 text-xs">
            {indiceAberto + 1} / {fotos.length}
          </span>
        </div>
      )}
    </>
  );
}
