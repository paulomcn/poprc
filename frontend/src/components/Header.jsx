import { Link, useLocation } from "react-router-dom";
import { Bell, Menu } from "lucide-react";
import UserMenu from "./UserMenu";

const contextoRotas = [
  { teste: (path) => path === "/", area: "Visão geral", pagina: "Dashboard Executivo" },
  { teste: (path) => path.startsWith("/contratos"), area: "Operação", pagina: "Contratos" },
  { teste: (path) => path.startsWith("/projetos"), area: "Operação", pagina: "Projetos" },
  { teste: (path) => path.startsWith("/ordens-servico"), area: "Operação", pagina: "Ordens de Serviço" },
  { teste: (path) => path.startsWith("/obras"), area: "Operação", pagina: "Gestão de Obras" },
  { teste: (path) => path.startsWith("/funcionarios"), area: "Operação", pagina: "Equipes" },
  { teste: (path) => path.startsWith("/estoque"), area: "Materiais", pagina: "Estoque" },
  { teste: (path) => path.startsWith("/auditoria"), area: "Auditoria", pagina: "Retirada e Devolução" },
  { teste: (path) => path.startsWith("/financeiro/lucratividade"), area: "Gestão", pagina: "Lucratividade" },
  { teste: (path) => path.startsWith("/financeiro/faturamento"), area: "Gestão", pagina: "Faturamento" },
  { teste: (path) => path.startsWith("/logistica"), area: "Gestão", pagina: "Viagens e Reembolsos" },
  { teste: (path) => path.startsWith("/configuracao"), area: "Sistema", pagina: "Notificações" },
];

export default function Header({ usuario, onMenuClick }) {
  const { pathname } = useLocation();
  const contexto = contextoRotas.find((item) => item.teste(pathname)) || {
    area: "RC Operations Hub",
    pagina: "Central operacional",
  };

  return (
    <header className="z-20 flex min-h-18 items-center justify-between border-b border-slate-200 bg-white px-4 sm:px-6">
      <div className="flex min-w-0 items-center gap-3">
        <button
          type="button"
          onClick={onMenuClick}
          aria-label="Abrir menu"
          title="Abrir menu"
          className="rounded border border-slate-200 p-2 text-slate-700 hover:bg-slate-50 md:hidden"
        >
          <Menu size={20} />
        </button>
        <div className="min-w-0">
          <p className="truncate text-xs font-semibold text-slate-500">{contexto.area}</p>
          <p className="truncate text-sm font-bold text-slate-900 sm:text-base">{contexto.pagina}</p>
        </div>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        {["ADMIN", "SUPERVISOR_TECNICO"].includes(usuario?.perfil) && (
          <Link
            to="/configuracao-notificacoes"
            aria-label="Abrir notificações"
            title="Notificações"
            className="rounded border border-slate-200 p-2 text-slate-500 hover:bg-slate-50 hover:text-blue-700"
          >
            <Bell size={18} />
          </Link>
        )}
        <UserMenu />
      </div>
    </header>
  );
}
