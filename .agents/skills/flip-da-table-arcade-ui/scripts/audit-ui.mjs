import { readdir, readFile } from 'node:fs/promises';
import { extname, join, relative, resolve } from 'node:path';

const repoRoot = resolve(import.meta.dirname, '../../../..');
const sourceRoot = join(repoRoot, 'frontend', 'src');
const allowedColorFiles = new Set([join(sourceRoot, 'styles', 'tokens.css')]);
const sourceExtensions = new Set(['.js', '.jsx', '.css']);
const daisyTokens = /^(?:btn|card|input|select|modal|alert|badge|toggle|table|tooltip|avatar|loading)(?:-[a-z0-9]+)?$/;
const largeRadius = /(?:^|\s)rounded-(?:lg|xl|2xl|3xl|full)(?:\s|$)/;
const endlessMotion = /(?:^|\s)animate-(?:spin|ping|pulse|bounce)(?:\s|$)/;
const softShadow = /(?:^|\s)shadow(?:-[^\s]+)?(?:\s|$)/;
const literalColor = /#[0-9a-f]{3,8}\b|\brgba?\s*\(|\bhsla?\s*\(/gi;
const cssShadow = /box-shadow\s*:\s*([^;]+);/gi;
const cssMotion = /(?:animation|transition)(?:-[a-z-]+)?\s*:\s*([^;]+);/gi;

async function walk(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? walk(path) : [path];
  }));
  return nested.flat();
}

function classValues(source) {
  const values = [];
  const matcher = /className\s*=\s*(?:"([^"]*)"|'([^']*)'|\{`([\s\S]*?)`\})/g;
  for (const match of source.matchAll(matcher)) values.push(match[1] ?? match[2] ?? match[3] ?? '');
  return values;
}

const violations = [];
for (const file of await walk(sourceRoot)) {
  if (!sourceExtensions.has(extname(file))) continue;
  const source = await readFile(file, 'utf8');
  const label = relative(repoRoot, file).replaceAll('\\', '/');

  for (const classes of classValues(source)) {
    const tokens = classes.split(/\s+/).map((token) => token.replace(/["'`;{}]/g, '')).filter(Boolean);
    const legacy = tokens.filter((token) => daisyTokens.test(token));
    if (legacy.length) violations.push(`${label}: DaisyUI classes: ${[...new Set(legacy)].join(', ')}`);
    if (largeRadius.test(classes)) violations.push(`${label}: radius exceeds the 4px arcade limit`);
    if (endlessMotion.test(classes)) violations.push(`${label}: continuous utility animation is not allowed`);
    if (softShadow.test(classes)) violations.push(`${label}: Tailwind soft/arbitrary shadows are not allowed`);
  }

  if (!allowedColorFiles.has(file)) {
    const matches = source.match(literalColor);
    if (matches) violations.push(`${label}: literal colors outside tokens.css: ${[...new Set(matches)].join(', ')}`);
  }

  if (extname(file) === '.css' && !allowedColorFiles.has(file)) {
    for (const match of source.matchAll(cssShadow)) {
      if (!/^(?:none)$|\b\d+px\s+\d+px\s+0\b|var\(--shadow-pixel/.test(match[1].trim())) {
        violations.push(`${label}: shadow must use a zero-blur hard pixel recipe: ${match[1].trim()}`);
      }
    }
    for (const match of source.matchAll(cssMotion)) {
      if (!/var\(--motion-(?:fast|panel)\)/.test(match[1]) && !/^1(?:ms)? !important$/.test(match[1].trim())) {
        violations.push(`${label}: motion duration must reference an approved token: ${match[1].trim()}`);
      }
    }
  }
}

if (violations.length) {
  console.error('Arcade UI audit failed:\n');
  for (const violation of violations) console.error(`- ${violation}`);
  process.exitCode = 1;
} else {
  console.log('Arcade UI audit passed. No legacy classes or design-token drift found.');
}
