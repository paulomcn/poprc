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
  SlidersHorizontal,
  ArrowRightLeft,
  Download,
} from "lucide-react";
import api from "../services/api";
import Modal from "../components/Modal";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";

const CATEGORIAS_MATERIAL = [
  { value: "MATERIAL_CONSUMO", label: "Materiais de Consumo" },
  { value: "FERRAMENTA", label: "Ferramentas" },
];

const TIPOS_CONTROLE = [
  { value: "UNIDADE", label: "Por unidade" },
  { value: "PECA_COM_COMPRIMENTO", label: "Peça com comprimento" },
  { value: "METRAGEM", label: "Por metragem" },
  { value: "ROLO", label: "Por rolo" },
  { value: "BOBINA", label: "Por bobina" },
];

const UNIDADES_MEDIDA = [
  { value: "UNIDADE", label: "un" },
  { value: "PECA", label: "peça" },
  { value: "METRO", label: "m" },
  { value: "ROLO", label: "rolo" },
  { value: "PACOTE", label: "pacote" },
  { value: "BOBINA", label: "bobina" },
];

const unidadePadraoPorControle = {
  UNIDADE: "UNIDADE",
  PECA_COM_COMPRIMENTO: "PECA",
  METRAGEM: "METRO",
  ROLO: "ROLO",
  BOBINA: "BOBINA",
};

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
  const [unidadesRastreaveis, setUnidadesRastreaveis] = useState([]);
  const [locaisEstoque, setLocaisEstoque] = useState([]);
  const [saldosLocais, setSaldosLocais] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [historicoFiltro, setHistoricoFiltro] = useState("");
  const [historicoTipoFiltro, setHistoricoTipoFiltro] = useState("");
  const [historicoComarcaFiltro, setHistoricoComarcaFiltro] = useState("");
  const [historicoProjetoFiltro, setHistoricoProjetoFiltro] = useState("");
  const [historicoMaterialFiltro, setHistoricoMaterialFiltro] = useState("");
  const [historicoDataInicio, setHistoricoDataInicio] = useState("");
  const [historicoDataFim, setHistoricoDataFim] = useState("");
  const [historicoPessoaFiltro, setHistoricoPessoaFiltro] = useState("");

  // Modais de controle
  const [showEntradaModal, setShowEntradaModal] = useState(false);
  const [showSaidaModal, setShowSaidaModal] = useState(false);
  const [showNovoMaterialModal, setShowNovoMaterialModal] = useState(false); //   NOVO MODAL
  const [showUnidadeRastreavelModal, setShowUnidadeRastreavelModal] = useState(false);
  const [showAjusteModal, setShowAjusteModal] = useState(false);
  const [showTransferenciaModal, setShowTransferenciaModal] = useState(false);
  const [showLocalEstoqueModal, setShowLocalEstoqueModal] = useState(false);
  const [showTransferenciaUnidadeModal, setShowTransferenciaUnidadeModal] = useState(false);
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
    localEstoqueId: "",
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
    tipoControle: "UNIDADE",
    unidadeMedida: "UNIDADE",
    dimensao: "",
    comprimentoPorPeca: "",
    metragemDisponivel: "0",
    estoqueMinimo: "0",
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
    alocacoes: {},
    devolucoesAlocacao: {},
  });
  const [unidadeForm, setUnidadeForm] = useState({
    materialId: "",
    codigo: "",
    metragemInicial: "",
    observacao: "",
    localEstoqueId: "",
  });
  const [materialOperacao, setMaterialOperacao] = useState(null);
  const [ajusteForm, setAjusteForm] = useState({
    tipo: "AJUSTE_POSITIVO",
    localEstoqueId: "",
    valor: "",
    motivo: "",
    lancadoPor: "",
    autorizadoPor: "",
  });
  const [transferenciaForm, setTransferenciaForm] = useState({
    origemId: "",
    destinoId: "",
    valor: "",
    motivo: "",
    lancadoPor: "",
    autorizadoPor: "",
  });
  const [localForm, setLocalForm] = useState({ nome: "", endereco: "" });
  const [unidadeOperacao, setUnidadeOperacao] = useState(null);
  const [transferenciaUnidadeForm, setTransferenciaUnidadeForm] = useState({
    destinoId: "",
    motivo: "",
    lancadoPor: "",
    autorizadoPor: "",
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

      const unidadesResponse = await api.get("/estoque/unidades-rastreaveis");
      setUnidadesRastreaveis(unidadesResponse.data || []);

      const [locaisResponse, saldosResponse] = await Promise.all([
        api.get("/estoque/locais"),
        api.get("/estoque/saldos-locais"),
      ]);
      setLocaisEstoque(locaisResponse.data || []);
      setSaldosLocais(saldosResponse.data || []);

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
    setShowUnidadeRastreavelModal(false);
    setShowAjusteModal(false);
    setShowTransferenciaModal(false);
    setShowLocalEstoqueModal(false);
    setShowTransferenciaUnidadeModal(false);
    setMaterialEmEdicao(null);
    setOrdemRetiradaAtual(null);
    setFormData({ materialId: "", quantidade: "", funcionarioId: "", comarcaId: "", localEstoqueId: "" });
    setOrForm({
      conferidoPor: "",
      levadoPor: "",
      assinaturaConferente: "",
      assinaturaRetirante: "",
      devolvidoPor: "",
      recebidoPor: "",
      assinaturaRecebimento: "",
      devolucoes: {},
      alocacoes: {},
      devolucoesAlocacao: {},
    });
    setUnidadeForm({ materialId: "", codigo: "", metragemInicial: "", observacao: "", localEstoqueId: "" });
    setMaterialOperacao(null);
    setAjusteForm({ tipo: "AJUSTE_POSITIVO", localEstoqueId: "", valor: "", motivo: "", lancadoPor: "", autorizadoPor: "" });
    setTransferenciaForm({ origemId: "", destinoId: "", valor: "", motivo: "", lancadoPor: "", autorizadoPor: "" });
    setLocalForm({ nome: "", endereco: "" });
    setUnidadeOperacao(null);
    setTransferenciaUnidadeForm({ destinoId: "", motivo: "", lancadoPor: "", autorizadoPor: "" });
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
      tipoControle: "UNIDADE",
      unidadeMedida: "UNIDADE",
      dimensao: "",
      comprimentoPorPeca: "",
      metragemDisponivel: "0",
      estoqueMinimo: "0",
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
      tipoControle: "UNIDADE",
      unidadeMedida: "UNIDADE",
      dimensao: "",
      comprimentoPorPeca: "",
      metragemDisponivel: "0",
      estoqueMinimo: "0",
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
      tipoControle: material.tipoControle || "UNIDADE",
      unidadeMedida: material.unidadeMedida || "UNIDADE",
      dimensao: material.dimensao || "",
      comprimentoPorPeca: String(material.comprimentoPorPeca ?? ""),
      metragemDisponivel: String(material.metragemDisponivel ?? 0),
      estoqueMinimo: String(material.estoqueMinimo ?? 0),
    });
    setShowNovoMaterialModal(true);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNovoMaterialChange = (e) => {
    const { name, value } = e.target;
    setNovoMaterialData((prev) => ({
      ...prev,
      [name]: value,
      ...(name === "tipoControle"
        ? { unidadeMedida: unidadePadraoPorControle[value] || "UNIDADE" }
        : {}),
    }));
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
        comprimentoPorPeca:
          parseFloat(novoMaterialData.comprimentoPorPeca) || null,
        metragemDisponivel:
          parseFloat(novoMaterialData.metragemDisponivel) || 0,
        estoqueMinimo: parseFloat(novoMaterialData.estoqueMinimo) || 0,
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
        quantidade:
          materialEntradaSelecionado?.tipoControle === "METRAGEM"
            ? null
            : parseInt(formData.quantidade, 10),
        metragem:
          materialEntradaSelecionado?.tipoControle === "METRAGEM"
            ? parseFloat(formData.quantidade)
            : null,
        funcionarioId: parseInt(formData.funcionarioId),
        localEstoqueId: Number(formData.localEstoqueId),
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
      alocacoes: Object.fromEntries(
        (ordemRetirada.itens || [])
          .filter((item) => ["BOBINA", "ROLO"].includes(item.material?.tipoControle))
          .map((item) => [
            item.id,
            [{ unidadeRastreavelId: "", metragem: String(item.quantidadeSolicitada || "") }],
          ]),
      ),
      devolucoesAlocacao: Object.fromEntries(
        (ordemRetirada.itens || []).flatMap((item) =>
          (item.alocacoes || []).map((alocacao) => [alocacao.id, "0"]),
        ),
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
          alocacoes: Object.entries(orForm.alocacoes).flatMap(([itemId, alocacoes]) =>
            alocacoes.map((alocacao) => ({
              itemId: Number(itemId),
              unidadeRastreavelId: Number(alocacao.unidadeRastreavelId),
              metragem: parseFloat(alocacao.metragem),
            })),
          ),
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
            quantidadeDevolvida: parseFloat(orForm.devolucoes[item.id] || 0),
          })),
          alocacoes: Object.entries(orForm.devolucoesAlocacao).map(
            ([alocacaoId, metragemDevolvida]) => ({
              alocacaoId: Number(alocacaoId),
              metragemDevolvida: parseFloat(metragemDevolvida || 0),
            }),
          ),
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

  const getReservado = (material) =>
    ["METRAGEM", "BOBINA", "ROLO"].includes(material?.tipoControle)
      ? Number(material.metragemReservada ?? 0)
      : Number(material.quantidadeReservada ?? 0);
  const controlaMetragem = (material) =>
    ["METRAGEM", "BOBINA", "ROLO"].includes(material?.tipoControle);
  const getLivre = (material) =>
    controlaMetragem(material)
      ? Math.max(0, Number(material.metragemDisponivel ?? 0) - getReservado(material))
      : Math.max(0, (material.quantidadeDisponivel ?? 0) - getReservado(material));
  const isCriticalStock = (material) =>
    getLivre(material) <= Number(material.estoqueMinimo ?? 0);
  const formatarNumero = (valor) =>
    Number(valor || 0).toLocaleString("pt-BR", { maximumFractionDigits: 3 });
  const unidadeMaterial = (material) =>
    controlaMetragem(material)
      ? "m"
      : UNIDADES_MEDIDA.find((item) => item.value === material.unidadeMedida)?.label || "un";
  const rastreavel = (material) => ["BOBINA", "ROLO"].includes(material?.tipoControle);
  const materiaisRastreaveis = materiais.filter((material) => rastreavel(material));
  const unidadesDisponiveis = (materialId) =>
    unidadesRastreaveis.filter(
      (unidade) =>
        String(unidade.material?.id) === String(materialId) && Number(unidade.metragemAtual || 0) > 0,
    );

  const atualizarAlocacao = (itemId, index, campo, valor) => {
    setOrForm((prev) => ({
      ...prev,
      alocacoes: {
        ...prev.alocacoes,
        [itemId]: (prev.alocacoes[itemId] || []).map((alocacao, indice) =>
          indice === index ? { ...alocacao, [campo]: valor } : alocacao,
        ),
      },
    }));
  };

  const adicionarAlocacao = (itemId) => {
    setOrForm((prev) => ({
      ...prev,
      alocacoes: {
        ...prev.alocacoes,
        [itemId]: [
          ...(prev.alocacoes[itemId] || []),
          { unidadeRastreavelId: "", metragem: "" },
        ],
      },
    }));
  };

  const removerAlocacao = (itemId, index) => {
    setOrForm((prev) => ({
      ...prev,
      alocacoes: {
        ...prev.alocacoes,
        [itemId]: (prev.alocacoes[itemId] || []).filter((_, indice) => indice !== index),
      },
    }));
  };

  const cadastrarUnidadeRastreavel = async (event) => {
    event.preventDefault();
    try {
      await api.post("/estoque/unidades-rastreaveis", {
        materialId: Number(unidadeForm.materialId),
        codigo: unidadeForm.codigo,
        metragemInicial: parseFloat(unidadeForm.metragemInicial),
        observacao: unidadeForm.observacao,
        localEstoqueId: Number(unidadeForm.localEstoqueId),
      });
      setSuccessMessage("Bobina/rolo cadastrado com sucesso!");
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.erro || err.response?.data?.message || "Erro ao cadastrar bobina/rolo.");
    }
  };

  const abrirAjuste = (material) => {
    setMaterialOperacao(material);
    const primeiroSaldo = saldosLocais.find((saldo) => saldo.material?.id === material.id);
    setAjusteForm({
      tipo: "AJUSTE_POSITIVO",
      localEstoqueId: primeiroSaldo?.localEstoque?.id ? String(primeiroSaldo.localEstoque.id) : "",
      valor: "",
      motivo: "",
      lancadoPor: "",
      autorizadoPor: "",
    });
    setShowAjusteModal(true);
  };

  const abrirTransferencia = (material) => {
    setMaterialOperacao(material);
    const primeiroSaldo = saldosLocais.find((saldo) => saldo.material?.id === material.id);
    setTransferenciaForm({
      origemId: primeiroSaldo?.localEstoque?.id ? String(primeiroSaldo.localEstoque.id) : "",
      destinoId: "",
      valor: "",
      motivo: "",
      lancadoPor: "",
      autorizadoPor: "",
    });
    setShowTransferenciaModal(true);
  };

  const registrarAjuste = async (event) => {
    event.preventDefault();
    try {
      await api.post("/estoque/ajustes", {
        materialId: materialOperacao.id,
        localEstoqueId: Number(ajusteForm.localEstoqueId),
        tipo: ajusteForm.tipo,
        valor: parseFloat(ajusteForm.valor),
        motivo: ajusteForm.motivo,
        lancadoPor: ajusteForm.lancadoPor,
        autorizadoPor: ajusteForm.autorizadoPor,
      });
      setSuccessMessage("Ajuste registrado no livro de movimentações.");
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.erro || err.response?.data?.message || "Erro ao registrar ajuste.");
    }
  };

  const registrarTransferencia = async (event) => {
    event.preventDefault();
    try {
      await api.post("/estoque/transferencias", {
        materialId: materialOperacao.id,
        origemId: Number(transferenciaForm.origemId),
        destinoId: Number(transferenciaForm.destinoId),
        valor: parseFloat(transferenciaForm.valor),
        motivo: transferenciaForm.motivo,
        lancadoPor: transferenciaForm.lancadoPor,
        autorizadoPor: transferenciaForm.autorizadoPor,
      });
      setSuccessMessage("Transferência registrada no livro de movimentações.");
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.erro || err.response?.data?.message || "Erro ao transferir material.");
    }
  };

  const cadastrarLocalEstoque = async (event) => {
    event.preventDefault();
    try {
      await api.post("/estoque/locais", localForm);
      setSuccessMessage("Depósito cadastrado com sucesso.");
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.erro || err.response?.data?.message || "Erro ao cadastrar depósito.");
    }
  };

  const abrirTransferenciaUnidade = (unidade) => {
    setUnidadeOperacao(unidade);
    setTransferenciaUnidadeForm({ destinoId: "", motivo: "", lancadoPor: "", autorizadoPor: "" });
    setShowTransferenciaUnidadeModal(true);
  };

  const transferirUnidadeRastreavel = async (event) => {
    event.preventDefault();
    try {
      await api.patch(`/estoque/unidades-rastreaveis/${unidadeOperacao.id}/transferir`, {
        destinoId: Number(transferenciaUnidadeForm.destinoId),
        motivo: transferenciaUnidadeForm.motivo,
        lancadoPor: transferenciaUnidadeForm.lancadoPor,
        autorizadoPor: transferenciaUnidadeForm.autorizadoPor,
      });
      setSuccessMessage("Bobina/rolo transferido com rastreabilidade completa.");
      handleCloseModal();
      fetchData();
    } catch (err) {
      setError(err.response?.data?.erro || err.response?.data?.message || "Erro ao transferir bobina/rolo.");
    }
  };
  const materialEntradaSelecionado = materiais.find(
    (material) => String(material.id) === String(formData.materialId),
  );
  const saldosDoMaterial = (materialId) =>
    saldosLocais.filter((saldo) => String(saldo.material?.id) === String(materialId));
  const saldoLocalValor = (saldo) =>
    controlaMetragem(saldo?.material)
      ? Number(saldo?.metragemDisponivel || 0)
      : Number(saldo?.quantidadeDisponivel || 0);
  const getCategoriaMaterialLabel = (categoria) =>
    CATEGORIAS_MATERIAL.find((item) => item.value === categoria)?.label ||
    "Materiais de Consumo";
  const materiaisPorCategoria = CATEGORIAS_MATERIAL.map((categoria) => ({
    ...categoria,
    materiais: materiais.filter(
      (material) => (material.categoria || "MATERIAL_CONSUMO") === categoria.value,
    ),
  }));
  const valorMovimentacao = (mov) =>
    mov.metragem != null ? `${formatarNumero(mov.metragem)} m` : `${mov.quantidade ?? 0} un`;
  const historicoFiltrado = historico.filter((mov) => {
    const termo = historicoFiltro.trim().toLowerCase();
    const correspondeTipo = !historicoTipoFiltro || mov.tipo === historicoTipoFiltro;
    const correspondeComarca =
      !historicoComarcaFiltro ||
      String(mov.comarca?.id || "") === String(historicoComarcaFiltro);
    const correspondeProjeto =
      !historicoProjetoFiltro ||
      String(mov.projeto?.id || "") === String(historicoProjetoFiltro);
    const correspondeMaterial =
      !historicoMaterialFiltro ||
      String(mov.material?.id || "") === String(historicoMaterialFiltro);
    const dataMovimentacao = mov.dataMovimentacao ? new Date(mov.dataMovimentacao) : null;
    const correspondeInicio =
      !historicoDataInicio ||
      (dataMovimentacao && dataMovimentacao >= new Date(`${historicoDataInicio}T00:00:00`));
    const correspondeFim =
      !historicoDataFim ||
      (dataMovimentacao && dataMovimentacao <= new Date(`${historicoDataFim}T23:59:59`));
    const pessoasMovimentacao = [
      mov.lancadoPor,
      mov.retiradoPor,
      mov.autorizadoPor,
      mov.funcionario?.nome,
    ].filter(Boolean);
    const correspondePessoa =
      !historicoPessoaFiltro || pessoasMovimentacao.includes(historicoPessoaFiltro);
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
      mov.motivo,
      mov.lancadoPor,
      mov.autorizadoPor,
      mov.retiradoPor,
      mov.unidadeRastreavel?.codigo,
      ]
        .filter(Boolean)
        .some((valor) => String(valor).toLowerCase().includes(termo));

    return correspondeTipo && correspondeComarca && correspondeProjeto && correspondeMaterial
      && correspondeInicio && correspondeFim && correspondePessoa && correspondeTexto;
  });

  const exportarHistoricoXlsx = async () => {
    const ExcelJS = (await import("exceljs")).default;
    const workbook = new ExcelJS.Workbook();
    workbook.creator = "RC Operations Hub";
    workbook.created = new Date();
    const worksheet = workbook.addWorksheet("Movimentações", {
      views: [{ state: "frozen", ySplit: 1 }],
    });
    worksheet.columns = [
      ["ID", "id"], ["Data", "data"], ["Hora", "hora"], ["Tipo", "tipo"],
      ["Código do material", "codigoMaterial"], ["Descrição do material", "material"],
      ["Categoria", "categoria"], ["Quantidade", "quantidade"], ["Unidade", "unidade"],
      ["Metragem", "metragem"], ["Bobina ou rolo", "unidadeRastreavel"],
      ["Saldo anterior", "saldoAnterior"], ["Saldo posterior", "saldoPosterior"],
      ["OS", "os"], ["OR", "or"], ["Autorizado por", "autorizadoPor"],
      ["Retirado por", "retiradoPor"], ["Adicionado/Lançado por", "lancadoPor"],
      ["Origem", "origem"], ["Destino", "destino"], ["Motivo", "motivo"],
      ["Observações", "observacoes"],
    ].map(([header, key]) => ({ header, key, width: 12 }));

    historicoFiltrado.forEach((mov) => {
      const data = mov.dataMovimentacao ? new Date(mov.dataMovimentacao) : null;
      worksheet.addRow({
        id: mov.id,
        data: data?.toLocaleDateString("pt-BR"),
        hora: data?.toLocaleTimeString("pt-BR"),
        tipo: mov.tipo,
        codigoMaterial: mov.material?.partNumber,
        material: mov.material?.nome,
        categoria: mov.material?.categoria,
        quantidade: mov.quantidade,
        unidade: mov.unidadeMedida,
        metragem: mov.metragem,
        unidadeRastreavel: mov.unidadeRastreavel?.codigo,
        saldoAnterior: mov.saldoAnterior,
        saldoPosterior: mov.saldoPosterior,
        os: mov.ordemServico?.numeroOs,
        or: mov.ordemRetirada?.numeroOr,
        autorizadoPor: mov.autorizadoPor,
        retiradoPor: mov.retiradoPor,
        lancadoPor: mov.lancadoPor || mov.funcionario?.nome,
        origem: mov.estoqueOrigem,
        destino: mov.estoqueDestino,
        motivo: mov.motivo,
        observacoes: mov.observacao,
      });
    });

    worksheet.getRow(1).eachCell((cell) => {
      cell.font = { bold: true, color: { argb: "FFFFFFFF" } };
      cell.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF1E293B" } };
      cell.alignment = { vertical: "middle", horizontal: "center", wrapText: true };
    });
    worksheet.getRow(1).height = 30;
    worksheet.autoFilter = { from: "A1", to: "V1" };

    worksheet.columns.forEach((column) => {
      let maior = String(column.header || "").length;
      column.eachCell({ includeEmpty: true }, (cell, rowNumber) => {
        if (rowNumber > 1) maior = Math.max(maior, String(cell.value ?? "").length);
        cell.alignment = { vertical: "top", wrapText: true };
      });
      column.width = Math.min(45, Math.max(10, maior + 2));
    });
    worksheet.eachRow((row, rowNumber) => {
      if (rowNumber === 1) return;
      let linhas = 1;
      row.eachCell({ includeEmpty: true }, (cell, columnNumber) => {
        const largura = worksheet.getColumn(columnNumber).width || 12;
        linhas = Math.max(linhas, Math.ceil(String(cell.value ?? "").length / largura));
      });
      row.height = Math.min(90, Math.max(20, linhas * 15));
    });

    const consumoSheet = workbook.addWorksheet("Consumo por OS-OR");
    consumoSheet.columns = [
      { header: "OS", key: "os", width: 24 },
      { header: "OR", key: "or", width: 28 },
      { header: "Material", key: "material", width: 32 },
      { header: "Consumo líquido", key: "consumo", width: 18 },
      { header: "Unidade", key: "unidade", width: 12 },
    ];
    relatorioConsumo.forEach((item) => consumoSheet.addRow(item));

    const alertasSheet = workbook.addWorksheet("Alertas por Depósito");
    alertasSheet.columns = [
      { header: "Depósito", key: "deposito", width: 26 },
      { header: "Material", key: "material", width: 34 },
      { header: "Saldo", key: "saldo", width: 14 },
      { header: "Estoque mínimo", key: "minimo", width: 16 },
      { header: "Unidade", key: "unidade", width: 12 },
    ];
    alertasReposicao.forEach((item) => alertasSheet.addRow(item));

    [consumoSheet, alertasSheet].forEach((sheet) => {
      sheet.autoFilter = { from: "A1", to: sheet === consumoSheet ? "E1" : "E1" };
      sheet.views = [{ state: "frozen", ySplit: 1 }];
      sheet.getRow(1).height = 28;
      sheet.getRow(1).eachCell((cell) => {
        cell.font = { bold: true, color: { argb: "FFFFFFFF" } };
        cell.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF1E293B" } };
        cell.alignment = { vertical: "middle", horizontal: "center", wrapText: true };
      });
      sheet.eachRow((row, rowNumber) => {
        if (rowNumber > 1) row.height = 22;
        row.eachCell((cell) => {
          cell.alignment = { vertical: "top", wrapText: true };
        });
      });
    });

    const buffer = await workbook.xlsx.writeBuffer();
    const url = URL.createObjectURL(new Blob([buffer], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    }));
    const link = document.createElement("a");
    link.href = url;
    link.download = `movimentacoes-estoque-${new Date().toISOString().slice(0, 10)}.xlsx`;
    link.click();
    URL.revokeObjectURL(url);
  };
  const tiposHistorico = [...new Set(historico.map((mov) => mov.tipo).filter(Boolean))];
  const projetosHistorico = [
    ...new Map(
      historico
        .filter((mov) => mov.projeto?.id)
        .map((mov) => [mov.projeto.id, mov.projeto]),
    ).values(),
  ];
  const pessoasHistorico = [
    ...new Set(
      historico.flatMap((mov) => [
        mov.lancadoPor,
        mov.retiradoPor,
        mov.autorizadoPor,
        mov.funcionario?.nome,
      ]).filter(Boolean),
    ),
  ].sort((a, b) => a.localeCompare(b, "pt-BR"));
  const relatorioConsumo = [
    ...historicoFiltrado
      .filter((mov) => ["RETIRADA_OR", "DEVOLUCAO_OR", "SAIDA"].includes(mov.tipo))
      .reduce((mapa, mov) => {
        const os = mov.ordemServico?.numeroOs || "Sem OS";
        const or = mov.ordemRetirada?.numeroOr || "Sem OR";
        const material = mov.material?.nome || "Material";
        const chave = `${os}|${or}|${mov.material?.id || material}`;
        const valor = Number(mov.metragem ?? mov.quantidade ?? 0)
          * (mov.tipo === "DEVOLUCAO_OR" ? -1 : 1);
        const atual = mapa.get(chave) || {
          os,
          or,
          material,
          unidade: mov.metragem != null ? "m" : "un",
          consumo: 0,
        };
        atual.consumo += valor;
        mapa.set(chave, atual);
        return mapa;
      }, new Map())
      .values(),
  ].filter((item) => item.consumo !== 0).sort((a, b) => b.consumo - a.consumo);

  const materiaisMaisUtilizados = [
    ...relatorioConsumo.reduce((mapa, item) => {
      const chave = `${item.material}|${item.unidade}`;
      mapa.set(chave, {
        material: item.material,
        unidade: item.unidade,
        consumo: (mapa.get(chave)?.consumo || 0) + item.consumo,
      });
      return mapa;
    }, new Map()).values(),
  ].sort((a, b) => b.consumo - a.consumo).slice(0, 5);

  const alertasReposicao = saldosLocais
    .filter((saldo) => saldoLocalValor(saldo) <= Number(saldo.material?.estoqueMinimo || 0))
    .map((saldo) => ({
      deposito: saldo.localEstoque?.nome || "Depósito",
      material: saldo.material?.nome || "Material",
      saldo: saldoLocalValor(saldo),
      minimo: Number(saldo.material?.estoqueMinimo || 0),
      unidade: unidadeMaterial(saldo.material),
    }))
    .sort((a, b) => a.saldo - b.saldo);

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
      AJUSTE_POSITIVO: {
        label: "Ajuste positivo",
        icon: Plus,
        className: "text-emerald-700 bg-emerald-50",
      },
      AJUSTE_NEGATIVO: {
        label: "Ajuste negativo",
        icon: Minus,
        className: "text-rose-700 bg-rose-50",
      },
      TRANSFERENCIA: {
        label: "Transferência",
        icon: ArrowRightLeft,
        className: "text-cyan-700 bg-cyan-50",
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

  const inicioMesAtual = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  const indicadoresEstoque = [
    { label: "Materiais cadastrados", valor: materiais.length },
    {
      label: "Metragem disponível",
      valor: `${formatarNumero(
        materiais.filter(controlaMetragem).reduce(
          (total, material) => total + Number(material.metragemDisponivel || 0),
          0,
        ),
      )} m`,
    },
    { label: "Abaixo do mínimo", valor: materiais.filter(isCriticalStock).length },
    {
      label: "Bobinas/rolos lacrados",
      valor: unidadesRastreaveis.filter((unidade) => unidade.status === "LACRADA").length,
    },
    {
      label: "Parcialmente utilizados",
      valor: unidadesRastreaveis.filter((unidade) =>
        ["PARCIALMENTE_UTILIZADA", "DEVOLVIDA_ESTOQUE"].includes(unidade.status),
      ).length,
    },
    {
      label: "Saídas no mês",
      valor: historico.filter((mov) =>
        ["RETIRADA_OR", "SAIDA", "AJUSTE_NEGATIVO"].includes(mov.tipo)
        && mov.dataMovimentacao
        && new Date(mov.dataMovimentacao) >= inicioMesAtual,
      ).length,
    },
  ];

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
              onClick={() => setShowUnidadeRastreavelModal(true)}
              disabled={materiaisRastreaveis.length === 0}
              className="flex items-center gap-2 bg-cyan-700 hover:bg-cyan-800 disabled:bg-slate-300 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
              title="Cadastrar bobina ou rolo individual"
            >
              <Plus size={20} /> Bobina / Rolo
            </button>
            <button
              onClick={() => setShowLocalEstoqueModal(true)}
              className="flex items-center gap-2 bg-slate-600 hover:bg-slate-700 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              <Plus size={20} /> Depósito
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
        <table className="w-full min-w-[1120px]">
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
              <th className="px-6 py-4 text-left text-sm font-semibold text-slate-700">
                Medição
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
                    colSpan="10"
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
                      <div className="flex max-w-[220px] flex-wrap gap-1">
                        {saldosDoMaterial(material.id).map((saldo) => (
                          <span key={saldo.id} className="rounded bg-slate-100 px-2 py-1 text-[11px]">
                            {saldo.localEstoque?.nome}: {formatarNumero(saldoLocalValor(saldo))} {unidadeMaterial(material)}
                          </span>
                        ))}
                        {saldosDoMaterial(material.id).length === 0 && "Não informado"}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-xs text-slate-600">
                      <strong className="block text-slate-700">
                        {TIPOS_CONTROLE.find(
                          (item) => item.value === (material.tipoControle || "UNIDADE"),
                        )?.label || "Por unidade"}
                      </strong>
                      <span>
                        {material.dimensao || unidadeMaterial(material)}
                        {material.comprimentoPorPeca
                          ? ` · ${formatarNumero(material.comprimentoPorPeca)} m/peça`
                          : ""}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className="inline-flex min-w-[3rem] items-center justify-center rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-800">
                        {controlaMetragem(material)
                          ? `${formatarNumero(material.metragemDisponivel)} m`
                          : `${material.quantidadeDisponivel ?? 0} ${unidadeMaterial(material)}`}
                      </span>
                      {!controlaMetragem(material) && Number(material.metragemDisponivel) > 0 && (
                        <span className="mt-1 block text-[11px] text-slate-500">
                          {formatarNumero(material.metragemDisponivel)} m no total
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span className="inline-flex min-w-[3rem] items-center justify-center rounded-full bg-amber-100 px-3 py-1 text-sm font-semibold text-amber-800">
                        {formatarNumero(getReservado(material))} {unidadeMaterial(material)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <span
                        className={`inline-flex min-w-[3rem] items-center justify-center rounded-full px-3 py-1 text-sm font-semibold ${isCriticalStock(material) ? "bg-red-200 text-red-800" : "bg-green-100 text-green-800"}`}
                      >
                        {isCriticalStock(material) && (
                          <AlertCircle size={14} className="mr-1" />
                        )}
                        {formatarNumero(getLivre(material))} {unidadeMaterial(material)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="flex justify-center gap-1">
                        <button
                          type="button"
                          onClick={() => abrirModalEditarMaterial(material)}
                          className="rounded-md border border-slate-200 bg-white p-2 text-slate-500 hover:text-blue-600"
                          title="Editar cadastro"
                        >
                          <Edit2 size={14} />
                        </button>
                        <button
                          type="button"
                          onClick={() => abrirAjuste(material)}
                          disabled={rastreavel(material)}
                          className="rounded-md border border-slate-200 bg-white p-2 text-slate-500 hover:text-amber-600 disabled:cursor-not-allowed disabled:opacity-30"
                          title="Ajustar saldo"
                        >
                          <SlidersHorizontal size={14} />
                        </button>
                        <button
                          type="button"
                          onClick={() => abrirTransferencia(material)}
                          disabled={rastreavel(material)}
                          className="rounded-md border border-slate-200 bg-white p-2 text-slate-500 hover:text-cyan-600 disabled:cursor-not-allowed disabled:opacity-30"
                          title="Transferir localização"
                        >
                          <ArrowRightLeft size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </Fragment>
            ))}
            {materiais.length === 0 && (
              <tr>
                <td colSpan="10" className="px-6 py-8 text-center text-slate-400">
                  Nenhum produto cadastrado no estoque.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        {indicadoresEstoque.map((indicador) => (
          <div key={indicador.label} className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
            <span className="block text-xs font-semibold text-slate-500">{indicador.label}</span>
            <strong className="mt-1 block text-xl text-slate-800">{indicador.valor}</strong>
          </div>
        ))}
      </div>

      {unidadesRastreaveis.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-md">
          <div className="border-b border-slate-200 bg-slate-50/50 p-5">
            <h2 className="font-bold text-slate-800">Bobinas e Rolos</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px]">
              <thead className="bg-slate-50 text-left text-xs font-bold uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3">Código</th>
                  <th className="px-5 py-3">Material</th>
                  <th className="px-5 py-3">Tipo</th>
                  <th className="px-5 py-3">Depósito</th>
                  <th className="px-5 py-3 text-right">Inicial</th>
                  <th className="px-5 py-3 text-right">Atual</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Entrada</th>
                  <th className="px-5 py-3 text-center">Ações</th>
                </tr>
              </thead>
              <tbody>
                {unidadesRastreaveis.map((unidade) => (
                  <tr key={unidade.id} className="border-t border-slate-100 text-sm">
                    <td className="px-5 py-3 font-mono font-bold text-slate-800">{unidade.codigo}</td>
                    <td className="px-5 py-3 text-slate-700">{unidade.material?.nome}</td>
                    <td className="px-5 py-3 text-slate-500">{unidade.tipo}</td>
                    <td className="px-5 py-3 text-slate-600">{unidade.localEstoque?.nome || "—"}</td>
                    <td className="px-5 py-3 text-right">{formatarNumero(unidade.metragemInicial)} m</td>
                    <td className="px-5 py-3 text-right font-bold">{formatarNumero(unidade.metragemAtual)} m</td>
                    <td className="px-5 py-3">
                      <span className="rounded bg-slate-100 px-2 py-1 text-xs font-bold text-slate-600">
                        {String(unidade.status || "").replaceAll("_", " ")}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-slate-500">
                      {unidade.dataEntrada ? new Date(unidade.dataEntrada).toLocaleString("pt-BR") : "—"}
                    </td>
                    <td className="px-5 py-3 text-center">
                      <button
                        type="button"
                        onClick={() => abrirTransferenciaUnidade(unidade)}
                        disabled={!unidade.localEstoque || Number(unidade.metragemAtual || 0) <= 0}
                        className="rounded-md border border-slate-200 bg-white p-2 text-slate-500 hover:text-cyan-600 disabled:opacity-30"
                        title="Transferir bobina/rolo completo"
                      >
                        <ArrowRightLeft size={15} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

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

      <section className="mb-8 border-y border-slate-200 bg-white">
        <div className="grid gap-0 xl:grid-cols-[0.8fr_1.35fr_1fr]">
          <div className="border-b border-slate-200 p-5 xl:border-b-0 xl:border-r">
            <h2 className="font-bold text-slate-800">Materiais mais utilizados</h2>
            <p className="mb-4 text-xs text-slate-500">Consumo líquido no período filtrado.</p>
            <div className="space-y-3">
              {materiaisMaisUtilizados.map((item, indice) => (
                <div key={item.material} className="flex items-center justify-between gap-3 text-sm">
                  <span className="min-w-0 truncate text-slate-700">
                    <strong className="mr-2 text-slate-400">{indice + 1}.</strong>
                    {item.material}
                  </span>
                  <strong className="shrink-0 text-slate-900">
                    {formatarNumero(item.consumo)} {item.unidade}
                  </strong>
                </div>
              ))}
              {materiaisMaisUtilizados.length === 0 && (
                <p className="text-sm text-slate-400">Sem consumo registrado para os filtros atuais.</p>
              )}
            </div>
          </div>

          <div className="border-b border-slate-200 p-5 xl:border-b-0 xl:border-r">
            <h2 className="font-bold text-slate-800">Consumo por OS e OR</h2>
            <p className="mb-3 text-xs text-slate-500">Retiradas menos devoluções, agrupadas por documento.</p>
            <div className="max-h-64 overflow-auto">
              <table className="w-full text-left text-sm">
                <thead className="sticky top-0 bg-white text-xs uppercase text-slate-500">
                  <tr>
                    <th className="py-2 pr-3">OS / OR</th>
                    <th className="py-2 pr-3">Material</th>
                    <th className="py-2 text-right">Consumo</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {relatorioConsumo.map((item) => (
                    <tr key={`${item.os}-${item.or}-${item.material}`}>
                      <td className="py-2 pr-3 text-xs text-slate-600">
                        <span className="block font-semibold text-slate-800">{item.os}</span>
                        {item.or}
                      </td>
                      <td className="py-2 pr-3 text-slate-700">{item.material}</td>
                      <td className="py-2 text-right font-bold text-slate-900">
                        {formatarNumero(item.consumo)} {item.unidade}
                      </td>
                    </tr>
                  ))}
                  {relatorioConsumo.length === 0 && (
                    <tr><td colSpan="3" className="py-6 text-center text-slate-400">Nenhum consumo encontrado.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          <div className="p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="font-bold text-slate-800">Alertas de reposição</h2>
                <p className="mb-3 text-xs text-slate-500">Saldo local igual ou abaixo do mínimo cadastrado.</p>
              </div>
              <span className={`rounded-full px-2.5 py-1 text-xs font-bold ${alertasReposicao.length ? "bg-red-100 text-red-700" : "bg-emerald-100 text-emerald-700"}`}>
                {alertasReposicao.length}
              </span>
            </div>
            <div className="max-h-64 space-y-2 overflow-auto">
              {alertasReposicao.map((item) => (
                <div key={`${item.deposito}-${item.material}`} className="border-l-2 border-red-400 pl-3 text-sm">
                  <span className="block font-semibold text-slate-800">{item.material}</span>
                  <span className="text-xs text-slate-500">
                    {item.deposito}: {formatarNumero(item.saldo)} em estoque / mínimo {formatarNumero(item.minimo)}
                  </span>
                </div>
              ))}
              {alertasReposicao.length === 0 && (
                <p className="text-sm text-emerald-700">Nenhum depósito abaixo do mínimo.</p>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Histórico */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden border border-slate-200">
        <div className="p-5 border-b border-slate-200 bg-slate-50/50 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div className="flex items-center gap-2">
            <History size={18} className="text-slate-500" />
            <h2 className="font-bold text-slate-800">
              Histórico de Movimentações
            </h2>
          </div>
          <div className="grid w-full gap-2 md:grid-cols-4 xl:grid-cols-[210px_140px_170px_130px_150px_150px_135px_135px_44px]">
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
            <select
              value={historicoMaterialFiltro}
              onChange={(e) => setHistoricoMaterialFiltro(e.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
            >
              <option value="">Todos os materiais</option>
              {materiais.map((material) => (
                <option key={material.id} value={material.id}>{material.nome}</option>
              ))}
            </select>
            <select
              value={historicoPessoaFiltro}
              onChange={(e) => setHistoricoPessoaFiltro(e.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
            >
              <option value="">Todas as pessoas</option>
              {pessoasHistorico.map((pessoa) => (
                <option key={pessoa} value={pessoa}>{pessoa}</option>
              ))}
            </select>
            <input
              type="date"
              value={historicoDataInicio}
              onChange={(e) => setHistoricoDataInicio(e.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
              title="Data inicial"
            />
            <input
              type="date"
              value={historicoDataFim}
              onChange={(e) => setHistoricoDataFim(e.target.value)}
              className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm"
              title="Data final"
            />
            <button
              type="button"
              onClick={exportarHistoricoXlsx}
              className="flex h-10 w-11 items-center justify-center rounded-lg bg-slate-800 text-white hover:bg-slate-700"
              title="Exportar movimentações filtradas em Excel"
            >
              <Download size={17} />
            </button>
          </div>
        </div>
        <table className="w-full min-w-[1200px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-600 border-b border-slate-200 text-xs font-bold uppercase">
            <tr>
              <th className="px-4 py-4">Data</th>
              <th className="px-6 py-4">Tipo</th>
              <th className="px-6 py-4">Material</th>
              <th className="px-4 py-4 text-center">Mov.</th>
              <th className="px-4 py-4 text-center">Saldo</th>
              <th className="px-4 py-4">Responsáveis</th>
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
                  <td className="px-4 py-4 whitespace-nowrap text-xs text-slate-500">
                    {mov.dataMovimentacao
                      ? new Date(mov.dataMovimentacao).toLocaleString("pt-BR")
                      : "—"}
                  </td>
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
                  <td className="px-4 py-4 text-center font-semibold text-slate-800">
                    {valorMovimentacao(mov)}
                  </td>
                  <td className="px-4 py-4 text-center text-xs text-slate-600">
                    {mov.saldoAnterior != null && mov.saldoPosterior != null
                      ? `${formatarNumero(mov.saldoAnterior)} → ${formatarNumero(mov.saldoPosterior)}`
                      : "Legado"}
                  </td>
                  <td className="px-4 py-4 text-xs text-slate-600">
                    <span className="block">
                      {mov.tipo === "ENTRADA" ? "Adicionou" : "Lançou"}: {mov.lancadoPor || mov.funcionario?.nome || "Sistema"}
                    </span>
                    {mov.autorizadoPor && <span className="block">Autorizou: {mov.autorizadoPor}</span>}
                    {mov.retiradoPor && <span className="block">Retirou: {mov.retiradoPor}</span>}
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
                  colSpan="7"
                  className="px-6 py-8 text-center text-slate-400"
                >
                  Nenhuma movimentação encontrada para o filtro informado.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <Modal isOpen={showLocalEstoqueModal} onClose={handleCloseModal} title="Cadastrar Depósito">
        <form onSubmit={cadastrarLocalEstoque} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Nome</label>
            <input
              required
              value={localForm.nome}
              onChange={(e) => setLocalForm((prev) => ({ ...prev, nome: e.target.value }))}
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Ex: Almoxarifado Central"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Endereço</label>
            <input
              value={localForm.endereco}
              onChange={(e) => setLocalForm((prev) => ({ ...prev, endereco: e.target.value }))}
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Endereço ou referência interna"
            />
          </div>
          <div className="flex justify-end gap-3 pt-3">
            <button type="button" onClick={handleCloseModal} className="rounded-lg border px-5 py-2">Cancelar</button>
            <button type="submit" className="rounded-lg bg-slate-800 px-5 py-2 font-bold text-white">Cadastrar</button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={showTransferenciaUnidadeModal}
        onClose={handleCloseModal}
        title="Transferir Bobina ou Rolo"
      >
        <form onSubmit={transferirUnidadeRastreavel} className="space-y-4">
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm">
            <strong className="block text-slate-800">{unidadeOperacao?.codigo}</strong>
            <span className="text-slate-500">
              {unidadeOperacao?.material?.nome} · {formatarNumero(unidadeOperacao?.metragemAtual)} m · Origem: {unidadeOperacao?.localEstoque?.nome}
            </span>
          </div>
          <select
            required
            value={transferenciaUnidadeForm.destinoId}
            onChange={(e) => setTransferenciaUnidadeForm((prev) => ({ ...prev, destinoId: e.target.value }))}
            className="w-full rounded-lg border border-slate-300 px-4 py-2"
          >
            <option value="">Depósito de destino</option>
            {locaisEstoque
              .filter((local) => String(local.id) !== String(unidadeOperacao?.localEstoque?.id))
              .map((local) => <option key={local.id} value={local.id}>{local.nome}</option>)}
          </select>
          <textarea
            required
            rows="3"
            value={transferenciaUnidadeForm.motivo}
            onChange={(e) => setTransferenciaUnidadeForm((prev) => ({ ...prev, motivo: e.target.value }))}
            className="w-full resize-none rounded-lg border border-slate-300 px-4 py-2"
            placeholder="Motivo da transferência"
          />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <input
              required
              value={transferenciaUnidadeForm.lancadoPor}
              onChange={(e) => setTransferenciaUnidadeForm((prev) => ({ ...prev, lancadoPor: e.target.value }))}
              className="rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Lançado por"
            />
            <input
              required
              value={transferenciaUnidadeForm.autorizadoPor}
              onChange={(e) => setTransferenciaUnidadeForm((prev) => ({ ...prev, autorizadoPor: e.target.value }))}
              className="rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Autorizado por"
            />
          </div>
          <div className="flex justify-end gap-3 pt-3">
            <button type="button" onClick={handleCloseModal} className="rounded-lg border px-5 py-2">Cancelar</button>
            <button type="submit" className="rounded-lg bg-cyan-700 px-5 py-2 font-bold text-white">Transferir unidade</button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={showAjusteModal} onClose={handleCloseModal} title="Ajustar Saldo">
        <form onSubmit={registrarAjuste} className="space-y-4">
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm">
            <strong className="block text-slate-800">{materialOperacao?.nome}</strong>
            <span className="text-slate-500">
              Saldo atual: {materialOperacao ? formatarNumero(getLivre(materialOperacao)) : 0} {materialOperacao ? unidadeMaterial(materialOperacao) : ""}
            </span>
          </div>
          <div className="grid grid-cols-2 overflow-hidden rounded-lg border border-slate-200">
            {[
              ["AJUSTE_POSITIVO", "Ajuste positivo"],
              ["AJUSTE_NEGATIVO", "Ajuste negativo"],
            ].map(([valor, label]) => (
              <button
                key={valor}
                type="button"
                onClick={() => setAjusteForm((prev) => ({ ...prev, tipo: valor }))}
                className={`px-3 py-2 text-sm font-bold ${ajusteForm.tipo === valor ? "bg-slate-800 text-white" : "bg-white text-slate-600"}`}
              >
                {label}
              </button>
            ))}
          </div>
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Depósito</label>
            <select
              required
              value={ajusteForm.localEstoqueId}
              onChange={(e) => setAjusteForm((prev) => ({ ...prev, localEstoqueId: e.target.value }))}
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
            >
              <option value="">Selecione o depósito</option>
              {locaisEstoque.map((local) => (
                <option key={local.id} value={local.id}>{local.nome}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Valor do ajuste</label>
            <input
              type="number"
              required
              min={controlaMetragem(materialOperacao) ? "0.001" : "1"}
              step={controlaMetragem(materialOperacao) ? "0.001" : "1"}
              value={ajusteForm.valor}
              onChange={(e) => setAjusteForm((prev) => ({ ...prev, valor: e.target.value }))}
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Motivo</label>
            <textarea
              required
              rows="3"
              value={ajusteForm.motivo}
              onChange={(e) => setAjusteForm((prev) => ({ ...prev, motivo: e.target.value }))}
              className="w-full resize-none rounded-lg border border-slate-300 px-4 py-2"
            />
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <input
              required
              value={ajusteForm.lancadoPor}
              onChange={(e) => setAjusteForm((prev) => ({ ...prev, lancadoPor: e.target.value }))}
              className="rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Lançado por"
            />
            <input
              required
              value={ajusteForm.autorizadoPor}
              onChange={(e) => setAjusteForm((prev) => ({ ...prev, autorizadoPor: e.target.value }))}
              className="rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Autorizado por"
            />
          </div>
          <div className="flex justify-end gap-3 pt-3">
            <button type="button" onClick={handleCloseModal} className="rounded-lg border px-5 py-2">Cancelar</button>
            <button type="submit" className="rounded-lg bg-slate-800 px-5 py-2 font-bold text-white">Registrar ajuste</button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={showTransferenciaModal} onClose={handleCloseModal} title="Transferir Localização">
        <form onSubmit={registrarTransferencia} className="space-y-4">
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm">
            <strong className="block text-slate-800">{materialOperacao?.nome}</strong>
            <span className="text-slate-500">Transferência parcial entre depósitos</span>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <select
              required
              value={transferenciaForm.origemId}
              onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, origemId: e.target.value }))}
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
            >
              <option value="">Origem</option>
              {saldosDoMaterial(materialOperacao?.id)
                .filter((saldo) => saldoLocalValor(saldo) > 0)
                .map((saldo) => (
                  <option key={saldo.localEstoque.id} value={saldo.localEstoque.id}>
                    {saldo.localEstoque.nome} ({formatarNumero(saldoLocalValor(saldo))} {unidadeMaterial(materialOperacao)})
                  </option>
                ))}
            </select>
            <select
              required
              value={transferenciaForm.destinoId}
              onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, destinoId: e.target.value }))}
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
            >
              <option value="">Destino</option>
              {locaisEstoque
                .filter((local) => String(local.id) !== String(transferenciaForm.origemId))
                .map((local) => <option key={local.id} value={local.id}>{local.nome}</option>)}
            </select>
          </div>
          <input
            type="number"
            required
            min={controlaMetragem(materialOperacao) ? "0.001" : "1"}
            step={controlaMetragem(materialOperacao) ? "0.001" : "1"}
            value={transferenciaForm.valor}
            onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, valor: e.target.value }))}
            className="w-full rounded-lg border border-slate-300 px-4 py-2"
            placeholder={`Valor a transferir (${materialOperacao ? unidadeMaterial(materialOperacao) : ""})`}
          />
          <textarea
            required
            rows="3"
            value={transferenciaForm.motivo}
            onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, motivo: e.target.value }))}
            className="w-full resize-none rounded-lg border border-slate-300 px-4 py-2"
            placeholder="Motivo da transferência"
          />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <input
              required
              value={transferenciaForm.lancadoPor}
              onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, lancadoPor: e.target.value }))}
              className="rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Lançado por"
            />
            <input
              required
              value={transferenciaForm.autorizadoPor}
              onChange={(e) => setTransferenciaForm((prev) => ({ ...prev, autorizadoPor: e.target.value }))}
              className="rounded-lg border border-slate-300 px-4 py-2"
              placeholder="Autorizado por"
            />
          </div>
          <div className="flex justify-end gap-3 pt-3">
            <button type="button" onClick={handleCloseModal} className="rounded-lg border px-5 py-2">Cancelar</button>
            <button type="submit" className="rounded-lg bg-cyan-700 px-5 py-2 font-bold text-white">Transferir</button>
          </div>
        </form>
      </Modal>

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
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Tipo de controle
              </label>
              <select
                name="tipoControle"
                value={novoMaterialData.tipoControle}
                onChange={handleNovoMaterialChange}
                className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
              >
                {TIPOS_CONTROLE.map((tipo) => (
                  <option key={tipo.value} value={tipo.value}>
                    {tipo.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Unidade de medida
              </label>
              <select
                name="unidadeMedida"
                value={novoMaterialData.unidadeMedida}
                onChange={handleNovoMaterialChange}
                disabled={novoMaterialData.tipoControle !== "UNIDADE"}
                className="w-full rounded-lg border border-slate-300 px-4 py-2 disabled:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-slate-500"
              >
                {UNIDADES_MEDIDA.map((unidade) => (
                  <option key={unidade.value} value={unidade.value}>
                    {unidade.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Tamanho / dimensão
              </label>
              <input
                type="text"
                name="dimensao"
                value={novoMaterialData.dimensao}
                onChange={handleNovoMaterialChange}
                className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
                placeholder="Ex: 20 mm ou 50 x 50 mm"
              />
            </div>
            {novoMaterialData.tipoControle === "PECA_COM_COMPRIMENTO" && (
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">
                  Comprimento por peça (m)
                </label>
                <input
                  type="number"
                  name="comprimentoPorPeca"
                  min="0.001"
                  step="0.001"
                  required
                  value={novoMaterialData.comprimentoPorPeca}
                  onChange={handleNovoMaterialChange}
                  className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
                  placeholder="3"
                />
              </div>
            )}
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
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {!materialEmEdicao && !["BOBINA", "ROLO"].includes(novoMaterialData.tipoControle) && (
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1">
                  {novoMaterialData.tipoControle === "METRAGEM"
                    ? "Metragem inicial"
                    : "Quantidade inicial"}
                </label>
                <input
                  type="number"
                  name={
                    novoMaterialData.tipoControle === "METRAGEM"
                      ? "metragemDisponivel"
                      : "quantidadeDisponivel"
                  }
                  min="0"
                  step={novoMaterialData.tipoControle === "METRAGEM" ? "0.001" : "1"}
                  value={
                    novoMaterialData.tipoControle === "METRAGEM"
                      ? novoMaterialData.metragemDisponivel
                      : novoMaterialData.quantidadeDisponivel
                  }
                  onChange={handleNovoMaterialChange}
                  className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
                  placeholder="0"
                />
              </div>
            )}
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1">
                Estoque mínimo ({UNIDADES_MEDIDA.find((item) => item.value === novoMaterialData.unidadeMedida)?.label})
              </label>
              <input
                type="number"
                name="estoqueMinimo"
                min="0"
                step={novoMaterialData.tipoControle === "METRAGEM" ? "0.001" : "1"}
                value={novoMaterialData.estoqueMinimo}
                onChange={handleNovoMaterialChange}
                className="w-full rounded-lg border border-slate-300 px-4 py-2 focus:outline-none focus:ring-2 focus:ring-slate-500"
                placeholder="0"
              />
            </div>
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
              {materiais.filter((material) => !rastreavel(material)).map((m) => (
                <option key={m.id} value={m.id}>
                  {m.nome} ({m.partNumber})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              {materialEntradaSelecionado?.tipoControle === "METRAGEM"
                ? "Metragem"
                : "Quantidade"}
            </label>
            <input
              type="number"
              name="quantidade"
              value={formData.quantidade}
              onChange={handleInputChange}
              min={materialEntradaSelecionado?.tipoControle === "METRAGEM" ? "0.001" : "1"}
              step={materialEntradaSelecionado?.tipoControle === "METRAGEM" ? "0.001" : "1"}
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
            <label className="block text-sm font-semibold text-slate-700 mb-1">Depósito de entrada</label>
            <select
              name="localEstoqueId"
              value={formData.localEstoqueId}
              onChange={handleInputChange}
              required
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
            >
              <option value="">Selecione o depósito</option>
              {locaisEstoque.map((local) => (
                <option key={local.id} value={local.id}>{local.nome}</option>
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

      <Modal
        isOpen={showUnidadeRastreavelModal}
        onClose={handleCloseModal}
        title="Cadastrar Bobina ou Rolo"
      >
        <form onSubmit={cadastrarUnidadeRastreavel} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Material</label>
            <select
              required
              value={unidadeForm.materialId}
              onChange={(event) =>
                setUnidadeForm((prev) => ({ ...prev, materialId: event.target.value }))
              }
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
            >
              <option value="">Selecione o material</option>
              {materiaisRastreaveis.map((material) => (
                <option key={material.id} value={material.id}>
                  {material.nome} ({material.tipoControle})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Depósito</label>
            <select
              required
              value={unidadeForm.localEstoqueId}
              onChange={(event) =>
                setUnidadeForm((prev) => ({ ...prev, localEstoqueId: event.target.value }))
              }
              className="w-full rounded-lg border border-slate-300 px-4 py-2"
            >
              <option value="">Selecione o depósito</option>
              {locaisEstoque.map((local) => (
                <option key={local.id} value={local.id}>{local.nome}</option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-semibold text-slate-700">Código único</label>
              <input
                required
                value={unidadeForm.codigo}
                onChange={(event) =>
                  setUnidadeForm((prev) => ({ ...prev, codigo: event.target.value }))
                }
                className="w-full rounded-lg border border-slate-300 px-4 py-2"
                placeholder="Ex: BOB-CAT6-001"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-semibold text-slate-700">Metragem inicial</label>
              <input
                type="number"
                required
                min="0.001"
                step="0.001"
                value={unidadeForm.metragemInicial}
                onChange={(event) =>
                  setUnidadeForm((prev) => ({ ...prev, metragemInicial: event.target.value }))
                }
                className="w-full rounded-lg border border-slate-300 px-4 py-2"
                placeholder="305"
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-sm font-semibold text-slate-700">Observação</label>
            <textarea
              rows="3"
              value={unidadeForm.observacao}
              onChange={(event) =>
                setUnidadeForm((prev) => ({ ...prev, observacao: event.target.value }))
              }
              className="w-full resize-none rounded-lg border border-slate-300 px-4 py-2"
            />
          </div>
          <div className="flex justify-end gap-3 pt-3">
            <button type="button" onClick={handleCloseModal} className="rounded-lg border px-5 py-2">
              Cancelar
            </button>
            <button type="submit" className="rounded-lg bg-cyan-700 px-5 py-2 font-bold text-white">
              Cadastrar
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
                    {item.nomeMaterial}: {item.quantidadeSolicitada} {unidadeMaterial(item.material)}
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
              {(ordemRetiradaAtual?.itens || [])
                .filter((item) => rastreavel(item.material))
                .map((item) => (
                  <div key={item.id} className="rounded-lg border border-cyan-200 bg-cyan-50/50 p-4">
                    <div className="mb-3 flex items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-bold text-slate-800">{item.nomeMaterial}</p>
                        <p className="text-xs text-slate-500">Total solicitado: {item.quantidadeSolicitada} m</p>
                      </div>
                      <button
                        type="button"
                        onClick={() => adicionarAlocacao(item.id)}
                        className="rounded border border-cyan-200 bg-white px-2 py-1 text-xs font-bold text-cyan-700"
                      >
                        + Bobina/Rolo
                      </button>
                    </div>
                    <div className="space-y-2">
                      {(orForm.alocacoes[item.id] || []).map((alocacao, index) => (
                        <div key={index} className="grid grid-cols-[1fr_110px_32px] gap-2">
                          <select
                            required
                            value={alocacao.unidadeRastreavelId}
                            onChange={(event) =>
                              atualizarAlocacao(
                                item.id,
                                index,
                                "unidadeRastreavelId",
                                event.target.value,
                              )
                            }
                            className="rounded border border-slate-300 bg-white px-3 py-2 text-sm"
                          >
                            <option value="">Selecione</option>
                            {unidadesDisponiveis(item.material?.id).map((unidade) => (
                              <option key={unidade.id} value={unidade.id}>
                                {unidade.codigo} ({formatarNumero(unidade.metragemAtual)} m)
                              </option>
                            ))}
                          </select>
                          <input
                            type="number"
                            required
                            min="0.001"
                            step="0.001"
                            value={alocacao.metragem}
                            onChange={(event) =>
                              atualizarAlocacao(item.id, index, "metragem", event.target.value)
                            }
                            className="rounded border border-slate-300 px-2 py-2 text-sm"
                          />
                          <button
                            type="button"
                            onClick={() => removerAlocacao(item.id, index)}
                            disabled={(orForm.alocacoes[item.id] || []).length === 1}
                            className="rounded text-slate-400 hover:bg-white hover:text-rose-600 disabled:opacity-30"
                            title="Remover alocação"
                          >
                            <Minus size={15} />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
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
                {(ordemRetiradaAtual?.itens || []).flatMap((item) =>
                  rastreavel(item.material)
                    ? (item.alocacoes || []).map((alocacao) => (
                        <div
                          key={`alocacao-${alocacao.id}`}
                          className="grid grid-cols-[1fr_120px] items-center gap-3 border-t border-slate-100 px-3 py-2 text-sm"
                        >
                          <span>
                            {item.nomeMaterial} · {alocacao.unidadeRastreavel?.codigo}
                            <small className="ml-2 text-slate-400">
                              retirado {alocacao.metragemRetirada} m
                            </small>
                          </span>
                          <input
                            type="number"
                            min="0"
                            max={Number(alocacao.metragemRetirada || 0) - Number(alocacao.metragemDevolvida || 0)}
                            step="0.001"
                            value={orForm.devolucoesAlocacao[alocacao.id] ?? 0}
                            onChange={(event) =>
                              setOrForm((prev) => ({
                                ...prev,
                                devolucoesAlocacao: {
                                  ...prev.devolucoesAlocacao,
                                  [alocacao.id]: event.target.value,
                                },
                              }))
                            }
                            className="w-full rounded border border-slate-300 px-2 py-1"
                          />
                        </div>
                      ))
                    : [
                        <div
                          key={`item-${item.id}`}
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
                            step={controlaMetragem(item.material) ? "0.001" : "1"}
                            value={orForm.devolucoes[item.id] ?? 0}
                            onChange={(event) =>
                              setOrForm((prev) => ({
                                ...prev,
                                devolucoes: {
                                  ...prev.devolucoes,
                                  [item.id]: event.target.value,
                                },
                              }))
                            }
                            className="w-full rounded border border-slate-300 px-2 py-1"
                          />
                        </div>,
                      ],
                )}
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
