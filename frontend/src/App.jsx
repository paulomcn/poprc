import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import { AuthProvider } from "./contexts/AuthContext";
import AuditoriaMateriaisEAsBuilt from "./pages/AuditoriaMateriaisEAsBuilt";
import ConfiguracaoNotificacoes from "./pages/ConfiguracaoNotificacoes";
import Contratos from "./pages/Contratos";
import DashboardExecutivo from "./pages/DashboardExecutivo";
import ExecutarOrdemServico from "./pages/ExecutarOrdemServico";
import Funcionarios from "./pages/Funcionarios";
import GestaoComarcas from "./pages/GestaoComarcas";
import GestaoFaturamento from "./pages/GestaoFaturamento";
import GestaoOrdensServico from "./pages/GestaoOrdensServico";
import Login from "./pages/Login";
import PainelEstoque from "./pages/PainelEstoque";
import PainelFinanceiro from "./pages/PainelFinanceiro";
import PainelViagensEReembolso from "./pages/PainelViagensEReembolso";
import PortalTecnicoDashboard from "./pages/PortalTecnicoDashboard";
import Projetos from "./pages/Projetos";
import MeuPerfil from "./pages/MeuPerfil";

const ADMIN = ["ADMIN"];
const GESTAO = ["ADMIN", "SUPERVISOR_TECNICO"];
const CAMPO = ["ADMIN", "SUPERVISOR_TECNICO", "TECNICO"];
const OBRAS = [...CAMPO, "AUDITOR"];
const ESTOQUE = ["ADMIN", "ESTOQUE"];
const AUDITORIA = ["ADMIN", "AUDITOR"];

function Pagina({ children, roles, layout = true }) {
  const conteudo = layout ? <Layout>{children}</Layout> : children;
  return <ProtectedRoute roles={roles}>{conteudo}</ProtectedRoute>;
}

function App() {
  return (
    <AuthProvider>
      <Router future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/perfil" element={<Pagina><MeuPerfil /></Pagina>} />
          <Route path="/" element={<Pagina roles={GESTAO}><DashboardExecutivo /></Pagina>} />
          <Route path="/contratos" element={<Pagina roles={GESTAO}><Contratos /></Pagina>} />
          <Route path="/projetos" element={<Pagina roles={GESTAO}><Projetos /></Pagina>} />
          <Route path="/funcionarios" element={<Pagina roles={ADMIN}><Funcionarios /></Pagina>} />
          <Route path="/ordens-servico" element={<Pagina roles={CAMPO}><GestaoOrdensServico /></Pagina>} />
          <Route path="/obras" element={<Pagina roles={OBRAS}><GestaoComarcas /></Pagina>} />
          <Route path="/comarcas" element={<Navigate to="/obras" replace />} />
          <Route path="/estoque" element={<Pagina roles={ESTOQUE}><PainelEstoque /></Pagina>} />
          <Route path="/financeiro/lucratividade" element={<Pagina roles={ADMIN}><PainelFinanceiro /></Pagina>} />
          <Route path="/financeiro/faturamento" element={<Pagina roles={ADMIN}><GestaoFaturamento /></Pagina>} />
          <Route path="/logistica/viagens" element={<Pagina roles={ADMIN}><PainelViagensEReembolso /></Pagina>} />
          <Route path="/auditoria/tecnica" element={<Pagina roles={AUDITORIA}><AuditoriaMateriaisEAsBuilt /></Pagina>} />
          <Route path="/configuracao-notificacoes" element={<Pagina roles={GESTAO}><ConfiguracaoNotificacoes /></Pagina>} />
          <Route path="/tecnico" element={<Pagina roles={CAMPO} layout={false}><PortalTecnicoDashboard /></Pagina>} />
          <Route path="/tecnico/os/:id" element={<Pagina roles={CAMPO} layout={false}><ExecutarOrdemServico /></Pagina>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
