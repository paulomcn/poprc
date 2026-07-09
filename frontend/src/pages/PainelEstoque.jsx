import { Fragment, useState, useEffect, useRef } from "react";
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
  Edit2,
} from "lucide-react";
import api from "../services/api";
import Modal from "../components/Modal";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";

const CATEGORIAS_MATERIAL = [
  { value: "MATERIAL_CONSUMO", label: "Materiais de Consumo" },
  { value: "FERRAMENTA", label: "Ferramentas" },
];

function SignatureBox({ label, value, onChange }) {
  const canvasRef = useRef(null);
  const drawingRef = useRef(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const ratio = window.devicePixelRatio || 1;
    canvas.width = rect.width * ratio;
    canvas.height = rect.height * ratio;
    const context = canvas.getContext("2d");
    context.scale(ratio, ratio);
    context.lineWidth = 2;
    context.lineCap = "round";
    context.lineJoin = "round";
    context.strokeStyle = "#0f172a";
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, rect.width, rect.height);

    if (value?.startsWith("data:image")) {
      const image = new Image();
      image.onload = () => {
        context.drawImage(image, 0, 0, rect.width, rect.height);
      };
      image.src = value;
    }
  }, [value]);

  const getPoint = (event) => {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const sourceEvent = event.touches?.[0] || event;
    return {
      x: sourceEvent.clientX - rect.left,
      y: sourceEvent.clientY - rect.top,
    };
  };

  const startDrawing = (event) => {
    event.preventDefault();
    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");
    const point = getPoint(event);
    drawingRef.current = true;
    context.beginPath();
    context.moveTo(point.x, point.y);
  };

  const draw = (event) => {
    if (!drawingRef.current) return;
    event.preventDefault();
    const context = canvasRef.current.getContext("2d");
    const point = getPoint(event);
    context.lineTo(point.x, point.y);
    context.stroke();
  };

  const stopDrawing = () => {
    if (!drawingRef.current) return;
    drawingRef.current = false;
    onChange(canvasRef.current.toDataURL("image/png"));
  };

  const clear = () => {
    const canvas = canvasRef.current;
    const context = canvas.getContext("2d");
    const rect = canvas.getBoundingClientRect();
    context.clearRect(0, 0, rect.width, rect.height);
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, rect.width, rect.height);
    onChange("");
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-2">
        <label className="block text-sm font-semibold text-slate-700">
          {label}
        </label>
        <button
          type="button"
          onClick={clear}
          className="rounded border border-slate-200 px-2 py-1 text-xs font-bold text-slate-500 hover:bg-slate-50"
        >
          Limpar
        </button>
      </div>
      <canvas
        ref={canvasRef}
        className="h-32 w-full rounded-lg border border-slate-300 bg-white touch-none"
        onMouseDown={startDrawing}
        onMouseMove={draw}
        onMouseUp={stopDrawing}
        onMouseLeave={stopDrawing}
        onTouchStart={startDrawing}
        onTouchMove={draw}
        onTouchEnd={stopDrawing}
      />
      {!value && (
        <p className="text-xs text-slate-400">
          Assine no campo acima com mouse ou toque.
        </p>
      )}
    </div>
  );
}

