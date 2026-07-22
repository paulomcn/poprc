import { useEffect, useState } from "react";
import { KeyRound, Pencil, Plus, Search, Users } from "lucide-react";
import Alert from "../components/Alert";
import LoadingSpinner from "../components/LoadingSpinner";
import Modal from "../components/Modal";
import api, { getApiErrorMessage } from "../services/api";

const perfis = [
  ["ADMIN", "Administrador"], ["SUPERVISOR_TECNICO", "Supervisor técnico"],
  ["TECNICO", "Técnico"], ["ESTOQUE", "Estoque"], ["AUDITOR", "Auditor"],
];
const funcoes = ["Administrador", "Supervisor técnico", "Líder de equipe", "Técnico de campo", "Técnico de redes", "Almoxarife", "Auditor", "Financeiro"];
const vazio = { nome: "", funcao: "Técnico de campo", cidade: "", cpf: "", telefone: "", email: "", perfilAcesso: "TECNICO", ativo: true, senha: "", certificacoes: "", documentPaths: "" };

export default function Funcionarios() {
  const [funcionarios, setFuncionarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busca, setBusca] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(vazio);
  const [salvando, setSalvando] = useState(false);

  const carregar = async () => {
    setLoading(true);
    try { setFuncionarios((await api.get("/funcionarios")).data); setError(""); }
    catch (err) { setError(getApiErrorMessage(err, "Erro ao carregar funcionários.")); }
    finally { setLoading(false); }
  };
  useEffect(() => { carregar(); }, []);

  const abrirNovo = () => { setEditando(null); setForm(vazio); setShowModal(true); };
  const abrirEdicao = (item) => {
    setEditando(item);
    setForm({ nome: item.nome || "", funcao: item.funcao || "Técnico de campo", cidade: item.cidade || "", cpf: "", telefone: item.telefone || "", email: item.email || "", perfilAcesso: item.perfilAcesso || "TECNICO", ativo: item.ativo !== false, senha: "", certificacoes: item.certificacoes?.join(", ") || "", documentPaths: item.documentPaths?.join(", ") || "" });
    setShowModal(true);
  };
  const lista = (texto) => texto ? texto.split(",").map((item) => item.trim()).filter(Boolean) : [];
  const salvar = async (event) => {
    event.preventDefault(); setSalvando(true); setError(""); setSuccess("");
    const payload = { ...form, cpf: form.cpf || null, email: form.email || null, senha: form.senha || null, certificacoes: lista(form.certificacoes), documentPaths: lista(form.documentPaths) };
    try {
      const response = editando
        ? await api.put(`/funcionarios/${editando.id}`, payload)
        : await api.post("/funcionarios", payload);
      const confirmado = response.data?.funcionario;
      if (form.cpf && !confirmado?.cpfMascarado) throw new Error("O servidor não confirmou a gravação do CPF.");
      if (form.senha && !confirmado?.senhaConfigurada) throw new Error("O servidor não confirmou a gravação da senha.");
      setShowModal(false); await carregar();
      setSuccess(editando ? "Funcionário atualizado e confirmado no banco." : "Funcionário cadastrado e confirmado no banco.");
    } catch (err) { setError(getApiErrorMessage(err, err.message || "Erro ao salvar funcionário.")); }
    finally { setSalvando(false); }
  };
  const redefinir = async (item) => {
    const senha = window.prompt(`Defina uma senha temporária para ${item.nome}:`);
    if (!senha) return;
    try { const response = await api.post(`/funcionarios/${item.id}/redefinir-senha`, { senhaTemporaria: senha }); if (!response.data?.funcionario?.senhaConfigurada) throw new Error("O servidor não confirmou a gravação da senha."); await carregar(); setSuccess(`Senha temporária de ${item.nome} gravada com sucesso.`); }
    catch (err) { setError(getApiErrorMessage(err, "Não foi possível redefinir a senha.")); }
  };
  const filtrados = funcionarios.filter((item) => [item.nome, item.funcao, item.cidade, item.cpfMascarado, item.telefone].some((valor) => String(valor || "").toLowerCase().includes(busca.toLowerCase())));

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3"><div><h1 className="text-2xl font-bold text-slate-900">Equipes e acessos</h1><p className="mt-1 text-sm text-slate-500">Colaboradores, funções operacionais e permissões do sistema.</p></div><button onClick={abrirNovo} className="flex items-center gap-2 rounded bg-blue-600 px-4 py-2 text-sm font-bold text-white"><Plus size={18} /> Novo funcionário</button></div>
      {error && <Alert type="error" message={error} onClose={() => setError("")} />}
      {success && <Alert type="success" message={success} onClose={() => setSuccess("")} />}
      <div className="flex items-center gap-2 rounded border border-slate-200 bg-white px-3 py-2"><Search size={17} className="text-slate-400" /><input value={busca} onChange={(e) => setBusca(e.target.value)} className="w-full border-0 text-sm outline-none" placeholder="Filtrar por nome, função, CPF, telefone ou cidade" /></div>
      {loading ? <LoadingSpinner /> : filtrados.length === 0 ? <div className="rounded border border-slate-200 bg-white p-8 text-center"><Users className="mx-auto text-slate-400" /><p className="mt-3 text-sm text-slate-500">Nenhum funcionário encontrado.</p></div> : (
        <div className="overflow-x-auto rounded border border-slate-200 bg-white"><table className="w-full min-w-[900px] text-sm"><thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500"><tr>{["Funcionário", "Contato", "Função", "Perfil", "Login", "Status", "Ações"].map((item) => <th key={item} className="px-4 py-3">{item}</th>)}</tr></thead><tbody className="divide-y divide-slate-100">{filtrados.map((item) => <tr key={item.id} className="hover:bg-slate-50"><td className="px-4 py-3"><p className="font-semibold text-slate-900">{item.nome}</p><p className="text-xs text-slate-500">{item.cidade || "Cidade não informada"}</p></td><td className="px-4 py-3 text-slate-600"><p>{item.cpfMascarado || "CPF não informado"}</p><p className="text-xs">{item.telefone || item.email || "Sem contato"}</p></td><td className="px-4 py-3 text-slate-700">{item.funcao}</td><td className="px-4 py-3 text-slate-700">{perfis.find(([valor]) => valor === item.perfilAcesso)?.[1] || item.perfilAcesso}</td><td className="px-4 py-3"><span className={`rounded px-2 py-1 text-xs font-semibold ${item.senhaConfigurada ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-800"}`}>{item.senhaConfigurada ? "Senha configurada" : "Sem senha"}</span></td><td className="px-4 py-3"><span className={`rounded px-2 py-1 text-xs font-semibold ${item.ativo === false ? "bg-slate-100 text-slate-600" : "bg-blue-50 text-blue-700"}`}>{item.ativo === false ? "Inativo" : "Ativo"}</span></td><td className="px-4 py-3"><div className="flex gap-1"><button title="Editar" onClick={() => abrirEdicao(item)} className="rounded border border-slate-200 p-2 text-blue-700 hover:bg-blue-50"><Pencil size={16} /></button><button title="Redefinir senha" onClick={() => redefinir(item)} disabled={!item.cpfMascarado} className="rounded border border-slate-200 p-2 text-slate-700 hover:bg-slate-100 disabled:opacity-30"><KeyRound size={16} /></button></div></td></tr>)}</tbody></table></div>
      )}
      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title={editando ? "Editar funcionário" : "Novo funcionário"}>
        <form onSubmit={salvar} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2"><Campo label="Nome *"><input required value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} className="campo" /></Campo><Campo label="Cidade *"><input required value={form.cidade} onChange={(e) => setForm({ ...form, cidade: e.target.value })} className="campo" /></Campo></div>
          <div className="grid gap-4 sm:grid-cols-2"><Campo label="Função operacional *"><input required list="funcoes-equipe" value={form.funcao} onChange={(e) => setForm({ ...form, funcao: e.target.value })} className="campo" /><datalist id="funcoes-equipe">{funcoes.map((item) => <option key={item} value={item} />)}</datalist></Campo><Campo label="Perfil de acesso *"><select value={form.perfilAcesso} onChange={(e) => setForm({ ...form, perfilAcesso: e.target.value })} className="campo bg-white">{perfis.map(([valor, label]) => <option key={valor} value={valor}>{label}</option>)}</select></Campo></div>
          <div className="grid gap-4 sm:grid-cols-2"><Campo label={editando?.cpfMascarado ? `CPF (atual: ${editando.cpfMascarado})` : "CPF *"}><input required={!editando} value={form.cpf} onChange={(e) => setForm({ ...form, cpf: e.target.value })} inputMode="numeric" placeholder={editando ? "Deixe vazio para manter" : "000.000.000-00"} className="campo" /></Campo><Campo label="Telefone"><input value={form.telefone} onChange={(e) => setForm({ ...form, telefone: e.target.value })} placeholder="(00) 00000-0000" className="campo" /></Campo></div>
          <Campo label="E-mail Zoho (opcional)"><input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} className="campo" /><p className="mt-1 text-xs text-slate-500">Necessário somente para entrar pela Zoho.</p></Campo>
          <Campo label={editando ? "Nova senha temporária (opcional)" : "Senha temporária *"}><input type="password" required={!editando} minLength={8} value={form.senha} onChange={(e) => setForm({ ...form, senha: e.target.value })} className="campo" /><p className="mt-1 text-xs text-slate-500">O funcionário deverá trocá-la no primeiro acesso. Use letras e números.</p></Campo>
          <label className="flex items-center gap-2 text-sm font-medium text-slate-700"><input type="checkbox" checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} /> Acesso ativo</label>
          <div className="flex justify-end gap-2 border-t border-slate-200 pt-4"><button type="button" onClick={() => setShowModal(false)} className="rounded border border-slate-300 px-4 py-2 text-sm font-semibold">Cancelar</button><button disabled={salvando} className="rounded bg-blue-600 px-4 py-2 text-sm font-bold text-white disabled:opacity-50">{salvando ? "Salvando..." : "Salvar"}</button></div>
        </form>
      </Modal>
    </div>
  );
}

function Campo({ label, children }) { return <label className="block text-sm font-semibold text-slate-700">{label}<div className="mt-1">{children}</div></label>; }
