import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Contratos from './pages/Contratos'
import Projetos from './pages/Projetos'
import Funcionarios from './pages/Funcionarios'
import GestaoOrdensServico from './pages/GestaoOrdensServico'
import GestaoComarcas from './pages/GestaoComarcas'
import PainelEstoque from './pages/PainelEstoque'
import PortalTecnicoDashboard from './pages/PortalTecnicoDashboard'
import ExecutarOrdemServico from './pages/ExecutarOrdemServico'

//  NOVOS IMPORTS DA ETAPA 4
import PainelFinanceiro from './pages/PainelFinanceiro'
import GestaoFaturamento from './pages/GestaoFaturamento'
import PainelViagensEReembolso from './pages/PainelViagensEReembolso'
import AuditoriaMateriaisEAsBuilt from './pages/AuditoriaMateriaisEAsBuilt'

function App() {
  const userName = 'Paulo Morais' // TODO: Get from session/JWT

  return (
    <Router future={{ v7_relativeSplatPath: true, v7_startTransition: true }}>
      <Routes>
        <Route
          path="/"
          element={
            <Layout userName={userName}>
              <Dashboard />
            </Layout>
          }
        />
        <Route
          path="/contratos"
          element={
            <Layout userName={userName}>
              <Contratos />
            </Layout>
          }
        />
        <Route
          path="/projetos"
          element={
            <Layout userName={userName}>
              <Projetos />
            </Layout>
          }
        />
        <Route
          path="/funcionarios"
          element={
            <Layout userName={userName}>
              <Funcionarios />
            </Layout>
          }
        />
        <Route
          path="/ordens-servico"
          element={
            <Layout userName={userName}>
              <GestaoOrdensServico />
            </Layout>
          }
        />
        <Route
          path="/comarcas"
          element={
            <Layout userName={userName}>
              <GestaoComarcas />
            </Layout>
          }
        />
        <Route
          path="/estoque"
          element={
            <Layout userName={userName}>
              <PainelEstoque />
            </Layout>
          }
        />

        {/* NOVAS ROTAS DA ETAPA 4 (DENTRO DO LAYOUT GERENCIAL) */}
        <Route
          path="/financeiro/lucratividade"
          element={
            <Layout userName={userName}>
              <PainelFinanceiro />
            </Layout>
          }
        />
        <Route
          path="/financeiro/faturamento"
          element={
            <Layout userName={userName}>
              <GestaoFaturamento />
            </Layout>
          }
        />
        <Route
          path="/logistica/viagens"
          element={
            <Layout userName={userName}>
              <PainelViagensEReembolso />
            </Layout>
          }
        />
        <Route
          path="/auditoria/tecnica"
          element={
            <Layout userName={userName}>
              <AuditoriaMateriaisEAsBuilt />
            </Layout>
          }
        />

        {/* ROTAS DO PORTAL DO TÉCNICO (FORA DO LAYOUT PADRÃO) */}
        <Route
          path="/tecnico"
          element={<PortalTecnicoDashboard />}
        />
        <Route
          path="/tecnico/os/:id"
          element={<ExecutarOrdemServico />}
        />
      </Routes>
    </Router>
  )
}

export default App