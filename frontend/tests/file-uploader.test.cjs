const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { resolve } = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

const compiled = ts.transpileModule(readFileSync(resolve(__dirname,
  '../src/features/contributions/file-uploader/file-uploader.component.ts'), 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, experimentalDecorators: true },
});
const components = {};
// Replace Angular's view hooks so the selection handler can run without a browser.
vm.runInNewContext(compiled.outputText, { exports: components, require: () => ({
  Component: () => target => target,
  signal: initial => { let value = initial; const state = () => value; state.set = next => { value = next; }; return state; },
  output: () => ({ value: [], emit(files) { this.value = files; } }),
}) });
const MB = 1024 * 1024;
const file = (name, size, type = '') => ({ name, size, type });
const select = files => {
  const uploader = new components.FileUploader();
  const input = { files, value: 'selection' };
  uploader.select(input);
  return { uploader, input };
};

test('accepts MP4 videos up to 300 MB, including missing MIME types and uppercase extensions', () => {
  for (const video of [file('video.mp4', 300 * MB, 'video/mp4'), file('VIDEO.MP4', 300 * MB)]) {
    const { uploader } = select([video]);
    assert.equal(uploader.error(), '');
    assert.equal(uploader.changed.value[0], video);
  }
});

test('accepts five videos of 300 MB in the same selection', () => {
  const { uploader } = select(Array.from({ length: 5 }, (_, id) => file(`${id}.mp4`, 300 * MB, 'video/mp4')));
  assert.equal(uploader.error(), '');
  assert.equal(uploader.changed.value.length, 5);
});

test('keeps the 20 MB limit for images, documents and audio', () => {
  for (const name of ['foto.jpg', 'documento.pdf', 'audio.mp3']) {
    assert.equal(select([file(name, 20 * MB)]).uploader.error(), '');
    assert.notEqual(select([file(name, 20 * MB + 1)]).uploader.error(), '');
  }
});

test('rejects oversized videos, empty files and more than five files', () => {
  for (const files of [
    [file('video.mp4', 300 * MB + 1, 'video/mp4')],
    [file('vazio.mp4', 0, 'video/mp4')],
    Array.from({ length: 6 }, () => file('video.mp4', MB, 'video/mp4')),
  ]) {
    const { uploader, input } = select(files);
    assert.match(uploader.error(), /300 MB/);
    assert.equal(input.value, '');
    assert.equal(uploader.names().length, 0);
    assert.equal(uploader.changed.value.length, 0);
  }
});

test('clears a previous selection when an oversized video is picked', () => {
  const { uploader } = select([file('valido.mp4', 300 * MB)]);
  uploader.select({files:[file('grande.mp4', 300 * MB + 1)], value:'selection'});
  assert.equal(uploader.changed.value.length, 0);
  assert.equal(uploader.names().length, 0);
});
