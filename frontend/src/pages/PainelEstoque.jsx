import { useState, useEffect } from "react";
import {
  Plus,
  Minus,
  Package,
  AlertCircle,
  History,
  ArrowUpRight,
  ArrowDownLeft,
  FolderPlus,
  Search,
} from "lucide-react";
import api from "../services/api";
import Modal from "../components/Modal";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";

export default function PainelEstoque() {
  const [materiais, setMateriais] = useState([]);
  const [funcionarios, setFuncionarios] = useState([]);
  const [comarcas, setComarcas] = useState([]);
  const [historico, setHistorico] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [historicoFiltro, setHistoricoFiltro] = useState("");
  const [historicoTipoFiltro, setHistoricoTipoFiltro] = useState("");
  const [historicoComarcaFiltro, setHistoricoComarcaFiltro] = useState("");
  const [historicoProjetoFiltro, setHistoricoProjetoFiltro] = useState("");

  // Modais de controle
  const [showEntradaModal, setShowEntradaModal] = useState(false);
  const [showSaidaModal, setShowSaidaModal] = useState(false);
  const [showNovoMaterialModal, setShowNovoMaterialModal] = useState(false); //   NOVO MODAL

  // Formulários
  const [formData, setFormData] = useState({
    materialId: "",
    quantidade: "",
    funcionarioId: "",
    comarcaId: "",
  });

  const [novoMaterialData, setNovoMaterialData] = useState({
    //   NOVO FORMULÁRIO
    nome: "",
    partNumber: "",
    fabricante: "",
    fornecedor: "",
    localizacao: "",
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const materiaisResponse = await api.get("/estoque/materiais");
      setMateriais(materiaisResponse.data);

      const funcionariosResponse = await api.get("/funcionarios");
      setFuncionarios(funcionariosResponse.data);

      const comarcasResponse = await api.get("/comarcas");
      setComarcas(comarcasResponse.data);

      const historicoResponse = await api.get("/estoque/historico");
      setHistorico(historicoResponse.data);

      setError(null);
    } catch (err) {
      setError("Erro ao carregar dados do estoque");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleCloseModal = () => {
    setShowEntradaModal(false);
    setShowSaidaModal(false);
    setShowNovoMaterialModal(false);
    setFormData({ materialId: "", quantidade: "", funcionarioId: "", comarcaId: "" });
    setNovoMaterialData({
      nome: "",
      partNumber: "",
      fabricante: "",
      fornecedor: "",
      localizacao: "",
    });
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNovoMaterialChange = (e) => {
    const { name, value } = e.target;
    setNovoMaterialData((prev) => ({ ...prev, [name]: value }));
  };

  //   SUBMIT DO PRODUTO NOVO (CATÁLOGO)
  const handleSubmitNovoMaterial = async (e) => {
    e.preventDefault();
    try {
      await api.post("/estoque/materiais", novoMaterialData);
      setSuccessMessage("Novo material cadastrado no catálogo com sucesso!");
      setTimeout(() => setSuccessMessage(null), 4000);
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(
        err.response?.data?.message || "Erro ao cadastrar novo material",
      );
      console.error(err);
    }
  };

  const handleSubmitEntrada = async (e) => {
    e.preventDefault();
    try {
      await api.post("/estoque/entrada", {
        materialId: parseInt(formData.materialId),
        quantidade: parseInt(formData.quantidade),
        funcionarioId: parseInt(formData.funcionarioId),
      });
      setSuccessMessage("Entrada de material registrada com sucesso!");
      setTimeout(() => setSuccessMessage(null), 4000);
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || "Erro ao registrar entrada");
      console.error(err);
    }
  };

  const handleSubmitSaida = async (e) => {
    e.preventDefault();
    try {
      await api.post("/estoque/saida", {
        materialId: parseInt(formData.materialId),
        quantidade: parseInt(formData.quantidade),
        funcionarioId: parseInt(formData.funcionarioId),
        comarcaId: parseInt(formData.comarcaId),
      });
      setSuccessMessage("Saída de material registrada com sucesso!");
      setTimeout(() => setSuccessMessage(null), 4000);
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || "Erro ao registrar saída");
      console.error(err);
    }
  };

  const getReservado = (material) => material.quantidadeReservada ?? 0;
  const getLivre = (material) =>
    Math.max(0, (material.quantidadeDisponivel ?? 0) - getReservado(material));
  const isCriticalStock = (material) => getLivre(material) <= 5;
  const historicoFiltrado = historico.filter((mov) => {
    const termo = historicoFiltro.trim().toLowerCase();
    const correspondeTipo = !historicoTipoFiltro || mov.tipo === historicoTipoFiltro;
    const correspondeComarca =
      !historicoComarcaFiltro ||
      String(mov.comarca?.id || "") === String(historicoComarcaFiltro);
    const correspondeProjeto =
      !historicoProjetoFiltro ||
      String(mov.projeto?.id || "") === String(historicoProjetoFiltro);
    const correspondeTexto =
      !termo ||
      [
      mov.tipo,
      mov.material?.nome,
      mov.material?.partNumber,
      mov.funcionario?.nome,
      mov.projeto?.id ? `Projeto ${mov.projeto.id}` : null,
      mov.ordemServico?.numeroOs,
      mov.comarca?.nomeComarca,
      mov.observacao,
      ]
        .filter(Boolean)
        .some((valor) => String(valor).toLowerCase().includes(termo));

    return correspondeTipo && correspondeComarca && correspondeProjeto && correspondeTexto;
  });
  const tiposHistorico = [...new Set(historico.map((mov) => mov.tipo).filter(Boolean))];
  const projetosHistorico = [
    ...new Map(
      historico
        .filter((mov) => mov.projeto?.id)
        .map((mov) => [mov.projeto.id, mov.projeto]),
    ).values(),
  ];

  const getMovimentacaoStyle = (tipo) => {
    const styles = {
      ENTRADA: {
        label: "Entrada",
        icon: ArrowDownLeft,
        className: "text-green-600 bg-green-50",
      },
      SAIDA: {
        label: "Saída",
        icon: ArrowUpRight,
        className: "text-blue-600 bg-blue-50",
      },
      RESERVA: {
        label: "Reserva",
        icon: Package,
        className: "text-amber-700 bg-amber-50",
      },
      ESTORNO_RESERVA: {
        label: "Estorno Reserva",
        icon: ArrowDownLeft,
        className: "text-slate-600 bg-slate-100",
      },
      BAIXA: {
        label: "Baixa OS",
        icon: ArrowUpRight,
        className: "text-rose-600 bg-rose-50",
      },
      ESTORNO_BAIXA: {
        label: "Estorno Baixa",
        icon: ArrowDownLeft,
        className: "text-purple-700 bg-purple-50",
      },
    };
    return styles[tipo] || {
      label: tipo || "Movimento",
      icon: History,
      className: "text-slate-600 bg-slate-100",
    };
  };

  const getComarcaOptionLabel = (comarca) => {
    const numeroOs = comarca.ordemServico?.numeroOs || "OS não vinculada";
    const projeto = comarca.projeto?.id ? `Projeto #${comarca.projeto.id}` : "Projeto não vinculado";
    return `${numeroOs} - ${comarca.nomeComarca} - ${projeto}`;
  };

  const getReferenciaOperacional = (mov) => {
    const partes = [];
    if (mov.ordemServico?.numeroOs) partes.push(mov.ordemServico.numeroOs);
    if (mov.comarca?.nomeComarca) partes.push(mov.comarca.nomeComarca);
    if (mov.projeto?.id) partes.push(`Projeto #${mov.projeto.id}`);
    return partes;
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <div className="flex justify-between items-start mb-6">
          <div>
            <h1 className="text-3xl font-bold text-slate-800">
              Estoque de Materiais
            </h1>
            <p className="text-slate-600 mt-2">
              Gerenciamento de entrada e saída de materiais
            </p>
          </div>
          <div className="flex gap-3">
            {/*   BOTAO NOVO: Cadastrar no Catálogo */}
            <button
              onClick={() => setShowNovoMaterialModal(true)}
              className="flex items-center gap-2 bg-slate-700 hover:bg-slate-800 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              <FolderPlus size={20} /> + Novo Material
            </button>
            <button
              onClick={() => setShowEntradaModal(true)}
              className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              <Plus size={20} /> Entrada de Material
            </button>
            <button
              onClick={() => setShowSaidaModal(true)}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              <Minus size={20} /> Saída de Material
            </button>
          </div>
        </div>
        {error && <Alert type="error" message={error} />}
        {successMessage && <Alert type="success" message={successMessage} />}
      </div>

      {/* Tabela de Saldo Atual */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-slate-200">
        <table className="w-full">
          <thead className="bg-slate-50 border-b border-slate-200">
            <tr>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">
                Nome do Material
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">
                Part Number
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">
                Fabricante
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">
                Localização
              </th>
              <th className="px-6 py-4 text-center text-sm font-semibold text-slate-700">
                Disponível
              </th>
              <th className="px-6 py-4 text-center text-sm font-semibold text-slate-700">
                Reservado
              </th>
              <th className="px-6 py-4 text-center text-sm font-semibold text-slate-700">
                Livre
              </th>
            </tr>
          </thead>
          <tbody>
            {materiais.map((material) => (
              <tr
                key={material.id}
                className={`border-b border-slate-200 hover:bg-slate-50 transition-colors ${isCriticalStock(material) ? "bg-red-50/50" : ""}`}
              >
                <td className="px-6 py-4 text-sm text-slate-800 flex items-center gap-2">
                  <Package size={16} className="text-slate-400" />{" "}
                  {material.nome}
                </td>
                <td className="px-6 py-4 text-sm font-mono text-slate-800">
                  {material.partNumber}
                </td>
                <td className="px-6 py-4 text-sm text-slate-800">
                  {material.fabricante}
                </td>
                <td className="px-6 py-4 text-sm text-slate-600">
                  {material.localizacao || "Não informada"}
                </td>
                <td className="px-6 py-4 text-center">
                  <span
                    className="inline-flex items-center justify-center min-w-[3rem] py-1 px-3 rounded-full font-semibold text-sm bg-slate-100 text-slate-800"
                  >
                    {material.quantidadeDisponivel ?? 0}
                  </span>
                </td>
                <td className="px-6 py-4 text-center">
                  <span className="inline-flex items-center justify-center min-w-[3rem] py-1 px-3 rounded-full font-semibold text-sm bg-amber-100 text-amber-800">
                    {getReservado(material)}
                  </span>
                </td>
                <td className="px-6 py-4 text-center">
                  <span
                    className={`inline-flex items-center justify-center min-w-[3rem] py-1 px-3 rounded-full font-semibold text-sm ${isCriticalStock(material) ? "bg-red-200 text-red-800" : "bg-green-100 text-green-800"}`}
                  >
                    {isCriticalStock(material) && (
                      <AlertCircle size={14} className="mr-1" />
                    )}
                    {getLivre(material)}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Histórico */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-slate-200">
        <div className="p-5 border-b border-slate-200 bg-slate-50/50 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div className="flex items-center gap-2">
            <History size={18} className="text-slate-500" />
            <h2 className="font-bold text-slate-800">
              Histórico de Movimentações
            </h2>
          </div>
          <div className="grid w-full gap-2 md:w-auto md:grid-cols-[220px_160px_220px_160px]">
            <div className="relative">
              <Search className="absolute left-3 top-2.5 text-slate-400 w-4 h-4" />
              <input
                type="text"
                value={historicoFiltro}
                onChange={(e) => setHistoricoFiltro(e.target.value)}
                placeholder="Material, OS, comarca..."
                className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <select
              value={historicoTipoFiltro}
              onChange={(e) => setHistoricoTipoFiltro(e.target.value)}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Todos os tipos</option>
              {tiposHistorico.map((tipo) => (
                <option key={tipo} value={tipo}>
                  {getMovimentacaoStyle(tipo).label}
                </option>
              ))}
            </select>
            <select
              value={historicoComarcaFiltro}
              onChange={(e) => setHistoricoComarcaFiltro(e.target.value)}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Todas as OS/comarcas</option>
              {comarcas.map((comarca) => (
                <option key={comarca.id} value={comarca.id}>
                  {getComarcaOptionLabel(comarca)}
                </option>
              ))}
            </select>
            <select
              value={historicoProjetoFiltro}
              onChange={(e) => setHistoricoProjetoFiltro(e.target.value)}
              className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Todos os projetos</option>
              {projetosHistorico.map((projeto) => (
                <option key={projeto.id} value={projeto.id}>
                  Projeto #{projeto.id}
                </option>
              ))}
            </select>
          </div>
        </div>
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-600 border-b border-slate-200 text-xs font-bold uppercase">
            <tr>
              <th className="px-6 py-4">Tipo</th>
              <th className="px-6 py-4">Material</th>
              <th className="px-6 py-4 text-center">Qtd.</th>
              <th className="px-6 py-4">Responsável</th>
              <th className="px-6 py-4">Referência</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {historicoFiltrado.map((mov) => {
              const style = getMovimentacaoStyle(mov.tipo);
              const Icon = style.icon;
              const referencias = getReferenciaOperacional(mov);

              return (
                <tr
                  key={mov.id}
                  className="hover:bg-slate-50/50 transition-colors"
                >
                  <td className="px-6 py-4">
                    <span
                      className={`inline-flex items-center gap-1 font-bold px-2 py-0.5 rounded text-xs ${style.className}`}
                    >
                      <Icon size={14} /> {style.label}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-slate-700 font-medium">
                    {mov.material?.nome || "Insumo"}
                    {mov.material?.partNumber && (
                      <span className="block text-[10px] font-mono text-slate-400">
                        {mov.material.partNumber}
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-center font-semibold text-slate-800">
                    {mov.quantidade}
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    {mov.funcionario?.nome || "Sistema"}
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-500 max-w-sm">
                    {referencias.length > 0 && (
                      <div className="mb-1 flex flex-wrap gap-1">
                        {referencias.map((referencia) => (
                          <span
                            key={referencia}
                            className="rounded bg-slate-100 px-2 py-0.5 font-semibold text-slate-700"
                          >
                            {referencia}
                          </span>
                        ))}
                      </div>
                    )}
                    <span>{mov.observacao || "Movimentação manual"}</span>
                  </td>
                </tr>
              );
            })}
            {historicoFiltrado.length === 0 && (
              <tr>
                <td
                  colSpan="5"
                  className="px-6 py-8 text-center text-slate-400"
                >
                  Nenhuma movimentação encontrada para o filtro informado.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/*   MODAL NOVO: Cadastro de Material do Catálogo */}
      <Modal
        isOpen={showNovoMaterialModal}
        onClose={handleCloseModal}
        title="Cadastrar Novo Material no Catálogo"
      >
        <form onSubmit={handleSubmitNovoMaterial} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Nome do Material
            </label>
            <input
              type="text"
              name="nome"
              value={novoMaterialData.nome}
              onChange={handleNovoMaterialChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-500 focus:outline-none"
              placeholder="Ex: Roteador Wireless Mikrotik"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Part Number
              </label>
              <input
                type="text"
                name="partNumber"
                value={novoMaterialData.partNumber}
                onChange={handleNovoMaterialChange}
                required
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-500 focus:outline-none"
                placeholder="Ex: PN-MK-951"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Fabricante
              </label>
              <input
                type="text"
                name="fabricante"
                value={novoMaterialData.fabricante}
                onChange={handleNovoMaterialChange}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-500 focus:outline-none"
                placeholder="Ex: Mikrotik"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Fornecedor
              </label>
              <input
                type="text"
                name="fornecedor"
                value={novoMaterialData.fornecedor}
                onChange={handleNovoMaterialChange}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-500 focus:outline-none"
                placeholder="Ex: WDC Networks"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Localização no Estoque
              </label>
              <input
                type="text"
                name="localizacao"
                value={novoMaterialData.localizacao}
                onChange={handleNovoMaterialChange}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-slate-500 focus:outline-none"
                placeholder="Ex: Armário C - Gaveta 3"
              />
            </div>
          </div>
          <div className="flex gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={handleCloseModal}
              className="px-6 py-2 border rounded-lg text-slate-700 hover:bg-slate-50"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-slate-700 hover:bg-slate-800 text-white font-semibold rounded-lg"
            >
              Cadastrar Produto
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Entrada */}
      <Modal
        isOpen={showEntradaModal}
        onClose={handleCloseModal}
        title="Entrada de Material"
      >
        <form onSubmit={handleSubmitEntrada} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Selecionar Material
            </label>
            <select
              name="materialId"
              value={formData.materialId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg"
            >
              <option value="">-- Selecione um material --</option>
              {materiais.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.nome} ({m.partNumber})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Quantidade
            </label>
            <input
              type="number"
              name="quantidade"
              value={formData.quantidade}
              onChange={handleInputChange}
              min="1"
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg"
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Funcionário Responsável
            </label>
            <select
              name="funcionarioId"
              value={formData.funcionarioId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg"
            >
              <option value="">-- Selecione um funcionário --</option>
              {funcionarios.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="flex gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={handleCloseModal}
              className="px-6 py-2 border rounded-lg"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-green-600 text-white font-semibold rounded-lg"
            >
              Registrar Entrada
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal Saída */}
      <Modal
        isOpen={showSaidaModal}
        onClose={handleCloseModal}
        title="Saída de Material"
      >
        <form onSubmit={handleSubmitSaida} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Selecionar Material
            </label>
            <select
              name="materialId"
              value={formData.materialId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg"
            >
              <option value="">-- Selecione um material --</option>
              {materiais.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.nome} - Livre: {getLivre(m)}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Quantidade
            </label>
            <input
              type="number"
              name="quantidade"
              value={formData.quantidade}
              onChange={handleInputChange}
              min="1"
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg"
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Funcionário Responsável
            </label>
            <select
              name="funcionarioId"
              value={formData.funcionarioId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg"
            >
              <option value="">-- Selecione um funcionário --</option>
              {funcionarios.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nome}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Destino Operacional
            </label>
            <select
              name="comarcaId"
              value={formData.comarcaId}
              onChange={handleInputChange}
              required
              className="w-full px-4 py-2 border border-slate-300 rounded-lg"
            >
              <option value="">-- Selecione a OS / Comarca --</option>
              {comarcas.map((comarca) => (
                <option key={comarca.id} value={comarca.id}>
                  {getComarcaOptionLabel(comarca)}
                </option>
              ))}
            </select>
          </div>
          <div className="flex gap-3 justify-end pt-4">
            <button
              type="button"
              onClick={handleCloseModal}
              className="px-6 py-2 border rounded-lg"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-6 py-2 bg-blue-600 text-white font-semibold rounded-lg"
            >
              Registrar Saída
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
