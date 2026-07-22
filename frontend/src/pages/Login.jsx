import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { ChevronDown, LogIn, ShieldCheck, User } from "lucide-react";
import rcLogo from "../assets/rclogo.jpg";
import { useAuth, homePorPerfil } from "../contexts/AuthContext";
import api, { getApiErrorMessage } from "../services/api";
import { API_ORIGIN } from "../services/runtimeConfig";

export default function Login() {
  const { usuario, carregando, configuracao, login, loginDesenvolvimento } = useAuth();
  const [credenciais, setCredenciais] = useState({ cpf: "", senha: "" });
  const [usuariosDev, setUsuariosDev] = useState([]);
  const [funcionarioId, setFuncionarioId] = useState("");
  const [devAberto, setDevAberto] = useState(false);
  const [entrando, setEntrando] = useState(false);
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const codigo = new URLSearchParams(location.search).get("error");
    const senhaAlterada = new URLSearchParams(location.search).get("senhaAlterada");
    const motivoSessao = sessionStorage.getItem("auth:reason");
    sessionStorage.removeItem("auth:reason");
    if (codigo === "conta_nao_vinculada") setErro("Esta conta Zoho não está vinculada a um funcionário ativo. O administrador deve cadastrar o mesmo e-mail em Equipes.");
    if (codigo === "oauth") setErro("A Zoho não concluiu a autenticação. Verifique a configuração OAuth e tente novamente.");
    if (codigo === "sessao" || motivoSessao === "sessao") setErro("Sua sessão expirou após a reinicialização do servidor. Entre novamente para continuar.");
    if (senhaAlterada === "1") setMensagem("Senha alterada e confirmada no banco. Entre com a nova senha.");
  }, [location.search]);

  useEffect(() => {
    if (!configuracao.devLoginEnabled) return;
    api.get("/auth/dev-usuarios").then(({ data }) => { setUsuariosDev(data); if (data.length) setFuncionarioId(String(data[0].id)); }).catch(() => {});
  }, [configuracao.devLoginEnabled]);

  if (!carregando && usuario) return <Navigate to={homePorPerfil(usuario.perfil)} replace />;

  const concluir = (usuarioLogado) => navigate(location.state?.from || homePorPerfil(usuarioLogado.perfil), { replace: true });
  const entrarLocal = async (event) => {
    event.preventDefault(); setEntrando(true); setErro("");
    try { concluir(await login(credenciais.cpf, credenciais.senha)); }
    catch (error) { setErro(getApiErrorMessage(error, "CPF ou senha inválidos.")); }
    finally { setEntrando(false); }
  };
  const entrarDev = async () => {
    setEntrando(true); setErro("");
    try { concluir(await loginDesenvolvimento(Number(funcionarioId))); }
    catch (error) { setErro(getApiErrorMessage(error)); }
    finally { setEntrando(false); }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-950 p-4">
      <section className="w-full max-w-md rounded border border-slate-800 bg-white p-6 shadow-xl">
        <div className="mb-6 flex items-center gap-3 border-b border-slate-200 pb-5"><img src={rcLogo} alt="RC Technology" className="h-12 w-12 rounded object-cover" /><div><h1 className="text-lg font-bold text-slate-950">RC Operations Hub</h1><p className="text-sm text-slate-500">Acesso ao ambiente operacional</p></div></div>
        {erro && <p className="mb-4 rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700">{erro}</p>}
        {mensagem && <p className="mb-4 rounded border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{mensagem}</p>}
        <form onSubmit={entrarLocal} className="space-y-4">
          <label className="block text-sm font-semibold text-slate-700">CPF<input value={credenciais.cpf} onChange={(e) => setCredenciais({ ...credenciais, cpf: e.target.value })} inputMode="numeric" autoComplete="username" required placeholder="000.000.000-00" className="mt-1 w-full rounded border border-slate-300 px-3 py-2.5 text-sm" /></label>
          <label className="block text-sm font-semibold text-slate-700">Senha<input value={credenciais.senha} onChange={(e) => setCredenciais({ ...credenciais, senha: e.target.value })} type="password" autoComplete="current-password" required className="mt-1 w-full rounded border border-slate-300 px-3 py-2.5 text-sm" /></label>
          <button disabled={entrando} className="flex w-full items-center justify-center gap-2 rounded bg-blue-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-blue-700 disabled:opacity-50"><LogIn size={18} /> {entrando ? "Entrando..." : "Entrar com CPF"}</button>
        </form>
        {configuracao.zohoEnabled && <div className="mt-4"><a href={`${API_ORIGIN}/oauth2/authorization/zoho`} className="flex w-full items-center justify-center gap-2 rounded border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-700 hover:bg-slate-50"><ShieldCheck size={18} /> Entrar com Zoho</a></div>}
        {configuracao.devLoginEnabled && <div className="mt-5 border-t border-slate-200 pt-4"><button type="button" onClick={() => setDevAberto(!devAberto)} className="flex w-full items-center justify-between text-sm font-semibold text-slate-600"><span className="flex items-center gap-2"><User size={17} /> Ambiente de teste</span><ChevronDown size={16} /></button>{devAberto && <div className="mt-3 space-y-3"><select value={funcionarioId} onChange={(e) => setFuncionarioId(e.target.value)} className="w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm">{usuariosDev.map((item) => <option key={item.id} value={item.id}>{item.nome} - {item.perfil.replaceAll("_", " ")}</option>)}</select><button type="button" onClick={entrarDev} disabled={!funcionarioId || entrando} className="w-full rounded border border-blue-300 px-3 py-2 text-sm font-bold text-blue-700 disabled:opacity-50">Entrar como usuário selecionado</button></div>}</div>}
      </section>
    </main>
  );
}
