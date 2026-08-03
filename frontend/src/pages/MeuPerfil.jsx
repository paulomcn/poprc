import { useState } from "react";
import { KeyRound, ShieldCheck, User } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { getApiErrorMessage } from "../services/api";

const nomePerfil = { ADMIN: "Administrador", SUPERVISOR_TECNICO: "Supervisor técnico", TECNICO: "Técnico", ESTOQUE: "Estoque", AUDITOR: "Auditor" };

export default function MeuPerfil() {
  const { usuario, configuracao, alterarSenha, logout } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ atual: "", nova: "", confirmacao: "" });
  const [mensagem, setMensagem] = useState("");
  const [erro, setErro] = useState("");
  const [salvando, setSalvando] = useState(false);

  const salvar = async (event) => {
    event.preventDefault();
    setErro("");
    setMensagem("");
    if (form.nova !== form.confirmacao) return setErro("A confirmação não corresponde à nova senha.");
    setSalvando(true);
    try {
      await alterarSenha(form.atual, form.nova);
      setForm({ atual: "", nova: "", confirmacao: "" });
      await logout();
      navigate("/login?senhaAlterada=1", { replace: true });
    } catch (error) {
      setErro(getApiErrorMessage(error));
    } finally {
      setSalvando(false);
    }
  };

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div><h1 className="text-2xl font-bold text-slate-900">Meu perfil</h1><p className="mt-1 text-sm text-slate-500">Dados da sua conta e credenciais de acesso.</p></div>
      {usuario?.trocaSenhaObrigatoria && <div className="rounded border border-amber-300 bg-amber-50 p-3 text-sm font-semibold text-amber-900">Defina uma nova senha antes de continuar no sistema.</div>}
      <section className="rounded border border-slate-200 bg-white">
        <div className="flex items-center gap-3 border-b border-slate-200 p-5"><User className="text-blue-600" /><div><h2 className="font-bold text-slate-900">{usuario?.nome}</h2><p className="text-sm text-slate-500">{nomePerfil[usuario?.perfil] || usuario?.perfil}</p></div></div>
        <dl className="grid gap-px bg-slate-200 sm:grid-cols-2">
          {[['CPF', usuario?.cpfMascarado], ['Telefone', usuario?.telefone], ['E-mail Zoho', usuario?.email], ['Função', usuario?.funcao], ['Cidade', usuario?.cidade], ['Método de acesso', usuario?.metodoAutenticacao]].map(([rotulo, valor]) => <div key={rotulo} className="bg-white p-4"><dt className="text-xs font-bold uppercase text-slate-400">{rotulo}</dt><dd className="mt-1 text-sm font-medium text-slate-800">{valor || "Não informado"}</dd></div>)}
        </dl>
      </section>
      {configuracao.securityEnabled ? <section className="rounded border border-slate-200 bg-white p-5">
        <div className="mb-4 flex items-center gap-2"><KeyRound size={19} className="text-blue-600" /><h2 className="font-bold text-slate-900">Alterar senha local</h2></div>
        <form onSubmit={salvar} className="grid gap-4 sm:grid-cols-3">
          {!usuario?.trocaSenhaObrigatoria && <input type="password" required value={form.atual} onChange={(e) => setForm({ ...form, atual: e.target.value })} placeholder="Senha atual" className="rounded border border-slate-300 px-3 py-2 text-sm" />}
          <input type="password" required minLength={8} value={form.nova} onChange={(e) => setForm({ ...form, nova: e.target.value })} placeholder="Nova senha" className="rounded border border-slate-300 px-3 py-2 text-sm" />
          <input type="password" required minLength={8} value={form.confirmacao} onChange={(e) => setForm({ ...form, confirmacao: e.target.value })} placeholder="Confirmar nova senha" className="rounded border border-slate-300 px-3 py-2 text-sm" />
          <div className="sm:col-span-3"><p className="mb-3 text-xs text-slate-500">Use ao menos 8 caracteres, com letras e números.</p>{erro && <p className="mb-3 text-sm text-red-700">{erro}</p>}{mensagem && <p className="mb-3 text-sm text-emerald-700">{mensagem}</p>}<button disabled={salvando} className="inline-flex items-center gap-2 rounded bg-blue-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-50"><ShieldCheck size={17} /> {salvando ? "Salvando..." : "Salvar nova senha"}</button></div>
        </form>
      </section> : <section className="rounded border border-blue-200 bg-blue-50 p-5 text-sm text-blue-900">A autenticação está desativada neste ambiente de desenvolvimento.</section>}
    </div>
  );
}
