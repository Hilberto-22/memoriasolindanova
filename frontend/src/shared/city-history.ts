import type { CityHistoryEntry } from '../api';

export const cityHistoryThemes = [
  { value: 'history', label: 'História da cidade' },
  { value: 'traditions', label: 'Tradições e cultura' },
  { value: 'flag', label: 'Bandeira' },
  { value: 'anthem', label: 'Hino da cidade' },
  { value: 'other', label: 'Outras informações' },
] as const;

export function cityHistoryTheme(category: string) {
  return cityHistoryThemes.find(theme => theme.value === category)?.label ?? 'Outras informações';
}

export function historyPeriod(item: CityHistoryEntry): string {
  if (item.period) return item.period;
  if (!item.startDate) return 'Sem período informado';
  const first = Number(item.startDate.slice(0, 4));
  const last = item.endDate ? Number(item.endDate.slice(0, 4)) : first;
  return last === first ? String(first) : `${first}–${last}`;
}

// ISO dates compare chronologically without timezone conversions. Undated entries stay last in either direction.
export function orderHistory(items: CityHistoryEntry[], direction: string): CityHistoryEntry[] {
  const sign = direction === 'newest' ? -1 : 1;
  return [...items].sort((a, b) => {
    if (!a.startDate || !b.startDate) return Number(!a.startDate) - Number(!b.startDate) || a.id - b.id;
    return sign * (a.startDate.localeCompare(b.startDate)
      || (a.endDate ?? a.startDate).localeCompare(b.endDate ?? b.startDate)) || a.id - b.id;
  });
}

export function groupHistory(items: CityHistoryEntry[], direction: string) {
  const groups = new Map<string, CityHistoryEntry[]>();
  for (const item of items) {
    const label = historyPeriod(item);
    groups.set(label, [...(groups.get(label) ?? []), item]);
  }
  const sign = direction === 'newest' ? -1 : 1;
  return [...groups].map(([label, entries]) => ({
    label, items: orderHistory(entries, direction),
    date: entries.map(item => item.startDate).filter((date): date is string => Boolean(date)).sort()[0] ?? null,
  })).sort((a, b) => {
    if (!a.date || !b.date) return Number(!a.date) - Number(!b.date) || a.label.localeCompare(b.label, 'pt-BR', { numeric: true });
    return sign * a.date.localeCompare(b.date) || a.label.localeCompare(b.label, 'pt-BR', { numeric: true });
  });
}