export default function PainelEstoque() {
  const [materiais, setMateriais] = useState([]);
  const [funcionarios, setFuncionarios] = useState([]);
  const [comarcas, setComarcas] = useState([]);
  const [historico, setHistorico] = useState([]);
  const [ordensRetirada, setOrdensRetirada] = useState([]);
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
  const [fotoExpandida, setFotoExpandida] = useState(null);
  const [materialEmEdicao, setMaterialEmEdicao] = useState(null);
  const [ordemRetiradaAtual, setOrdemRetiradaAtual] = useState(null);
  const [acaoOr, setAcaoOr] = useState("retirada");

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
    categoria: "MATERIAL_CONSUMO",
    descricao: "",
    fotoProdutoUrl: "",
    fabricante: "",
    fornecedor: "",
    localizacao: "",
    quantidadeDisponivel: "0",
  });
  const [orForm, setOrForm] = useState({
    conferidoPor: "",
    levadoPor: "",
    assinaturaConferente: "",
    assinaturaRetirante: "",
    devolvidoPor: "",
    recebidoPor: "",
    assinaturaRecebimento: "",
    devolucoes: {},
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

      const ordensRetiradaResponse = await api.get("/ordens-retirada");
      setOrdensRetirada(ordensRetiradaResponse.data || []);

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
    setMaterialEmEdicao(null);
    setOrdemRetiradaAtual(null);
    setFormData({ materialId: "", quantidade: "", funcionarioId: "", comarcaId: "" });
    setOrForm({
      conferidoPor: "",
      levadoPor: "",
      assinaturaConferente: "",
      assinaturaRetirante: "",
      devolvidoPor: "",
      recebidoPor: "",
      assinaturaRecebimento: "",
      devolucoes: {},
    });
    setNovoMaterialData({
      nome: "",
      partNumber: "",
      categoria: "MATERIAL_CONSUMO",
      descricao: "",
      fotoProdutoUrl: "",
      fabricante: "",
      fornecedor: "",
      localizacao: "",
      quantidadeDisponivel: "0",
    });
    setFotoExpandida(null);
  };

  const abrirModalNovoMaterial = () => {
    setMaterialEmEdicao(null);
    setNovoMaterialData({
      nome: "",
      partNumber: "",
      categoria: "MATERIAL_CONSUMO",
      descricao: "",
      fotoProdutoUrl: "",
      fabricante: "",
      fornecedor: "",
      localizacao: "",
      quantidadeDisponivel: "0",
    });
    setShowNovoMaterialModal(true);
  };

  const abrirModalEditarMaterial = (material) => {
    setMaterialEmEdicao(material);
    setNovoMaterialData({
      nome: material.nome || "",
      partNumber: material.partNumber || "",
      categoria: material.categoria || "MATERIAL_CONSUMO",
      descricao: material.descricao || "",
      fotoProdutoUrl: material.fotoProdutoUrl || "",
      fabricante: material.fabricante || "",
      fornecedor: material.fornecedor || "",
      localizacao: material.localizacao || "",
      quantidadeDisponivel: String(material.quantidadeDisponivel ?? 0),
    });
    setShowNovoMaterialModal(true);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNovoMaterialChange = (e) => {
    const { name, value } = e.target;
    setNovoMaterialData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNovoMaterialFotoChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setError("Selecione uma imagem válida nos formatos JPEG, PNG ou WEBP.");
      e.target.value = "";
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setNovoMaterialData((prev) => ({
        ...prev,
        fotoProdutoUrl: reader.result,
      }));
    };
    reader.readAsDataURL(file);
  };

  //   SUBMIT DO PRODUTO NOVO (CATÁLOGO)
  const handleSubmitNovoMaterial = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        ...novoMaterialData,
        quantidadeDisponivel:
          parseInt(novoMaterialData.quantidadeDisponivel, 10) || 0,
      };
      if (materialEmEdicao) {
        await api.put(`/estoque/materiais/${materialEmEdicao.id}`, payload);
      } else {
        await api.post("/estoque/materiais", payload);
      }
      setSuccessMessage(
        materialEmEdicao
          ? "Material atualizado com sucesso!"
          : "Novo material cadastrado no catálogo com sucesso!",
      );
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

  const abrirModalOr = (ordemRetirada, acao) => {
    setOrdemRetiradaAtual(ordemRetirada);
    setAcaoOr(acao);
    setOrForm({
      conferidoPor: "",
      levadoPor: "",
      assinaturaConferente: "",
      assinaturaRetirante: "",
      devolvidoPor: "",
      recebidoPor: "",
      assinaturaRecebimento: "",
      devolucoes: Object.fromEntries(
        (ordemRetirada.itens || []).map((item) => [
          item.id,
          item.categoria === "FERRAMENTA"
            ? item.quantidadeRetirada || item.quantidadeSolicitada || 0
            : 0,
        ]),
      ),
    });
    setShowSaidaModal(true);
  };

  const abrirPrimeiraOrPendente = () => {
    const ordem = ordensRetirada.find((or) => or.status === "GERADA");
    if (!ordem) {
      alert("Não há OR gerada pendente de retirada.");
      return;
    }
    abrirModalOr(ordem, "retirada");
  };

  const handleSubmitOr = async (e) => {
    e.preventDefault();
    if (!ordemRetiradaAtual) return;

    try {
      if (acaoOr === "retirada") {
        if (!orForm.assinaturaConferente || !orForm.assinaturaRetirante) {
          alert("As duas assinaturas desenhadas são obrigatórias para executar a OR.");
          return;
        }
        await api.patch(`/ordens-retirada/${ordemRetiradaAtual.id}/executar`, {
          conferidoPor: orForm.conferidoPor,
          levadoPor: orForm.levadoPor,
          assinaturaConferenteBase64: orForm.assinaturaConferente,
          assinaturaRetiranteBase64: orForm.assinaturaRetirante,
        });
        setSuccessMessage("Retirada por OR executada e assinada com sucesso!");
      } else {
        if (!orForm.assinaturaRecebimento) {
          alert("A assinatura desenhada de recebimento é obrigatória para devolver a OR.");
          return;
        }
        await api.patch(`/ordens-retirada/${ordemRetiradaAtual.id}/devolver`, {
          devolvidoPor: orForm.devolvidoPor,
          recebidoPor: orForm.recebidoPor,
          assinaturaRecebimentoBase64: orForm.assinaturaRecebimento,
          itens: (ordemRetiradaAtual.itens || []).map((item) => ({
            itemId: item.id,
            quantidadeDevolvida: parseInt(orForm.devolucoes[item.id] || 0, 10),
          })),
        });
        setSuccessMessage("Devolução da OR registrada com sucesso!");
      }
      setTimeout(() => setSuccessMessage(null), 4000);
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.erro || err.response?.data?.message || "Erro ao processar OR.");
      console.error(err);
    }
  };

  const getReservado = (material) => material.quantidadeReservada ?? 0;
  const getLivre = (material) =>
    Math.max(0, (material.quantidadeDisponivel ?? 0) - getReservado(material));
  const isCriticalStock = (material) => getLivre(material) <= 5;
  const getCategoriaMaterialLabel = (categoria) =>
    CATEGORIAS_MATERIAL.find((item) => item.value === categoria)?.label ||
    "Materiais de Consumo";
  const materiaisPorCategoria = CATEGORIAS_MATERIAL.map((categoria) => ({
    ...categoria,
    materiais: materiais.filter(
      (material) => (material.categoria || "MATERIAL_CONSUMO") === categoria.value,
    ),
  }));
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
      RETIRADA_OR: {
        label: "Retirada OR",
        icon: ArrowUpRight,
        className: "text-blue-700 bg-blue-50",
      },
      DEVOLUCAO_OR: {
        label: "Devolução OR",
        icon: ArrowDownLeft,
        className: "text-emerald-700 bg-emerald-50",
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
    if (mov.ordemRetirada?.numeroOr) partes.push(mov.ordemRetirada.numeroOr);
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
              onClick={abrirModalNovoMaterial}
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
              onClick={abrirPrimeiraOrPendente}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              <Minus size={20} /> Executar OR
            </button>
          </div>
        </div>
        {error && <Alert type="error" message={error} />}
        {successMessage && <Alert type="success" message={successMessage} />}
      </div>

      {/* Tabela de Saldo Atual */}
      <div className="bg-white rounded-lg shadow-md overflow-x-auto border border-slate-200">
        <table className="w-full min-w-[980px]">
          <thead className="bg-slate-50 border-b border-slate-200">
            <tr>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">
                Produto
              </th>
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">
                Categoria
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
                Em estoque
              </th>
              <th className="px-6 py-4 text-center text-sm font-semibold text-slate-700">
                Reservado
              </th>
              <th className="px-6 py-4 text-center text-sm font-semibold text-slate-700">
                Disponível
              </th>
              <th className="px-6 py-4 text-center text-sm font-semibold text-slate-700">
                Ações
              </th>
            </tr>
          </thead>
          <tbody>
            {materiaisPorCategoria.map((grupo) => (
              <Fragment key={grupo.value}>
                <tr className="bg-slate-100/70">
                  <td
                    colSpan="9"
                    className="px-6 py-2 text-xs font-black uppercase tracking-wide text-slate-600"
                  >
                    {grupo.label} ({grupo.materiais.length})
                  </td>
                </tr>
                {grupo.materiais.map((material) => (
                  <tr
                    key={material.id}
                    className={`border-b border-slate-200 hover:bg-slate-50 transition-colors ${isCriticalStock(material) ? "bg-red-50/50" : ""}`}
                  >
                    <td className="px-6 py-4 text-sm text-slate-800">
                      <div className="flex items-center gap-3">
                        {material.fotoProdutoUrl ? (
                          <button
                            type="button"
                            onClick={() => setFotoExpandida(material)}
                            className="h-11 w-11 overflow-hidden rounded-md border border-slate-200 bg-slate-50 transition hover:scale-105 hover:border-blue-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
                            title="Expandir foto do produto"
                          >
                            <img
                              src={material.fotoProdutoUrl}
                              alt={`Foto de ${material.nome}`}
                              className="h-full w-full object-cover"
                            />
                          </button>
                        ) : (
                          <span className="flex h-11 w-11 items-center justify-center rounded-md border border-slate-200 bg-slate-50 text-slate-400">
                            <Package size={18} />
                          </span>
                        )}
                        <span>
                          <strong className="block text-slate-800">
                            {material.nome}
                          </strong>
                          {material.descricao && (
                            <span className="block max-w-xs truncate text-xs text-slate-500">
                              {material.descricao}
                            </span>
                          )}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-xs font-bold uppercase text-slate-500">
                      {getCategoriaMaterialLabel(material.categoria)}
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
                      <span className="inline-flex min-w-[3rem] items-center justify-center rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-800">
                        {material.quantidadeDisponivel ?? 0}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className="inline-flex min-w-[3rem] items-center justify-center rounded-full bg-amber-100 px-3 py-1 text-sm font-semibold text-amber-800">
                        {getReservado(material)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span
                        className={`inline-flex min-w-[3rem] items-center justify-center rounded-full px-3 py-1 text-sm font-semibold ${isCriticalStock(material) ? "bg-red-200 text-red-800" : "bg-green-100 text-green-800"}`}
                      >
                        {isCriticalStock(material) && (
                          <AlertCircle size={14} className="mr-1" />
                        )}
                        {getLivre(material)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <button
                        type="button"
                        onClick={() => abrirModalEditarMaterial(material)}
                        className="inline-flex items-center gap-1 rounded-md border border-slate-200 bg-white px-2 py-1 text-xs font-bold text-slate-600 hover:bg-slate-50 hover:text-blue-600"
                        title="Editar item em estoque"
                      >
                        <Edit2 size={14} />
                        Editar
                      </button>
                    </td>
                  </tr>
                ))}
              </Fragment>
            ))}
            {materiais.length === 0 && (
              <tr>
                <td colSpan="9" className="px-6 py-8 text-center text-slate-400">
                  Nenhum produto cadastrado no estoque.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-slate-200 mb-8">
        <div className="p-5 border-b border-slate-200 bg-slate-50/50 flex items-center justify-between gap-3">
          <div>
            <h2 className="font-bold text-slate-800">Ordens de Retirada</h2>
            <p className="text-xs text-slate-500">
              Documento obrigatório para qualquer retirada ou devolução de material.
            </p>
          </div>
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-bold text-slate-600">
            {ordensRetirada.length} ORs
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-slate-50 text-left text-xs font-bold uppercase tracking-wide text-slate-500">
              <tr>
                <th className="px-6 py-3">OR</th>
                <th className="px-6 py-3">OS / Obra</th>
                <th className="px-6 py-3">Itens</th>
                <th className="px-6 py-3">Log</th>
                <th className="px-6 py-3 text-center">Status</th>
                <th className="px-6 py-3 text-center">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {ordensRetirada.map((or) => (
                <tr key={or.id} className="text-sm">
                  <td className="px-6 py-4 font-black text-slate-800">
                    {or.numeroOr}
                  </td>
                  <td className="px-6 py-4 text-slate-600">
                    <span className="block font-semibold text-slate-800">
                      {or.ordemServico?.numeroOs || "OS não vinculada"}
                    </span>
                    <span className="text-xs">{or.comarca?.nomeComarca}</span>
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-600">
                    {(or.itens || []).map((item) => (
                      <span
                        key={item.id}
                        className="mb-1 mr-1 inline-flex rounded bg-slate-100 px-2 py-1"
                      >
                        {item.nomeMaterial}: {item.quantidadeSolicitada}
                      </span>
                    ))}
                  </td>
                  <td className="px-6 py-4 text-xs text-slate-500">
                    {or.levadoPor && <p>Levou: {or.levadoPor}</p>}
                    {or.dataRetirada && (
                      <p>Retirada: {new Date(or.dataRetirada).toLocaleString("pt-BR")}</p>
                    )}
                    {or.devolvidoPor && <p>Devolveu: {or.devolvidoPor}</p>}
                    {or.dataDevolucao && (
                      <p>Devolução: {new Date(or.dataDevolucao).toLocaleString("pt-BR")}</p>
                    )}
                  </td>
                  <td className="px-6 py-4 text-center">
                    <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-black uppercase text-blue-700">
                      {or.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    {or.status === "GERADA" && (
                      <button
                        type="button"
                        onClick={() => abrirModalOr(or, "retirada")}
                        className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-bold text-white hover:bg-blue-700"
                      >
                        Executar Retirada
                      </button>
                    )}
                    {or.status === "RETIRADA" && (
                      <button
                        type="button"
                        onClick={() => abrirModalOr(or, "devolucao")}
                        className="rounded-lg bg-emerald-600 px-3 py-2 text-xs font-bold text-white hover:bg-emerald-700"
                      >
                        Registrar Devolução
                      </button>
                    )}
                    {or.status === "DEVOLVIDA" && (
                      <span className="text-xs font-bold text-slate-400">Concluída</span>
                    )}
                  </td>
                </tr>
              ))}
              {ordensRetirada.length === 0 && (
                <tr>
                  <td colSpan="6" className="px-6 py-8 text-center text-slate-400">
                    Nenhuma Ordem de Retirada gerada.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
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

      <Modal
        isOpen={Boolean(fotoExpandida)}
        onClose={() => setFotoExpandida(null)}
        title={fotoExpandida?.nome || "Foto do Produto"}
      >
        <div className="space-y-3">
          {fotoExpandida?.fotoProdutoUrl && (
            <img
              src={fotoExpandida.fotoProdutoUrl}
              alt={`Foto ampliada de ${fotoExpandida.nome}`}
              className="max-h-[70vh] w-full rounded-lg border border-slate-200 bg-slate-950 object-contain"
            />
          )}
          {fotoExpandida?.descricao && (
            <p className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
              {fotoExpandida.descricao}
            </p>
          )}
        </div>
      </Modal>

      {/*   MODAL NOVO: Cadastro de Material do Catálogo */}
      <Modal
        isOpen={showNovoMaterialModal}
        onClose={handleCloseModal}
        title={materialEmEdicao ? "Editar Material do Estoque" : "Cadastrar Novo Material no Catálogo"}
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
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Categoria
            </label>
            <select
              name="categoria"
              value={novoMaterialData.categoria}
              onChange={handleNovoMaterialChange}
              className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
            >
              {CATEGORIAS_MATERIAL.map((categoria) => (
                <option key={categoria.value} value={categoria.value}>
                  {categoria.label}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Descrição
            </label>
            <textarea
              name="descricao"
              rows="3"
              value={novoMaterialData.descricao}
              onChange={handleNovoMaterialChange}
              className="w-full resize-none rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
              placeholder="Descreva aplicação, especificações, observações de uso ou retorno."
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              {materialEmEdicao ? "Quantidade em estoque" : "Quantidade inicial"}
            </label>
            <input
              type="number"
              name="quantidadeDisponivel"
              min="0"
              step="1"
              value={novoMaterialData.quantidadeDisponivel}
              onChange={handleNovoMaterialChange}
              className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
              placeholder="0"
            />
          </div>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_96px] md:items-end">
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">
                  Foto do Produto
                </label>
                <input
                  type="url"
                  name="fotoProdutoUrl"
                  value={
                    novoMaterialData.fotoProdutoUrl?.startsWith("data:")
                      ? ""
                      : novoMaterialData.fotoProdutoUrl
                  }
                  onChange={handleNovoMaterialChange}
                  className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
                  placeholder="https://exemplo.com/foto-produto.png"
                />
              </div>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleNovoMaterialFotoChange}
                className="w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-slate-100 file:px-3 file:py-2 file:text-sm file:font-bold file:text-slate-700 hover:file:bg-slate-200"
              />
            </div>
            <div className="flex h-24 w-24 items-center justify-center rounded-lg border border-dashed border-slate-300 bg-slate-50">
              {novoMaterialData.fotoProdutoUrl ? (
                <img
                  src={novoMaterialData.fotoProdutoUrl}
                  alt="Preview do produto"
                  className="h-full w-full rounded-lg object-cover"
                />
              ) : (
                <Package size={22} className="text-slate-400" />
              )}
            </div>
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
              {materialEmEdicao ? "Salvar Alterações" : "Cadastrar Produto"}
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

      {/* Modal OR */}
      <Modal
        isOpen={showSaidaModal}
        onClose={handleCloseModal}
        title={`${acaoOr === "retirada" ? "Executar Retirada" : "Registrar Devolução"} - ${ordemRetiradaAtual?.numeroOr || ""}`}
      >
        <form onSubmit={handleSubmitOr} className="space-y-4">
          {ordemRetiradaAtual && (
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600">
              <p className="font-black text-slate-800">{ordemRetiradaAtual.numeroOr}</p>
              <p>
                {ordemRetiradaAtual.ordemServico?.numeroOs} -{" "}
                {ordemRetiradaAtual.comarca?.nomeComarca}
              </p>
              <div className="mt-2 flex flex-wrap gap-1">
                {(ordemRetiradaAtual.itens || []).map((item) => (
                  <span key={item.id} className="rounded bg-white px-2 py-1">
                    {item.nomeMaterial}: {item.quantidadeSolicitada}
                  </span>
                ))}
              </div>
            </div>
          )}

          {acaoOr === "retirada" ? (
            <>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1">
                    Quem conferiu os itens *
                  </label>
                  <input
                    value={orForm.conferidoPor}
                    onChange={(e) =>
                      setOrForm((prev) => ({ ...prev, conferidoPor: e.target.value }))
                    }
                    required
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1">
                    Quem levou os itens *
                  </label>
                  <input
                    value={orForm.levadoPor}
                    onChange={(e) =>
                      setOrForm((prev) => ({ ...prev, levadoPor: e.target.value }))
                    }
                    required
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
              </div>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <SignatureBox
                  label="Assinatura desenhada do conferente *"
                  value={orForm.assinaturaConferente}
                  onChange={(assinaturaConferente) =>
                    setOrForm((prev) => ({ ...prev, assinaturaConferente }))
                  }
                />
                <SignatureBox
                  label="Assinatura desenhada de quem levou *"
                  value={orForm.assinaturaRetirante}
                  onChange={(assinaturaRetirante) =>
                    setOrForm((prev) => ({ ...prev, assinaturaRetirante }))
                  }
                />
              </div>
            </>
          ) : (
            <>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1">
                    Quem devolveu *
                  </label>
                  <input
                    value={orForm.devolvidoPor}
                    onChange={(e) =>
                      setOrForm((prev) => ({ ...prev, devolvidoPor: e.target.value }))
                    }
                    required
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1">
                    Quem conferiu/recebeu *
                  </label>
                  <input
                    value={orForm.recebidoPor}
                    onChange={(e) =>
                      setOrForm((prev) => ({ ...prev, recebidoPor: e.target.value }))
                    }
                    required
                    className="w-full px-4 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
              </div>
              <div className="rounded-lg border border-slate-200">
                <div className="grid grid-cols-[1fr_120px] gap-3 bg-slate-50 px-3 py-2 text-xs font-black uppercase text-slate-500">
                  <span>Item devolvido</span>
                  <span>Qtd.</span>
                </div>
                {(ordemRetiradaAtual?.itens || []).map((item) => (
                  <div
                    key={item.id}
                    className="grid grid-cols-[1fr_120px] items-center gap-3 border-t border-slate-100 px-3 py-2 text-sm"
                  >
                    <span>
                      {item.nomeMaterial}
                      {item.categoria === "FERRAMENTA" && (
                        <strong className="ml-2 rounded bg-amber-100 px-2 py-0.5 text-[10px] uppercase text-amber-800">
                          retorno obrigatório
                        </strong>
                      )}
                    </span>
                    <input
                      type="number"
                      min="0"
                      max={item.quantidadeRetirada || item.quantidadeSolicitada || 0}
                      value={orForm.devolucoes[item.id] ?? 0}
                      onChange={(e) =>
                        setOrForm((prev) => ({
                          ...prev,
                          devolucoes: {
                            ...prev.devolucoes,
                            [item.id]: e.target.value,
                          },
                        }))
                      }
                      className="w-full rounded border border-slate-300 px-2 py-1"
                    />
                  </div>
                ))}
              </div>
              <SignatureBox
                label="Assinatura desenhada de recebimento *"
                value={orForm.assinaturaRecebimento}
                onChange={(assinaturaRecebimento) =>
                  setOrForm((prev) => ({ ...prev, assinaturaRecebimento }))
                }
              />
            </>
          )}

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
              {acaoOr === "retirada" ? "Confirmar Retirada" : "Confirmar Devolução"}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
