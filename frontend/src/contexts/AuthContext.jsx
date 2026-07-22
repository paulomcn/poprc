import { createContext, useContext, useEffect, useMemo, useRef, useState } from "react";
import api, { refreshCsrfToken, setReauthHandler } from "../services/api";

const AuthContext = createContext(null);

export const homePorPerfil = (perfil) => {
  if (perfil === "TECNICO") return "/tecnico";
  if (perfil === "ESTOQUE") return "/estoque";
  if (perfil === "AUDITOR") return "/auditoria/tecnica";
  return "/";
};

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [configuracao, setConfiguracao] = useState({ securityEnabled: true, devLoginEnabled: false, zohoEnabled: false });
  const [reauthAberta, setReauthAberta] = useState(false);
  const [reauthErro, setReauthErro] = useState("");
  const [reauthProcessando, setReauthProcessando] = useState(false);
  const reauthPromise = useRef(null);

  const carregarUsuario = async (configAtual = configuracao) => {
    try {
      const selecionarUsuario = new URLSearchParams(window.location.search).get("trocar") === "1";
      if (!configAtual.securityEnabled && configAtual.devLoginEnabled && !selecionarUsuario) {
        const usuarios = await api.get("/auth/dev-usuarios");
        const administrador = usuarios.data.find((item) => item.perfil === "ADMIN") || usuarios.data[0];
        if (!administrador) throw new Error("Nenhum usuário de teste ativo foi encontrado.");
        const response = await api.post("/auth/dev-login", { funcionarioId: administrador.id });
        setUsuario(response.data);
        return;
      }
      await api.get("/auth/csrf");
      const response = await api.get("/auth/me");
      setUsuario(response.data);
    } catch (error) {
      if (error.response?.status !== 401) throw error;
      setUsuario(null);
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => {
    api.get("/auth/config")
      .then(async ({ data }) => { setConfiguracao(data); await carregarUsuario(data); })
      .catch(() => setCarregando(false));
    const expirarSessao = () => setUsuario(null);
    window.addEventListener("auth:unauthorized", expirarSessao);
    return () => window.removeEventListener("auth:unauthorized", expirarSessao);
  }, []);

  useEffect(() => {
    setReauthHandler(() => {
      if (reauthPromise.current) return reauthPromise.current.promise;
      let resolve;
      let reject;
      const promise = new Promise((ok, fail) => { resolve = ok; reject = fail; });
      reauthPromise.current = { promise, resolve, reject };
      setReauthErro("");
      setReauthAberta(true);
      return promise;
    });
    return () => setReauthHandler(null);
  }, []);

  const login = async (cpf, senha) => {
    const response = await api.post("/auth/login", { cpf, senha });
    await refreshCsrfToken();
    setUsuario(response.data);
    return response.data;
  };

  const loginDesenvolvimento = async (funcionarioId) => {
    const response = await api.post("/auth/dev-login", { funcionarioId });
    await refreshCsrfToken();
    setUsuario(response.data);
    return response.data;
  };

  const logout = async () => {
    try {
      await refreshCsrfToken();
      await api.post("/auth/logout");
    } finally {
      setUsuario(null);
    }
  };

  const alterarSenha = async (senhaAtual, novaSenha) => {
    await refreshCsrfToken();
    const response = await api.post("/auth/alterar-senha", { senhaAtual, novaSenha });
    if (!response.data?.senhaConfigurada || response.data?.trocaSenhaObrigatoria) {
      throw new Error("O servidor não confirmou a alteração da senha.");
    }
    setUsuario(response.data);
    return response.data;
  };

  const confirmarReautenticacao = async (senha) => {
    setReauthProcessando(true);
    setReauthErro("");
    try {
      await refreshCsrfToken();
      await api.post("/auth/reauth", { senha });
      reauthPromise.current?.resolve();
      reauthPromise.current = null;
      setReauthAberta(false);
    } catch (error) {
      setReauthErro(error.response?.data?.erro || "Senha inválida.");
    } finally {
      setReauthProcessando(false);
    }
  };

  const cancelarReautenticacao = () => {
    reauthPromise.current?.reject(new Error("Operação cancelada pelo usuário."));
    reauthPromise.current = null;
    setReauthAberta(false);
  };

  const value = useMemo(
    () => ({ usuario, carregando, configuracao, login, loginDesenvolvimento, logout, alterarSenha, recarregar: carregarUsuario }),
    [usuario, carregando, configuracao],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      {reauthAberta && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/70 p-4" role="dialog" aria-modal="true">
          <form
            className="w-full max-w-sm rounded border border-slate-200 bg-white p-5 shadow-2xl"
            onSubmit={(event) => { event.preventDefault(); confirmarReautenticacao(event.currentTarget.senha.value); }}
          >
            <h2 className="text-base font-bold text-slate-900">Confirme sua identidade</h2>
            <p className="mt-1 text-sm text-slate-500">Digite sua senha para autorizar esta operação importante.</p>
            <input name="senha" type="password" autoFocus required className="mt-4 w-full rounded border border-slate-300 px-3 py-2 text-sm" placeholder="Senha" />
            {reauthErro && <p className="mt-2 text-sm text-red-700">{reauthErro}</p>}
            <div className="mt-5 flex justify-end gap-2">
              <button type="button" onClick={cancelarReautenticacao} className="rounded border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700">Cancelar</button>
              <button type="submit" disabled={reauthProcessando} className="rounded bg-blue-600 px-3 py-2 text-sm font-semibold text-white disabled:opacity-50">{reauthProcessando ? "Validando..." : "Confirmar"}</button>
            </div>
          </form>
        </div>
      )}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth deve ser usado dentro de AuthProvider");
  return context;
};
