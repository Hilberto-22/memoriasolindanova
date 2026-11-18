const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

// Exercise the same ordering functions used by the Angular page, without a browser or new dependencies.
const history = {};
const compiled = ts.transpileModule(readFileSync(resolve(__dirname, '../src/shared/city-history.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
});
vm.runInNewContext(compiled.outputText, { exports: history });
const { orderHistory, groupHistory, historyPeriod } = history;
const entry = (id, startDate, period = '', endDate = null) => ({ id, startDate, period, endDate });
const ids = items => items.map(item => item.id).join(',');

test('sorts complete historical dates, including dates within the same year', () => {
  const items = [entry(1, '2000-12-31'), entry(2, '1999-01-01'), entry(3, '2000-01-01')];
  assert.equal(ids(orderHistory(items, 'oldest')), '2,3,1');
  assert.equal(ids(orderHistory(items, 'newest')), '1,3,2');
  assert.equal(ids(items), '1,2,3'); // computed sorting must not mutate the loaded response
});

test('keeps undated information last in both directions', () => {
  const items = [entry(1, null), entry(2, '1990-01-01'), entry(3, null), entry(4, '2000-01-01')];
  assert.equal(ids(orderHistory(items, 'oldest')), '2,4,1,3');
  assert.equal(ids(orderHistory(items, 'newest')), '4,2,1,3');
});

test('orders coincident dates by end date with stable ID tie breaking', () => {
  const items = [entry(3, '1900-01-01'), entry(2, '1900-01-01', '', '1910-01-01'), entry(1, '1900-01-01')];
  assert.equal(ids(orderHistory(items, 'oldest')), '1,3,2');
  assert.equal(ids(orderHistory(items, 'newest')), '2,1,3');
});

test('groups periods by their earliest reference date instead of alphabetical labels', () => {
  const items = [entry(1, '2000-01-01', 'Atualidade'), entry(2, '1800-01-01', 'Século XIX'),
    entry(3, '1850-01-01', 'Século XIX'), entry(4, null, 'Tradição sem data')];
  const oldest = groupHistory(items, 'oldest');
  const newest = groupHistory(items, 'newest');
  assert.equal(oldest.map(group => group.label).join('|'), 'Século XIX|Atualidade|Tradição sem data');
  assert.equal(newest.map(group => group.label).join('|'), 'Atualidade|Século XIX|Tradição sem data');
  assert.equal(ids(newest[1].items), '3,2');
});

test('keeps mixed dated and undated entries together under their named period', () => {
  const groups = groupHistory([entry(1, null, 'Anos 1990'), entry(2, '1995-01-01', 'Anos 1990')], 'newest');
  assert.equal(groups.length, 1);
  assert.equal(groups[0].date, '1995-01-01');
  assert.equal(ids(groups[0].items), '2,1');
});

test('provides period labels for exact dates, ranges and undated information', () => {
  assert.equal(historyPeriod(entry(1, '1900-01-01', 'Século XX')), 'Século XX');
  assert.equal(historyPeriod(entry(1, '1900-01-01')), '1900');
  assert.equal(historyPeriod(entry(1, '1900-01-01', '', '1910-01-01')), '1900–1910');
  assert.equal(historyPeriod(entry(1, null)), 'Sem período informado');
  assert.equal(groupHistory([], 'oldest').length, 0);
});
