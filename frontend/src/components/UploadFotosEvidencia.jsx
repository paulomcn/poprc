import { useState, useRef } from "react";
import { Upload, X, Image as ImageIcon } from "lucide-react";

const TIPOS_PERMITIDOS = ["image/jpeg", "image/jpg", "image/png", "image/webp"];
const EXTENSOES_PERMITIDAS = ".jpg,.jpeg,.png,.webp";

export default function UploadFotosEvidencia({ arquivos, onChange }) {
  const [arrastando, setArrastando] = useState(false);
  const [erro, setErro] = useState(null);
  const inputRef = useRef(null);

  const validarEAdicionar = (fileList) => {
    const novos = [];
    let mensagemErro = null;

    Array.from(fileList).forEach((file) => {
      if (!TIPOS_PERMITIDOS.includes(file.type)) {
        mensagemErro = `"${file.name}" não é uma imagem suportada (use .jpg, .png ou .webp).`;
        return;
      }
      novos.push(file);
    });

    setErro(mensagemErro);
    if (novos.length > 0) {
      onChange([...arquivos, ...novos]);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setArrastando(false);
    validarEAdicionar(e.dataTransfer.files);
  };

  const handleSelecionarArquivos = (e) => {
    if (e.target.files?.length) {
      validarEAdicionar(e.target.files);
    }
    e.target.value = "";
  };

  const removerArquivo = (index) => {
    onChange(arquivos.filter((_, i) => i !== index));
  };

  return (
    <div className="space-y-3">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setArrastando(true);
        }}
        onDragLeave={() => setArrastando(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition ${
          arrastando
            ? "border-blue-500 bg-blue-50"
            : "border-slate-300 hover:border-slate-400"
        }`}
      >
        <Upload className="w-8 h-8 mx-auto text-slate-400 mb-2" />
        <p className="text-sm text-slate-600 font-medium">
          Arraste fotos aqui ou clique para selecionar
        </p>
        <p className="text-xs text-slate-400 mt-1">
          Formatos aceitos: JPG, PNG, WEBP
        </p>
        <input
          ref={inputRef}
          type="file"
          accept={EXTENSOES_PERMITIDAS}
          multiple
          className="hidden"
          onChange={handleSelecionarArquivos}
        />
      </div>

      {erro && <p className="text-xs text-red-600">{erro}</p>}

      {arquivos.length > 0 && (
        <div className="grid grid-cols-3 sm:grid-cols-4 gap-3">
          {arquivos.map((file, index) => (
            <div
              key={index}
              className="relative group rounded-lg overflow-hidden border border-slate-200"
            >
              <img
                src={URL.createObjectURL(file)}
                alt={file.name}
                className="w-full h-20 object-cover"
              />
              <button
                type="button"
                onClick={() => removerArquivo(index)}
                className="absolute top-1 right-1 bg-black/60 hover:bg-black/80 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition"
              >
                <X size={12} />
              </button>
            </div>
          ))}
        </div>
      )}

      {arquivos.length === 0 && (
        <div className="flex items-center gap-2 text-xs text-slate-400">
          <ImageIcon size={14} /> Nenhuma foto selecionada ainda
        </div>
      )}
    </div>
  );
}
