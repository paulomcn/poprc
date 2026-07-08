import { useState, useEffect, useRef } from "react";
import {
  AlertTriangle,
  Edit2,
  MapPin,
  User,
  Zap,
  History,
  FileText,
  Upload,
  CheckCircle2,
  ChevronRight,
  PenTool,
  Plus,
  Trash2,
} from "lucide-react";
import api from "../services/api";
import Modal from "../components/Modal";
import LoadingSpinner from "../components/LoadingSpinner";
import Alert from "../components/Alert";
import HistoricoAtividadesComarca from "../components/HistoricoAtividadesComarca";

const API_FILE_BASE_URL = "http://localhost:8085";

export default function GestaoComarcas() {
  const [comarcas, setComarcas] = useState([]);
  const [materiaisEstoque, setMateriaisEstoque] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [selectedComarca, setSelectedComarca] = useState(null);
  const [comarcaHistorico, setComarcaHistorico] = useState(null);
  const [showAssinaturaModal, setShowAssinaturaModal] = useState(false);
  const [comarcaAssinaturaAtual, setComarcaAssinaturaAtual] = useState(null);
  const [showMaterialModal, setShowMaterialModal] = useState(false);
  const [comarcaMaterialAtual, setComarcaMaterialAtual] = useState(null);
  const [materialEmEdicao, setMaterialEmEdicao] = useState(null);
  const [materialForm, setMaterialForm] = useState({
    materialId: "",
    nomeMaterial: "",
    quantidadePrevista: "",
    itemAdicional: false,
  });
  const [showFaltantesModal, setShowFaltantesModal] = useState(false);
  const [comarcaFaltantesAtual, setComarcaFaltantesAtual] = useState(null);
  const [faltantesForm, setFaltantesForm] = useState({
    faltouMaterial: false,
    materialItemIds: [],
    descricao: "",
  });
  const [showTimelineModal, setShowTimelineModal] = useState(false);
  const [timelineMaterialAtual, setTimelineMaterialAtual] = useState(null);
  const [timelineForm, setTimelineForm] = useState({
    dataHoraSolicitacao: "",
    dataHoraRetirada: "",
    dataHoraUso: "",
  });
  const [viradaForms, setViradaForms] = useState({});

  // 💥 Controles reativos de validação para a Etapa 1 (Vistoria)
  const [fotosVistoria, setFotosVistoria] = useState({});
  const [assinaturasVistoria, setAssinaturasVistoria] = useState({});
  const [assinaturaEmEdicao, setAssinaturaEmEdicao] = useState(false);
  const assinaturaCanvasRef = useRef(null);
  const assinandoRef = useRef(false);
  const fotosVistoriaRef = useRef({});

  const [formData, setFormData] = useState({
    percentualConcluido: 0,
    pendencias: "",
  });

  useEffect(() => {
    fetchComarcas();
    fetchMateriaisEstoque();
  }, []);

  useEffect(() => {
    return () => {
      Object.values(fotosVistoriaRef.current).forEach((foto) => {
        if (foto?.previewUrl) {
          URL.revokeObjectURL(foto.previewUrl);
        }
      });
    };
  }, []);

  useEffect(() => {
    if (!showAssinaturaModal) return;

    const canvas = assinaturaCanvasRef.current;
    if (!canvas) return;

    const context = canvas.getContext("2d");
    const { width } = canvas.getBoundingClientRect();
    const height = 220;
    const ratio = window.devicePixelRatio || 1;

    canvas.width = width * ratio;
    canvas.height = height * ratio;
    canvas.style.height = `${height}px`;

    context.scale(ratio, ratio);
    context.lineWidth = 2.5;
    context.lineCap = "round";
    context.lineJoin = "round";
    context.strokeStyle = "#0f172a";
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, width, height);
    setAssinaturaEmEdicao(false);
  }, [showAssinaturaModal]);

  const fetchComarcas = async () => {
    try {
      setLoading(true);
      const response = await api.get("/comarcas");
      setComarcas(response.data || []);
      setError(null);
    } catch (err) {
      setError("Erro ao carregar comarcas");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const fetchMateriaisEstoque = async () => {
    try {
      const response = await api.get("/estoque/materiais");
      setMateriaisEstoque(response.data || []);
    } catch (err) {
      console.error("Erro ao carregar materiais do estoque", err);
    }
  };

  const getArquivoUrl = (caminho) => {
    if (!caminho) return null;
    if (
      caminho.startsWith("data:") ||
      caminho.startsWith("http://") ||
      caminho.startsWith("https://") ||
      caminho.startsWith("blob:")
    ) {
      return caminho;
    }
    return `${API_FILE_BASE_URL}${caminho}`;
  };

  const atualizarComarcaNaLista = (comarcaAtualizada) => {
    setComarcas((prev) =>
      prev.map((comarca) =>
        comarca.id === comarcaAtualizada.id
          ? { ...comarca, ...comarcaAtualizada }
          : comarca,
      ),
    );
  };

  const temFotoVistoria = (comarca) =>
    !!fotosVistoria[comarca.id]?.previewUrl || !!comarca.fotoVistoriaUrl;

  const temAssinaturaVistoria = (comarca) =>
    !!assinaturasVistoria[comarca.id]?.base64 || !!comarca.assinaturaBase64;

  // 💥 REGRA DE NEGÓCIO: Bloqueia avanço se não houver foto e assinatura digital
  const handleAvancarFase = async (comarca) => {
    if (!temFotoVistoria(comarca) || !temAssinaturaVistoria(comarca)) {
      alert(
        "🔒 BLOQUEIO OPERACIONAL: Upload de foto da vistoria e Assinatura com o Gerente são obrigatórios para liberar a infraestrutura!",
      );
      return;
    }

    try {
      const response = await api.patch(`/comarcas/${comarca.id}/avancar-etapa`);
      atualizarComarcaNaLista(response.data);
      const proximaEtapa = response.data.etapaAtual;
      alert(
        proximaEtapa === 3
          ? `Virada de Rede liberada para a comarca de ${comarca.nomeComarca}.`
          : `Etapa 2 (Infraestrutura) desbloqueada para a comarca de ${comarca.nomeComarca}.`,
      );
    } catch (err) {
      alert(
        err.response?.data?.erro ||
          "Erro ao homologar vistoria e liberar infraestrutura.",
      );
      console.error("Erro ao transicionar etapa", err);
    }
  };

  const handleOpenModal = (comarca) => {
    setSelectedComarca(comarca);
    setFormData({
      percentualConcluido: comarca.percentualConcluido || 0,
      pendencias: comarca.pendencias || "",
    });
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setSelectedComarca(null);
    setFormData({ percentualConcluido: 0, pendencias: "" });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedComarca) return;

    try {
      await api.patch(`/comarcas/${selectedComarca.id}`, {
        percentualConcluido: parseFloat(formData.percentualConcluido),
        pendencias: formData.pendencias,
      });

      setError(null);
      handleCloseModal();
      fetchComarcas();
    } catch (err) {
      setError("Erro ao atualizar comarca");
      console.error(err);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const abrirModalMaterial = (comarca, material = null) => {
    setComarcaMaterialAtual(comarca);
    setMaterialEmEdicao(material);
    setMaterialForm({
      materialId: material?.material?.id || material?.materialId || "",
      nomeMaterial: material?.nomeMaterial || "",
      quantidadePrevista: material?.quantidadePrevista || "",
      itemAdicional: Boolean(material?.itemAdicional),
    });
    setShowMaterialModal(true);
  };

  const abrirModalItemAdicional = (comarca) => {
    setComarcaMaterialAtual(comarca);
    setMaterialEmEdicao(null);
    setMaterialForm({
      materialId: "",
      nomeMaterial: "",
      quantidadePrevista: "",
      itemAdicional: true,
    });
    setShowMaterialModal(true);
  };

  const fecharModalMaterial = () => {
    setShowMaterialModal(false);
    setComarcaMaterialAtual(null);
    setMaterialEmEdicao(null);
    setMaterialForm({
      materialId: "",
      nomeMaterial: "",
      quantidadePrevista: "",
      itemAdicional: false,
    });
  };

  const handleMaterialFormChange = (e) => {
    const { name, value } = e.target;
    setMaterialForm((prev) => {
      if (name === "materialId") {
        const materialSelecionado = materiaisEstoque.find(
          (material) => String(material.id) === String(value),
        );
        return {
          ...prev,
          materialId: value,
          nomeMaterial: materialSelecionado?.nome || "",
        };
      }

      return { ...prev, [name]: value };
    });
  };

  const handleSalvarMaterialPrevisto = async (e) => {
    e.preventDefault();
    if (!comarcaMaterialAtual) return;

    const payload = {
      materialId: materialForm.materialId
        ? parseInt(materialForm.materialId, 10)
        : null,
      nomeMaterial: materialForm.nomeMaterial.trim(),
      quantidadePrevista: parseInt(materialForm.quantidadePrevista, 10),
    };

    try {
      const response = materialEmEdicao
        ? await api.put(
            `/comarcas/materiais-previstos/${materialEmEdicao.id}`,
            payload,
          )
        : materialForm.itemAdicional
          ? await api.post(
              `/comarcas/${comarcaMaterialAtual.id}/itens-adicionais`,
              payload,
            )
        : await api.post(
            `/comarcas/${comarcaMaterialAtual.id}/materiais-previstos`,
            payload,
          );

      atualizarComarcaNaLista(response.data);
      fecharModalMaterial();
    } catch (err) {
      alert(
        err.response?.data?.erro ||
          "Erro ao salvar material previsto para esta comarca.",
      );
      console.error("Erro ao salvar material previsto", err);
    }
  };

  const abrirModalFaltantes = (comarca) => {
    const materiaisFaltantes = (comarca.materiais || [])
      .filter((material) => material.materialFaltante)
      .map((material) => material.id);
    setComarcaFaltantesAtual(comarca);
    setFaltantesForm({
      faltouMaterial: Boolean(comarca.faltouMaterial || materiaisFaltantes.length),
      materialItemIds: materiaisFaltantes,
      descricao:
        comarca.descricaoMaterialFaltante ||
        (comarca.materiais || []).find((material) => material.descricaoFaltante)
          ?.descricaoFaltante ||
        "",
    });
    setShowFaltantesModal(true);
  };

  const fecharModalFaltantes = () => {
    setShowFaltantesModal(false);
    setComarcaFaltantesAtual(null);
    setFaltantesForm({ faltouMaterial: false, materialItemIds: [], descricao: "" });
  };

  const toggleMaterialFaltante = (materialId) => {
    setFaltantesForm((prev) => {
      const selecionados = prev.materialItemIds.includes(materialId)
        ? prev.materialItemIds.filter((id) => id !== materialId)
        : [...prev.materialItemIds, materialId];
      return { ...prev, materialItemIds: selecionados };
    });
  };

  const handleSalvarFaltantes = async (e) => {
    e.preventDefault();
    if (!comarcaFaltantesAtual) return;
    if (
      faltantesForm.faltouMaterial &&
      (!faltantesForm.descricao.trim() || faltantesForm.materialItemIds.length === 0)
    ) {
      alert("Selecione ao menos um material e descreva o que está faltando.");
      return;
    }

    try {
      const response = await api.patch(
        `/comarcas/${comarcaFaltantesAtual.id}/materiais-faltantes`,
        {
          faltouMaterial: faltantesForm.faltouMaterial,
          materialItemIds: faltantesForm.faltouMaterial
            ? faltantesForm.materialItemIds
            : [],
          descricao: faltantesForm.faltouMaterial
            ? faltantesForm.descricao.trim()
            : "",
        },
      );
      atualizarComarcaNaLista(response.data);
      fecharModalFaltantes();
    } catch (err) {
      alert(
        err.response?.data?.erro ||
          "Erro ao salvar controle de materiais faltantes.",
      );
      console.error(err);
    }
  };

  const toDatetimeLocal = (value) => {
    if (!value) return "";
    return String(value).slice(0, 16);
  };

  const fromDatetimeLocal = (value) => (value ? `${value}:00` : null);

  const formatarDataHora = (value) => {
    if (!value) return "-";
    return new Date(value).toLocaleString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const abrirModalTimeline = (material) => {
    setTimelineMaterialAtual(material);
    setTimelineForm({
      dataHoraSolicitacao: toDatetimeLocal(material.dataHoraSolicitacao),
      dataHoraRetirada: toDatetimeLocal(material.dataHoraRetirada),
      dataHoraUso: toDatetimeLocal(material.dataHoraUso),
    });
    setShowTimelineModal(true);
  };

  const fecharModalTimeline = () => {
    setShowTimelineModal(false);
    setTimelineMaterialAtual(null);
    setTimelineForm({
      dataHoraSolicitacao: "",
      dataHoraRetirada: "",
      dataHoraUso: "",
    });
  };

  const handleSalvarTimeline = async (e) => {
    e.preventDefault();
    if (!timelineMaterialAtual) return;

    try {
      const response = await api.patch(
        `/comarcas/materiais-previstos/${timelineMaterialAtual.id}/timeline`,
        {
          dataHoraSolicitacao: fromDatetimeLocal(timelineForm.dataHoraSolicitacao),
          dataHoraRetirada: fromDatetimeLocal(timelineForm.dataHoraRetirada),
          dataHoraUso: fromDatetimeLocal(timelineForm.dataHoraUso),
        },
      );
      atualizarComarcaNaLista(response.data);
      fecharModalTimeline();
    } catch (err) {
      alert(err.response?.data?.erro || "Erro ao salvar timeline do material.");
      console.error(err);
    }
  };

  const getViradaForm = (comarca) => ({
      provasFuncionamento: comarca.viradaRedeProvasFuncionamento || "",
      checklist: comarca.viradaRedeChecklist || "",
      concluida: Boolean(comarca.viradaRedeConcluida),
      ...(viradaForms[comarca.id] || {}),
    });

  const setViradaFormValue = (comarcaId, campo, valor) => {
    setViradaForms((prev) => ({
      ...prev,
      [comarcaId]: {
        ...(prev[comarcaId] || {}),
        [campo]: valor,
      },
    }));
  };

  const handleSalvarViradaRede = async (comarca) => {
    const form = getViradaForm(comarca);

    if (
      form.concluida &&
      !form.provasFuncionamento &&
      !comarca.viradaRedeProvasFuncionamento
    ) {
      alert("Envie a foto da prova de funcionamento antes de concluir.");
      return;
    }

    try {
      const response = await api.patch(`/comarcas/${comarca.id}/virada-rede`, form);
      atualizarComarcaNaLista(response.data);
      setViradaForms((prev) => {
        const next = { ...prev };
        delete next[comarca.id];
        return next;
      });
    } catch (err) {
      alert(err.response?.data?.erro || "Erro ao salvar Virada de Rede.");
      console.error(err);
    }
  };

  const handleRemoverMaterialPrevisto = async (material) => {
    if (!material?.id) return;
    const confirmar = window.confirm(
      `Remover o material previsto "${material.nomeMaterial}" desta comarca?`,
    );
    if (!confirmar) return;

    try {
      const response = await api.delete(
        `/comarcas/materiais-previstos/${material.id}`,
      );
      atualizarComarcaNaLista(response.data);
    } catch (err) {
      alert(
        err.response?.data?.erro ||
          "Erro ao remover material previsto desta comarca.",
      );
      console.error("Erro ao remover material previsto", err);
    }
  };

  const handleProvaViradaRedeChange = async (e, comarcaId) => {
    const file = e.target.files?.[0];

    if (!file) return;

    if (!["image/jpeg", "image/png"].includes(file.type)) {
      alert("Selecione uma imagem válida nos formatos JPEG ou PNG.");
      e.target.value = "";
      return;
    }

    const payload = new FormData();
    payload.append("foto", file);

    try {
      const response = await api.post(
        `/comarcas/${comarcaId}/virada-rede/prova`,
        payload,
        {
          headers: { "Content-Type": "multipart/form-data" },
        },
      );

      atualizarComarcaNaLista(response.data);
      setViradaForms((prev) => {
        const next = { ...prev };
        delete next[comarcaId];
        return next;
      });
    } catch (err) {
      alert(
        err.response?.data?.erro ||
          "Não foi possível salvar a prova da Virada de Rede.",
      );
      console.error(err);
    } finally {
      e.target.value = "";
    }
  };

  const handleFotoVistoriaChange = async (e, comarcaId) => {
    const file = e.target.files?.[0];

    if (!file) return;

    if (!["image/jpeg", "image/png"].includes(file.type)) {
      alert("Selecione uma imagem válida nos formatos JPEG ou PNG.");
      e.target.value = "";
      return;
    }

    const payload = new FormData();
    payload.append("foto", file);

    try {
      const response = await api.post(
        `/comarcas/${comarcaId}/vistoria/foto`,
        payload,
        {
          headers: { "Content-Type": "multipart/form-data" },
        },
      );

      atualizarComarcaNaLista(response.data);
      setFotosVistoria((prev) => {
        if (prev[comarcaId]?.previewUrl?.startsWith("blob:")) {
          URL.revokeObjectURL(prev[comarcaId].previewUrl);
        }

        const next = { ...prev };
        delete next[comarcaId];
        fotosVistoriaRef.current = next;
        return next;
      });
    } catch (err) {
      alert(
        err.response?.data?.erro ||
          "Não foi possível salvar a foto da vistoria no servidor.",
      );
      console.error(err);
    } finally {
      e.target.value = "";
    }
  };

  const abrirModalAssinatura = (comarca) => {
    setComarcaAssinaturaAtual(comarca);
    setShowAssinaturaModal(true);
  };

  const fecharModalAssinatura = () => {
    setShowAssinaturaModal(false);
    setComarcaAssinaturaAtual(null);
    setAssinaturaEmEdicao(false);
  };

  const getAssinaturaPoint = (event) => {
    const canvas = assinaturaCanvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const sourceEvent = event.touches?.[0] || event;

    return {
      x: sourceEvent.clientX - rect.left,
      y: sourceEvent.clientY - rect.top,
    };
  };

  const iniciarAssinatura = (event) => {
    event.preventDefault();
    const canvas = assinaturaCanvasRef.current;
    const context = canvas.getContext("2d");
    const point = getAssinaturaPoint(event);

    assinandoRef.current = true;
    context.beginPath();
    context.moveTo(point.x, point.y);
  };

  const desenharAssinatura = (event) => {
    if (!assinandoRef.current) return;

    event.preventDefault();
    const canvas = assinaturaCanvasRef.current;
    const context = canvas.getContext("2d");
    const point = getAssinaturaPoint(event);

    context.lineTo(point.x, point.y);
    context.stroke();
    setAssinaturaEmEdicao(true);
  };

  const finalizarAssinatura = () => {
    assinandoRef.current = false;
  };

  const limparAssinatura = () => {
    const canvas = assinaturaCanvasRef.current;
    const context = canvas.getContext("2d");
    const { width, height } = canvas.getBoundingClientRect();

    context.clearRect(0, 0, width, height);
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, width, height);
    setAssinaturaEmEdicao(false);
  };

  const confirmarAssinatura = async () => {
    if (!comarcaAssinaturaAtual) return;

    if (!assinaturaEmEdicao) {
      alert("Desenhe a assinatura antes de confirmar.");
      return;
    }

    const canvas = assinaturaCanvasRef.current;
    const assinaturaBase64 = canvas.toDataURL("image/png");

    try {
      const response = await api.patch(
        `/comarcas/${comarcaAssinaturaAtual.id}/vistoria/assinatura`,
        { assinaturaBase64 },
      );

      atualizarComarcaNaLista(response.data);
      setAssinaturasVistoria((prev) => {
        const next = { ...prev };
        delete next[comarcaAssinaturaAtual.id];
        return next;
      });
      fecharModalAssinatura();
    } catch (err) {
      alert(
        err.response?.data?.erro ||
          "Não foi possível salvar a assinatura no servidor.",
      );
      console.error(err);
    }
  };

  const getPercentualConcluido = (comarca) => {
    const progressoVistoria =
      (temFotoVistoria(comarca) ? 50 : 0) +
      (temAssinaturaVistoria(comarca) ? 50 : 0);

    if (!comarca.etapaAtual || comarca.etapaAtual === 1) {
      return progressoVistoria;
    }

    if (comarca.etapaAtual === 3) {
      return comarca.viradaRedeConcluida
        ? 100
        : Math.max(comarca.percentualConcluido ?? 0, 85);
    }

    return Math.min(comarca.percentualConcluido ?? progressoVistoria, 100);
  };

  const getProgressColor = (percentage) => {
    if (percentage >= 80) return "bg-green-500";
    if (percentage >= 50) return "bg-yellow-500";
    return "bg-red-500";
  };

  const materialEstoqueSelecionado = materiaisEstoque.find(
    (material) => String(material.id) === String(materialForm.materialId),
  );
  const quantidadeLivreMaterialSelecionado = materialEstoqueSelecionado
    ? Math.max(
        0,
        (materialEstoqueSelecionado.quantidadeDisponivel ?? 0) -
          (materialEstoqueSelecionado.quantidadeReservada ?? 0),
      )
    : 0;

  if (loading) return <LoadingSpinner />;
  if (error) return <Alert type="error" message={error} />;

  return (
    <div>
      <div className="mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-800">
            Gestão de Comarcas
          </h1>
          <p className="text-slate-600 mt-2">
            Monitore o progresso, vistorias obrigatórias e liberação de
            infraestrutura regional.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {comarcas.map((comarca) => {
          const materiaisPrevistos = Array.isArray(comarca.materiais)
            ? comarca.materiais
            : [];
          const totalMateriaisPrevistos = materiaisPrevistos.reduce(
            (total, material) => total + (material.quantidadePrevista || 0),
            0,
          );
          const totalMateriaisAuditados = materiaisPrevistos.reduce(
            (total, material) => total + (material.quantidadeAuditada || 0),
            0,
          );
          const fotoVistoria = fotosVistoria[comarca.id];
          const assinaturaValida = assinaturasVistoria[comarca.id];
          const fotoVistoriaPreview =
            fotoVistoria?.previewUrl || getArquivoUrl(comarca.fotoVistoriaUrl);
          const assinaturaPreview =
            assinaturaValida?.base64 || comarca.assinaturaBase64;
          const fotoVistoriaConcluida = !!fotoVistoriaPreview;
          const assinaturaConcluida = !!assinaturaPreview;
          const percentualConcluido = getPercentualConcluido(comarca);
          const etapaAtual = comarca.etapaAtual || 1;
          const materiaisFaltantes = materiaisPrevistos.filter(
            (material) => material.materialFaltante,
          );
          const viradaForm = getViradaForm(comarca);

          return (
          <div
            key={comarca.id}
            className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow border border-slate-200 overflow-hidden flex flex-col justify-between"
          >
            <div
              className={`p-6 flex-1 space-y-4 ${comarca.pendencias ? "bg-red-50 border-l-4 border-l-red-500" : "bg-white"}`}
            >
              {/* Topo do Card: Identificador Único da OS + Stepper Visual de Linha do Tempo */}
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <h3 className="text-xl font-bold text-slate-800">
                    {comarca.nomeComarca}
                  </h3>
                  <div className="mt-1 inline-flex items-center gap-1 text-xs font-mono font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded border border-blue-100">
                    <FileText size={12} /> OS:{" "}
                    {comarca.ordemServico?.numeroOs || `OS-2026-0${comarca.id}`}
                  </div>
                </div>

                {/* Indicador de Passos / Status do Fluxo Linear */}
                <div className="flex flex-wrap items-center gap-1 bg-slate-100 p-1 rounded-lg text-[10px] font-black uppercase tracking-wider">
                  <span
                    className={`px-2 py-0.5 rounded ${etapaAtual === 1 ? "bg-amber-500 text-white shadow-sm" : "bg-slate-200 text-slate-500"}`}
                  >
                    1. Vistoria
                  </span>
                  <ChevronRight size={10} className="text-slate-400" />
                  <span
                    className={`px-2 py-0.5 rounded ${etapaAtual === 2 ? "bg-emerald-500 text-white shadow-sm" : "bg-slate-200 text-slate-500"}`}
                  >
                    2. Infra
                  </span>
                  <ChevronRight size={10} className="text-slate-400" />
                  <span
                    className={`px-2 py-0.5 rounded ${etapaAtual === 3 ? "bg-blue-600 text-white shadow-sm" : "bg-slate-200 text-slate-500"}`}
                  >
                    3. Virada
                  </span>
                </div>
              </div>

              {/* Dados Estruturados de Campo */}
              <div className="space-y-2 text-sm text-slate-700">
                <div className="flex items-start gap-3">
                  <MapPin
                    size={18}
                    className="text-blue-500 mt-0.5 flex-shrink-0"
                  />
                  <div>
                    <p className="text-xs font-bold text-slate-400 uppercase">
                      Endereço
                    </p>
                    <p className="text-slate-800 font-medium">
                      {comarca.endereco}
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <User
                    size={18}
                    className="text-purple-500 mt-0.5 flex-shrink-0"
                  />
                  <div>
                    <p className="text-xs font-bold text-slate-400 uppercase">
                      Juiz Responsável
                    </p>
                    <p className="text-slate-800 font-medium">
                      {comarca.juizResponsavel}
                    </p>
                  </div>
                </div>

                {/* Painel de Previsão de Materiais / Insumos Planejados */}
                <div className="flex items-start gap-3 pt-1">
                  <Zap
                    size={18}
                    className="text-yellow-500 mt-0.5 flex-shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="text-xs font-bold text-slate-400 uppercase">
                        Painel de Previsão de Materiais
                      </p>
                      <div className="flex flex-wrap gap-1">
                        <button
                          type="button"
                          onClick={() => abrirModalFaltantes(comarca)}
                          className="inline-flex items-center gap-1 rounded-md border border-amber-200 bg-amber-50 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-amber-700 hover:bg-amber-100"
                        >
                          <AlertTriangle size={12} />
                          Faltantes
                        </button>
                        <button
                          type="button"
                          onClick={() => abrirModalItemAdicional(comarca)}
                          className="inline-flex items-center gap-1 rounded-md border border-emerald-200 bg-emerald-50 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-emerald-700 hover:bg-emerald-100"
                        >
                          <Plus size={12} />
                          Item adicional
                        </button>
                        <button
                          type="button"
                          onClick={() => abrirModalMaterial(comarca)}
                          className="inline-flex items-center gap-1 rounded-md border border-blue-200 bg-blue-50 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-blue-700 hover:bg-blue-100"
                        >
                          <Plus size={12} />
                          Adicionar
                        </button>
                      </div>
                    </div>
                    {materiaisPrevistos.length > 0 ? (
                      <div className="mt-2 border border-slate-200 rounded-lg overflow-hidden">
                        <div className="bg-slate-50 px-3 py-2 flex items-center justify-between gap-2 text-xs font-bold text-slate-600">
                          <span>{materiaisPrevistos.length} itens previstos</span>
                          <span>
                            {totalMateriaisPrevistos} prev. /{" "}
                            {totalMateriaisAuditados} audit.
                          </span>
                        </div>
                        <div className="grid grid-cols-[1.1fr_auto_auto_minmax(8rem,1fr)_auto_auto_auto] gap-3 bg-white px-3 py-2 text-[10px] font-black uppercase tracking-wide text-slate-400 border-b border-slate-100">
                          <span>Material</span>
                          <span>Prev.</span>
                          <span>Aud.</span>
                          <span>O que está faltando</span>
                          <span>Status</span>
                          <span>Timeline</span>
                          <span>Ações</span>
                        </div>
                        <div className="divide-y divide-slate-100">
                          {materiaisPrevistos.map((material) => (
                            <div
                              key={material.id || material.nomeMaterial}
                              className="px-3 py-2 grid grid-cols-[1.1fr_auto_auto_minmax(8rem,1fr)_auto_auto_auto] gap-3 items-center text-xs"
                            >
                              <span className="font-semibold text-slate-700 truncate">
                                {material.nomeMaterial || "Material sem nome"}
                                {material.itemAdicional && (
                                  <span className="ml-1 rounded bg-emerald-50 px-1.5 py-0.5 text-[9px] font-bold uppercase text-emerald-700">
                                    Adicional
                                  </span>
                                )}
                              </span>
                              <span className="text-slate-500">
                                Prev.:{" "}
                                <strong className="text-slate-800">
                                  {material.quantidadePrevista || 0}
                                </strong>
                              </span>
                              <span className="text-slate-500">
                                Aud.:{" "}
                                <strong className="text-slate-800">
                                  {material.quantidadeAuditada || 0}
                                </strong>
                              </span>
                              <span className="text-slate-500 whitespace-normal">
                                {material.materialFaltante ? (
                                  <span className="rounded bg-amber-50 px-2 py-0.5 text-[10px] font-bold text-amber-800">
                                    {material.descricaoFaltante ||
                                      comarca.descricaoMaterialFaltante ||
                                      "Material faltante"}
                                  </span>
                                ) : (
                                  <span className="text-slate-300">-</span>
                                )}
                              </span>
                              <span className="inline-flex flex-wrap gap-1">
                              {material.estoqueReservado && !material.estoqueBaixado && (
                                <span className="rounded bg-blue-50 px-2 py-0.5 text-[10px] font-bold uppercase text-blue-700">
                                  Reservado
                                </span>
                              )}
                              {material.estoqueBaixado && (
                                <span className="rounded bg-emerald-50 px-2 py-0.5 text-[10px] font-bold uppercase text-emerald-700">
                                  Baixado
                                </span>
                              )}
                              </span>
                              <button
                                type="button"
                                onClick={() => abrirModalTimeline(material)}
                                className="rounded border border-slate-200 px-2 py-1 text-[10px] font-bold uppercase text-slate-600 hover:bg-slate-50"
                              >
                                Horários
                              </button>
                              <span className="inline-flex items-center gap-1">
                                <button
                                  type="button"
                                  onClick={() =>
                                    abrirModalMaterial(comarca, material)
                                  }
                                  disabled={material.estoqueBaixado}
                                  className="p-1 rounded text-slate-500 hover:bg-slate-100 hover:text-blue-600"
                                  title="Editar material previsto"
                                >
                                  <Edit2 size={12} />
                                </button>
                                <button
                                  type="button"
                                  onClick={() =>
                                    handleRemoverMaterialPrevisto(material)
                                  }
                                  disabled={material.estoqueBaixado}
                                  className="p-1 rounded text-slate-500 hover:bg-rose-50 hover:text-rose-600"
                                  title="Remover material previsto"
                                >
                                  <Trash2 size={12} />
                                </button>
                              </span>
                            </div>
                          ))}
                        </div>
                        <div className="border-t border-slate-100 bg-slate-50 px-3 py-2">
                          <p className="mb-2 text-[10px] font-black uppercase tracking-wide text-slate-400">
                            Timeline logística
                          </p>
                          <div className="space-y-1">
                            {materiaisPrevistos.map((material) => (
                              <div
                                key={`timeline-${material.id || material.nomeMaterial}`}
                                className="grid grid-cols-[1fr_auto_auto_auto] gap-2 text-[10px] text-slate-500"
                              >
                                <span className="font-semibold text-slate-700 truncate">
                                  {material.nomeMaterial}
                                </span>
                                <span>Solic.: {formatarDataHora(material.dataHoraSolicitacao)}</span>
                                <span>Ret.: {formatarDataHora(material.dataHoraRetirada)}</span>
                                <span>Uso: {formatarDataHora(material.dataHoraUso)}</span>
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    ) : (
                      <p className="mt-2 rounded-lg border border-dashed border-slate-200 bg-slate-50 px-3 py-3 text-xs text-slate-500">
                        Nenhum material previsto cadastrado.{" "}
                        {comarca.quantidadePontos ?? 0} pontos previstos para
                        instalação física.
                      </p>
                    )}
                  </div>
                </div>
              </div>

              {/* 🛑 SEÇÃO DE VISTORIA COM GERENTE COM CARDS DE UPLOAD/ASSINATURA (ETAPA 1) */}
              {(!comarca.etapaAtual || comarca.etapaAtual === 1) && (
                <div className="bg-amber-50/40 border border-amber-200/70 rounded-xl p-3 grid grid-cols-2 gap-3 text-xs font-bold tracking-wide">
                  <label
                    className={`border border-dashed p-3 rounded-lg text-center cursor-pointer transition flex flex-col items-center justify-center gap-2 ${fotoVistoriaConcluida ? "bg-emerald-50 border-emerald-300 text-emerald-800" : "bg-white hover:border-blue-400 text-slate-500"}`}
                  >
                    <input
                      type="file"
                      accept="image/jpeg,image/png"
                      className="hidden"
                      onChange={(e) => handleFotoVistoriaChange(e, comarca.id)}
                    />
                    <span className="flex items-center justify-center gap-1">
                      <Upload size={14} />{" "}
                      {fotoVistoriaConcluida
                        ? "Foto Carregada"
                        : "Fazer Upload Foto"}
                    </span>
                    {fotoVistoriaPreview && (
                      <img
                        src={fotoVistoriaPreview}
                        alt={`Preview da vistoria ${fotoVistoria?.file?.name || "salva"}`}
                        className="h-16 w-full rounded-md object-cover border border-emerald-200"
                      />
                    )}
                  </label>
                  <div
                    onClick={() => abrirModalAssinatura(comarca)}
                    className={`border border-dashed p-3 rounded-lg text-center cursor-pointer transition flex flex-col items-center justify-center gap-2 ${assinaturaConcluida ? "bg-emerald-50 border-emerald-300 text-emerald-800" : "bg-white hover:border-blue-400 text-slate-500"}`}
                  >
                    <span className="flex items-center justify-center gap-1">
                      <PenTool size={14} />{" "}
                      {assinaturaConcluida
                        ? "Termo Assinado"
                        : "Coletar Assinatura"}
                    </span>
                    {assinaturaPreview && (
                      <img
                        src={assinaturaPreview}
                        alt="Preview da assinatura coletada"
                        className="h-16 w-full rounded-md object-contain border border-emerald-200 bg-white"
                      />
                    )}
                  </div>
                </div>
              )}

              {etapaAtual === 3 && (
                <div className="rounded-xl border border-blue-200 bg-blue-50/40 p-3 space-y-3">
                  <div className="flex items-center justify-between gap-2">
                    <div>
                      <p className="text-xs font-black uppercase tracking-wide text-blue-800">
                        3. Virada de Rede
                      </p>
                      <p className="text-xs text-blue-700">
                        Provas de funcionamento, conectividade e validação final.
                      </p>
                    </div>
                    {comarca.viradaRedeConcluida && (
                      <span className="inline-flex items-center gap-1 rounded bg-emerald-100 px-2 py-1 text-[10px] font-bold uppercase text-emerald-700">
                        <CheckCircle2 size={12} /> Concluída
                      </span>
                    )}
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                        Provas de funcionamento
                      </label>
                      <label
                        className={`border border-dashed p-3 rounded-lg text-center cursor-pointer transition flex flex-col items-center justify-center gap-2 text-xs font-bold ${
                          comarca.viradaRedeProvasFuncionamento
                            ? "bg-emerald-50 border-emerald-300 text-emerald-800"
                            : "bg-white hover:border-blue-400 text-slate-500"
                        }`}
                      >
                        <input
                          type="file"
                          accept="image/jpeg,image/png"
                          className="hidden"
                          onChange={(e) =>
                            handleProvaViradaRedeChange(e, comarca.id)
                          }
                        />
                        <span className="flex items-center justify-center gap-1">
                          <Upload size={14} />
                          {comarca.viradaRedeProvasFuncionamento
                            ? "Prova Carregada"
                            : "Enviar Foto da Prova"}
                        </span>
                        {comarca.viradaRedeProvasFuncionamento && (
                          <img
                            src={getArquivoUrl(
                              comarca.viradaRedeProvasFuncionamento,
                            )}
                            alt={`Prova de funcionamento ${comarca.nomeComarca}`}
                            className="h-24 w-full rounded-md object-cover border border-emerald-200"
                          />
                        )}
                      </label>
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold uppercase text-slate-500 mb-1">
                        Checklist de validação
                      </label>
                      <textarea
                        rows="3"
                        value={viradaForm.checklist}
                        onChange={(e) =>
                          setViradaFormValue(comarca.id, "checklist", e.target.value)
                        }
                        className="w-full rounded-lg border border-blue-100 px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-blue-400"
                        placeholder="Ex: ping ok, rota ok, link ativo, acesso validado..."
                      />
                    </div>
                  </div>
                  <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
                    <label className="inline-flex items-center gap-2 text-xs font-bold text-slate-700">
                      <input
                        type="checkbox"
                        checked={viradaForm.concluida}
                        onChange={(e) =>
                          setViradaFormValue(comarca.id, "concluida", e.target.checked)
                        }
                      />
                      Virada de Rede concluída
                    </label>
                    <button
                      type="button"
                      onClick={() => handleSalvarViradaRede(comarca)}
                      className="rounded-lg bg-blue-600 px-3 py-2 text-xs font-bold text-white hover:bg-blue-700"
                    >
                      Salvar Virada
                    </button>
                  </div>
                </div>
              )}

              {/* Barra de Progresso Operacional */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <p className="text-xs font-bold text-slate-400 uppercase">
                    Progresso de Conclusão
                  </p>
                  <span className="text-sm font-bold text-slate-700">
                    {percentualConcluido}%
                  </span>
                </div>
                <div className="w-full bg-slate-200 rounded-full h-3 overflow-hidden">
                  <div
                    className={`h-full transition-all duration-300 ${getProgressColor(percentualConcluido)}`}
                    style={{
                      width: `${Math.min(percentualConcluido, 100)}%`,
                    }}
                  />
                </div>
              </div>

              {comarca.pendencias && (
                <div className="bg-red-100 border border-red-300 rounded-lg p-3 text-xs text-red-800 flex items-center gap-2">
                  <AlertTriangle size={14} className="flex-shrink-0" />
                  <p>
                    <span className="font-bold">Pendência:</span>{" "}
                    {comarca.pendencias}
                  </p>
                </div>
              )}

              {materiaisFaltantes.length > 0 && (
                <div className="bg-amber-100 border border-amber-300 rounded-lg p-3 text-xs text-amber-900 flex items-start gap-2">
                  <AlertTriangle size={14} className="flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="font-bold">O que está faltando</p>
                    <p>{comarca.descricaoMaterialFaltante}</p>
                    <p className="mt-1 text-[10px] font-bold uppercase tracking-wide">
                      {materiaisFaltantes
                        .map((material) => material.nomeMaterial)
                        .join(", ")}
                    </p>
                  </div>
                </div>
              )}
            </div>

            {/* Ações dinâmicas com trava de avanço baseada na etapa */}
            <div className="bg-slate-50 px-6 py-3 border-t border-slate-200 flex flex-col sm:flex-row gap-2">
              {etapaAtual === 1 ? (
                <button
                  onClick={() => handleAvancarFase(comarca)}
                  className="flex-1 bg-amber-600 hover:bg-amber-700 text-white font-bold py-2 px-4 rounded-lg text-xs transition-colors shadow-sm uppercase tracking-wider"
                >
                  Homologar Vistoria e Liberar Obras
                </button>
              ) : etapaAtual === 2 ? (
                <button
                  onClick={() => handleAvancarFase(comarca)}
                  className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded-lg text-xs transition-colors shadow-sm uppercase tracking-wider"
                >
                  Liberar Virada de Rede
                </button>
              ) : (
                <button
                  onClick={() => handleOpenModal(comarca)}
                  className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg text-xs transition-colors shadow-sm"
                >
                  Registrar Pendência/Ajustar Progresso
                </button>
              )}
              <button
                onClick={() => setComarcaHistorico(comarca)}
                className="flex-shrink-0 flex items-center justify-center gap-1 bg-white hover:bg-slate-100 border text-slate-700 font-bold py-2 px-4 rounded-lg text-xs transition-colors"
              >
                <History size={14} />
                Histórico
              </button>
            </div>
          </div>
          );
        })}
      </div>

      {/* Modal Ajustar Progresso */}
      <Modal
        isOpen={showModal}
        onClose={handleCloseModal}
        title={`Ajustar Progresso - ${selectedComarca?.nomeComarca}`}
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Percentual de Conclusão (%)
            </label>
            <div className="flex items-center gap-4">
              <input
                type="number"
                name="percentualConcluido"
                value={formData.percentualConcluido}
                onChange={handleInputChange}
                min="0"
                max="100"
                className="flex-1 px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <span className="text-slate-700 font-semibold">%</span>
            </div>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Pendências/Observações
            </label>
            <textarea
              name="pendencias"
              value={formData.pendencias}
              onChange={handleInputChange}
              rows="4"
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
              placeholder="Descreva qualquer detalhe em campo..."
            />
          </div>
          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={handleCloseModal}
              className="px-5 py-2 border rounded-lg text-slate-700 font-semibold hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors"
            >
              Salvar Alterações
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={showMaterialModal}
        onClose={fecharModalMaterial}
        title={`${materialEmEdicao ? "Editar" : materialForm.itemAdicional ? "Adicionar Item Adicional" : "Adicionar Material Previsto"} - ${comarcaMaterialAtual?.nomeComarca || ""}`}
      >
        <form onSubmit={handleSalvarMaterialPrevisto} className="space-y-5">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Material do Estoque *
            </label>
            <select
              name="materialId"
              required
              value={materialForm.materialId}
              onChange={handleMaterialFormChange}
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Selecione um material cadastrado</option>
              {materiaisEstoque.map((material) => (
                <option key={material.id} value={material.id}>
                  {material.nome}{" "}
                  {material.partNumber ? `- ${material.partNumber}` : ""}{" "}
                  (
                  {Math.max(
                    0,
                    (material.quantidadeDisponivel ?? 0) -
                      (material.quantidadeReservada ?? 0),
                  )}{" "}
                  livre)
                </option>
              ))}
            </select>
            {materialEstoqueSelecionado && (
              <div className="mt-2 grid grid-cols-3 gap-2 text-xs">
                <span className="rounded bg-slate-50 px-2 py-1 text-slate-500">
                  Total:{" "}
                  <strong className="text-slate-800">
                    {materialEstoqueSelecionado.quantidadeDisponivel ?? 0}
                  </strong>
                </span>
                <span className="rounded bg-blue-50 px-2 py-1 text-blue-700">
                  Reservado:{" "}
                  <strong>{materialEstoqueSelecionado.quantidadeReservada ?? 0}</strong>
                </span>
                <span className="rounded bg-emerald-50 px-2 py-1 text-emerald-700">
                  Livre: <strong>{quantidadeLivreMaterialSelecionado}</strong>
                </span>
              </div>
            )}
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">
              Quantidade Prevista *
            </label>
            <input
              type="number"
              name="quantidadePrevista"
              required
              min="1"
              step="1"
              value={materialForm.quantidadePrevista}
              onChange={handleMaterialFormChange}
              className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="0"
            />
          </div>

          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={fecharModalMaterial}
              className="px-5 py-2 border rounded-lg text-slate-700 font-semibold hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors"
            >
              {materialEmEdicao
                ? "Salvar Material"
                : materialForm.itemAdicional
                  ? "Adicionar Item Adicional"
                  : "Adicionar Material"}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={showFaltantesModal}
        onClose={fecharModalFaltantes}
        title={`Materiais Faltantes - ${comarcaFaltantesAtual?.nomeComarca || ""}`}
      >
        <form onSubmit={handleSalvarFaltantes} className="space-y-5">
          <label className="inline-flex items-center gap-2 text-sm font-bold text-slate-700">
            <input
              type="checkbox"
              checked={faltantesForm.faltouMaterial}
              onChange={(e) =>
                setFaltantesForm((prev) => ({
                  ...prev,
                  faltouMaterial: e.target.checked,
                  materialItemIds: e.target.checked ? prev.materialItemIds : [],
                  descricao: e.target.checked ? prev.descricao : "",
                }))
              }
            />
            Faltou material?
          </label>

          {faltantesForm.faltouMaterial && (
            <>
              <div>
                <p className="block text-sm font-semibold text-slate-700 mb-2">
                  Materiais que faltaram *
                </p>
                <div className="max-h-48 overflow-auto rounded-lg border border-slate-200 divide-y divide-slate-100">
                  {(comarcaFaltantesAtual?.materiais || []).map((material) => (
                    <label
                      key={material.id}
                      className="flex items-center justify-between gap-3 px-3 py-2 text-sm"
                    >
                      <span className="font-medium text-slate-700">
                        {material.nomeMaterial}
                      </span>
                      <input
                        type="checkbox"
                        checked={faltantesForm.materialItemIds.includes(material.id)}
                        onChange={() => toggleMaterialFaltante(material.id)}
                      />
                    </label>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Descrição detalhada *
                </label>
                <textarea
                  rows="4"
                  required
                  value={faltantesForm.descricao}
                  onChange={(e) =>
                    setFaltantesForm((prev) => ({
                      ...prev,
                      descricao: e.target.value,
                    }))
                  }
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-amber-500 resize-none"
                  placeholder="Descreva exatamente o que faltou, quantidade, impacto e providência necessária..."
                />
              </div>
            </>
          )}

          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={fecharModalFaltantes}
              className="px-5 py-2 border rounded-lg text-slate-700 font-semibold hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-5 py-2 bg-amber-600 hover:bg-amber-700 text-white font-semibold rounded-lg transition-colors"
            >
              Salvar Faltantes
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={showTimelineModal}
        onClose={fecharModalTimeline}
        title={`Timeline - ${timelineMaterialAtual?.nomeMaterial || ""}`}
      >
        <form onSubmit={handleSalvarTimeline} className="space-y-5">
          {[
            ["dataHoraSolicitacao", "Data/Hora de Solicitação"],
            ["dataHoraRetirada", "Data/Hora de Retirada"],
            ["dataHoraUso", "Data/Hora de Uso"],
          ].map(([campo, label]) => (
            <div key={campo}>
              <label className="block text-sm font-semibold text-slate-700 mb-2">
                {label}
              </label>
              <input
                type="datetime-local"
                value={timelineForm[campo]}
                onChange={(e) =>
                  setTimelineForm((prev) => ({
                    ...prev,
                    [campo]: e.target.value,
                  }))
                }
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          ))}

          <div className="flex gap-3 justify-end">
            <button
              type="button"
              onClick={fecharModalTimeline}
              className="px-5 py-2 border rounded-lg text-slate-700 font-semibold hover:bg-slate-50 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-5 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition-colors"
            >
              Salvar Timeline
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={showAssinaturaModal}
        onClose={fecharModalAssinatura}
        title={`Coletar Assinatura - ${comarcaAssinaturaAtual?.nomeComarca || ""}`}
      >
        <div className="space-y-4">
          <canvas
            ref={assinaturaCanvasRef}
            className="w-full rounded-lg border border-slate-300 bg-white touch-none"
            onMouseDown={iniciarAssinatura}
            onMouseMove={desenharAssinatura}
            onMouseUp={finalizarAssinatura}
            onMouseLeave={finalizarAssinatura}
            onTouchStart={iniciarAssinatura}
            onTouchMove={desenharAssinatura}
            onTouchEnd={finalizarAssinatura}
          />

          <div className="flex flex-col sm:flex-row justify-end gap-3">
            <button
              type="button"
              onClick={limparAssinatura}
              className="px-5 py-2 border rounded-lg text-slate-700 font-semibold hover:bg-slate-50 transition-colors"
            >
              Limpar
            </button>
            <button
              type="button"
              onClick={confirmarAssinatura}
              disabled={!assinaturaEmEdicao}
              className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold rounded-lg transition-colors"
            >
              Confirmar
            </button>
          </div>
        </div>
      </Modal>

      {/* Modal Histórico */}
      <Modal
        isOpen={!!comarcaHistorico}
        onClose={() => setComarcaHistorico(null)}
        title={`Atividades - ${comarcaHistorico?.nomeComarca || ""}`}
      >
        {comarcaHistorico && (
          <HistoricoAtividadesComarca comarcaId={comarcaHistorico.id} />
        )}
      </Modal>
    </div>
  );
}
