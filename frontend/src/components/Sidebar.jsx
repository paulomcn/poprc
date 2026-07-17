import { NavLink } from "react-router-dom";
import { API_ORIGIN } from "../services/runtimeConfig";
import rcLogo from "../assets/rclogo.jpg";
import {
  Bell,
  Briefcase,
  Building2,
  ClipboardList,
  DollarSign,
  FileText,
  Layers,
  LayoutDashboard,
  LogOut,
  Package,
  Plane,
  Smartphone,
  TrendingUp,
  Users,
  X,
} from "lucide-react";

const secoesMenu = [
  {
    titulo: "Visão geral",
    itens: [{ path: "/", icon: LayoutDashboard, label: "Dashboard Executivo", end: true }],
  },
  {
    titulo: "Operação",
    itens: [
      { path: "/contratos", icon: FileText, label: "Contratos" },
      { path: "/projetos", icon: Briefcase, label: "Projetos" },
      { path: "/ordens-servico", icon: ClipboardList, label: "Ordens de Serviço" },
      { path: "/obras", icon: Building2, label: "Gestão de Obras" },
      { path: "/funcionarios", icon: Users, label: "Equipes" },
    ],
  },
  {
    titulo: "Materiais e auditoria",
    itens: [
      { path: "/estoque", icon: Package, label: "Estoque" },
      { path: "/auditoria/tecnica", icon: Layers, label: "Retirada e Devolução" },
    ],
  },
  {
    titulo: "Gestão",
    itens: [
      { path: "/financeiro/lucratividade", icon: TrendingUp, label: "Lucratividade" },
      { path: "/financeiro/faturamento", icon: DollarSign, label: "Faturamento" },
      { path: "/logistica/viagens", icon: Plane, label: "Viagens e Reembolsos" },
      { path: "/configuracao-notificacoes", icon: Bell, label: "Notificações" },
    ],
  },
];

export default function Sidebar({ isOpen, onClose }) {
  const handleLogout = () => {
    window.location.href = `${API_ORIGIN}/logout`;
  };

  return (
    <>
      <aside
        aria-label="Navegação principal"
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-800 bg-slate-950 text-white transition-transform duration-200 md:static md:translate-x-0 ${
          isOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex min-h-18 items-center gap-3 border-b border-slate-800 px-4">
          <img src={rcLogo} alt="RC Technology" className="h-10 w-10 rounded object-cover" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-bold text-white">RC Operations Hub</p>
            <p className="text-xs text-slate-400">Central operacional</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar menu"
            title="Fechar menu"
            className="rounded p-2 text-slate-400 hover:bg-slate-800 hover:text-white md:hidden"
          >
            <X size={19} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-3">
          {secoesMenu.map((secao) => (
            <div key={secao.titulo} className="mb-4">
              <p className="mb-1 px-3 text-[10px] font-bold uppercase text-slate-500">
                {secao.titulo}
              </p>
              <ul className="space-y-0.5">
                {secao.itens.map((item) => (
                  <li key={item.path}>
                    <NavLink
                      to={item.path}
                      end={item.end}
                      onClick={onClose}
                      className={({ isActive }) =>
                        `flex min-h-9 items-center gap-3 rounded px-3 py-2 text-sm transition-colors ${
                          isActive
                            ? "bg-blue-600 font-semibold text-white"
                            : "text-slate-300 hover:bg-slate-800 hover:text-white"
                        }`
                      }
                    >
                      <item.icon size={17} aria-hidden="true" />
                      <span className="min-w-0 leading-4">{item.label}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>

        <div className="border-t border-slate-800 p-3">
          <NavLink
            to="/tecnico"
            onClick={onClose}
            className="mb-2 flex min-h-10 items-center gap-3 rounded border border-slate-700 px-3 py-2 text-sm font-semibold text-slate-200 hover:border-blue-500 hover:bg-slate-900"
          >
            <Smartphone size={17} />
            <span>Área do Técnico</span>
          </NavLink>
          <button
            type="button"
            onClick={handleLogout}
            className="flex min-h-9 w-full items-center gap-3 rounded px-3 py-2 text-sm text-slate-400 hover:bg-slate-800 hover:text-white"
          >
            <LogOut size={17} />
            <span>Sair</span>
          </button>
        </div>
      </aside>

      {isOpen && (
        <button
          type="button"
          aria-label="Fechar menu"
          onClick={onClose}
          className="fixed inset-0 z-30 bg-slate-950/60 md:hidden"
        />
      )}
    </>
  );
}
