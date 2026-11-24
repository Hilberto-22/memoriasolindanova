export const PUBLICATION_CATEGORIES = [
  'Educação',
  'Festas populares',
  'História local',
  'Patrimônio',
  'Tradições e costumes',
];

export const PUBLICATION_USAGE_SUGGESTIONS = [
  'Consultar a equipe do acervo antes de reutilizar.',
  'Reprodução somente mediante autorização dos responsáveis.',
  'Uso educacional permitido com indicação dos créditos.',
];

export function publicationCategories(existing: string[] = []) {
  return [...new Set([...existing, ...PUBLICATION_CATEGORIES])]
    .sort((a, b) => a.localeCompare(b, 'pt-BR'));
}
