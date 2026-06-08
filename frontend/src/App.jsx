import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Contratos from './pages/Contratos'
import Projetos from './pages/Projetos'
import Funcionarios from './pages/Funcionarios'

function App() {
  const userName = 'Paulo Silva' // TODO: Get from session/JWT

  return (
    <Router>
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
      </Routes>
    </Router>
  )
}

export default App
