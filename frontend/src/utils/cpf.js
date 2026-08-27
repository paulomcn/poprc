export const normalizarCpf = (valor) => String(valor || "").replace(/\D/g, "").slice(0, 11);

export const formatarCpf = (valor) => {
  const digitos = normalizarCpf(valor);
  if (!digitos) return "";
  return digitos
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
};

export const cpfCompleto = (valor) => normalizarCpf(valor).length === 11;
