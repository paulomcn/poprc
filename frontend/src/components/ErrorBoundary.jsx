import { Component } from "react";
import { AlertTriangle, Home, RefreshCw } from "lucide-react";
import rcLogo from "../assets/rclogo.jpg";

export default class ErrorBoundary extends Component {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error("Falha ao renderizar a página", error, info);
  }

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-100 p-5">
        <section className="w-full max-w-lg rounded-lg border border-slate-200 bg-white p-7 shadow-sm">
          <div className="flex items-center gap-3 border-b border-slate-200 pb-5">
            <img src={rcLogo} alt="RC Technology" className="h-10 w-10 rounded object-cover" />
            <div>
              <p className="text-sm font-bold text-slate-900">RC Operations Hub</p>
              <p className="text-xs text-slate-500">Central operacional</p>
            </div>
          </div>
          <div className="mt-6 flex items-start gap-3">
            <AlertTriangle className="mt-0.5 shrink-0 text-amber-600" size={22} />
            <div>
              <h1 className="text-lg font-bold text-slate-900">Não foi possível abrir esta página</h1>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                O restante do sistema continua disponível. Recarregue a tela ou volte ao painel principal.
              </p>
            </div>
          </div>
          <div className="mt-6 grid grid-cols-1 gap-2 sm:grid-cols-2">
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="inline-flex h-10 items-center justify-center gap-2 rounded bg-blue-600 px-4 text-sm font-bold text-white hover:bg-blue-700"
            >
              <RefreshCw size={16} /> Recarregar
            </button>
            <button
              type="button"
              onClick={() => { window.location.href = "/"; }}
              className="inline-flex h-10 items-center justify-center gap-2 rounded border border-slate-300 bg-white px-4 text-sm font-bold text-slate-700 hover:bg-slate-50"
            >
              <Home size={16} /> Ir ao Dashboard
            </button>
          </div>
        </section>
      </main>
    );
  }
}
