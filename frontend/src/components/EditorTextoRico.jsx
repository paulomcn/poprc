import { useRef, useEffect } from "react";
import { Bold, Italic, List } from "lucide-react";

export default function EditorTextoRico({ value, onChange, placeholder }) {
  const editorRef = useRef(null);

  useEffect(() => {
    if (editorRef.current && editorRef.current.innerHTML !== value) {
      editorRef.current.innerHTML = value || "";
    }
  }, [value]);

  const aplicarFormatacao = (comando) => {
    document.execCommand(comando);
    editorRef.current?.focus();
    onChange(editorRef.current.innerHTML);
  };

  return (
    <div className="border border-slate-300 rounded-lg overflow-hidden focus-within:ring-2 focus-within:ring-blue-500">
      <div className="flex items-center gap-1 bg-slate-50 border-b border-slate-200 px-2 py-1.5">
        <button
          type="button"
          onClick={() => aplicarFormatacao("bold")}
          className="p-1.5 hover:bg-slate-200 rounded"
          title="Negrito"
        >
          <Bold size={14} />
        </button>
        <button
          type="button"
          onClick={() => aplicarFormatacao("italic")}
          className="p-1.5 hover:bg-slate-200 rounded"
          title="Itálico"
        >
          <Italic size={14} />
        </button>
        <button
          type="button"
          onClick={() => aplicarFormatacao("insertUnorderedList")}
          className="p-1.5 hover:bg-slate-200 rounded"
          title="Lista"
        >
          <List size={14} />
        </button>
      </div>
      <div
        ref={editorRef}
        contentEditable
        onInput={(e) => onChange(e.currentTarget.innerHTML)}
        className="min-h-[120px] px-4 py-3 text-sm text-slate-800 focus:outline-none [&_ul]:list-disc [&_ul]:pl-5"
        data-placeholder={placeholder}
        suppressContentEditableWarning
      />
    </div>
  );
}
