export function vincularComarcasAosProjetos(projetos, comarcas) {
  const porProjeto = new Map(comarcas.filter((comarca) => comarca.projeto?.id)
    .map((comarca) => [String(comarca.projeto.id), comarca]));
  return projetos.map((projeto) => ({
    ...projeto,
    comarca: porProjeto.get(String(projeto.id)) || projeto.comarca,
  }));
}
