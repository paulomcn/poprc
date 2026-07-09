import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  FileText,
  Briefcase,
  Users,
  Building2,
  Package,
  LogOut,
  Menu,
  X,
  Smartphone,
  // 💥 ÍCONE NOVO PARA A O.S.
  ClipboardList,
  TrendingUp,
  DollarSign,
  Plane,
  Layers,
  Bell,
} from "lucide-react";

export default function Sidebar() {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);

  // 🎛️ ARRAY ATUALIZADO COM A ORDEM DE SERVIÇO NO LUGAR CERTO 💥
  const menuItems = [
    { path: "/", icon: LayoutDashboard, label: "Dashboard Executivo" },
    { path: "/contratos", icon: FileText, label: "Contratos" },
    { path: "/projetos", icon: Briefcase, label: "Projetos" },
    {
      path: "/ordens-servico",
      icon: ClipboardList,
      label: "Ordens de Serviço",
    }, // 💥 LINK INJETADO AQUI!
    { path: "/funcionarios", icon: Users, label: "Funcionários" },
    { path: "/obras", icon: Building2, label: "Gestão de Obras" },
    { path: "/estoque", icon: Package, label: "Estoque de Materiais" },

    // Novas páginas do financeiro, logística e engenharia
    {
      path: "/financeiro/lucratividade",
      icon: TrendingUp,
      label: "Lucratividade",
    },
    {
      path: "/financeiro/faturamento",
      icon: DollarSign,
      label: "Gestão Faturamento",
    },
    { path: "/logistica/viagens", icon: Plane, label: "Viagens e Reembolsos" },
    { path: "/auditoria/tecnica", icon: Layers, label: "Auditoria de Retirada/Devolução" },
    { path: "/configuracao-notificacoes", icon: Bell, label: "Notificações" },

    { path: "/tecnico", icon: Smartphone, label: "Área do Técnico" },
  ];

  const isActive = (path) => location.pathname === path;

  const handleLogout = () => {
    window.location.href = "http://localhost:8085/logout";
  };

  return (
    <>
      {/* Mobile Toggle */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="md:hidden fixed top-4 left-4 z-50 p-2 rounded-lg bg-slate-100"
      >
        {isOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      {/* Sidebar */}
      <aside
        className={`${
          isOpen ? "translate-x-0" : "-translate-x-full"
        } md:translate-x-0 fixed md:static top-0 left-0 h-screen w-64 bg-gradient-to-b from-slate-900 to-slate-800 text-white transition-transform duration-300 z-40 flex flex-col`}
      >
        {/* Logo */}
        <div className="p-6 border-b border-slate-700">
          <h1 className="text-2xl font-bold text-white">RC Hub</h1>
          <p className="text-slate-400 text-sm mt-1">Operations</p>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4 overflow-y-auto">
          <ul className="space-y-2">
            {menuItems.map((item) => (
              <li key={item.path}>
                <Link
                  to={item.path}
                  onClick={() => setIsOpen(false)}
                  className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    isActive(item.path)
                      ? "bg-blue-600 text-white"
                      : "text-slate-300 hover:bg-slate-700"
                  }`}
                >
                  <item.icon size={20} />
                  <span>{item.label}</span>
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        {/* Logout */}
        <div className="p-4 border-t border-slate-700">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-slate-300 hover:bg-slate-700 transition-colors"
          >
            <LogOut size={20} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Mobile Overlay */}
      {isOpen && (
        <div
          onClick={() => setIsOpen(false)}
          className="fixed inset-0 bg-black/50 md:hidden z-30"
        />
      )}
    </>
  );
}
