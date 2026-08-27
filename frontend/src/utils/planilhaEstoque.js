export const normalizarTextoPlanilha = (valor) =>
  String(valor ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9]+/g, " ")
    .trim()
    .replace(/\s+/g, " ")
    .toLowerCase();

export const valorDaCelula = (celula) => {
  const valor = celula?.value;
  if (valor && typeof valor === "object") {
    if (valor.result != null) return valor.result;
    if (valor.text != null) return valor.text;
    if (Array.isArray(valor.richText)) {
      return valor.richText.map((item) => item.text || "").join("");
    }
  }
  return valor;
};

export const celulaPossuiFormulaSemResultado = (celula) => {
  const valor = celula?.value;
  if (!valor || typeof valor !== "object") return false;
  return Boolean(valor.formula || valor.sharedFormula) && valor.result == null;
};

export const localizarCabecalhoEstoque = (sheet) => {
  const limiteLinhas = Math.min(sheet.rowCount, 20);
  const limiteColunas = Math.min(sheet.columnCount, 15);
  for (let linha = 1; linha <= limiteLinhas; linha += 1) {
    const mapa = {};
    for (let coluna = 1; coluna <= limiteColunas; coluna += 1) {
      const texto = normalizarTextoPlanilha(valorDaCelula(sheet.getCell(linha, coluna)));
      if (texto === "produto" && !mapa.produto) mapa.produto = coluna;
      if ([
        "estoque atual",
        "estoque apos retiradas",
        "quantidade em estoque",
      ].includes(texto) && !mapa.quantidade) mapa.quantidade = coluna;
      if (texto === "valor unitario" && !mapa.custo) mapa.custo = coluna;
      if (texto === "quantidade da retirada" && !mapa.retirada) mapa.retirada = coluna;
      if (texto === "data de retirada" && !mapa.dataRetirada) mapa.dataRetirada = coluna;
      if (texto === "estoque apos retirada" && !mapa.saldoFinal) mapa.saldoFinal = coluna;
    }
    if (mapa.produto && mapa.quantidade && mapa.custo) {
      return { linha, ...mapa };
    }
  }
  return null;
};
