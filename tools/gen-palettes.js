
const fs = require('fs');
const path = require('path');
const BASE = 'F:/Agent-Program/项目09_(拼豆)';

function cssToHex(v) {
  if (!v) return null;
  v = String(v).trim();
  if (/^#[0-9a-fA-F]{6}$/.test(v)) return v.toUpperCase();
  if (/^#[0-9a-fA-F]{3}$/.test(v)) return ('#' + v[1]+v[1]+v[2]+v[2]+v[3]+v[3]).toUpperCase();
  const m = v.match(/rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)/i);
  if (m) {
    const h = (n) => Math.max(0, Math.min(255, Math.round(Number(n)))).toString(16).padStart(2, '0');
    return ('#' + h(m[1]) + h(m[2]) + h(m[3])).toUpperCase();
  }
  return null;
}

const out = [];
// --- pindou-main palettes.json ---
const main = JSON.parse(fs.readFileSync(path.join(BASE, 'pindou-main/pindou-main/packages/shared/src/palettes.json'), 'utf8'));
const brandNames = { mard: 'MARD 漫漫', perler: 'Perler', artkal: 'Artkal', hama: 'Hama' };
for (const brand of main) {
  const colors = (brand.colors || [])
    .slice()
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    .map(c => ({
      code: c.code,
      name: c.name || c.code,
      hex: cssToHex(c.hex),
      series: c.series || '?',
      type: 'normal',
      hex2: null,
    }))
    .filter(c => c.hex);
  out.push({
    id: 'brand-' + brand.code,
    name: (brandNames[brand.code] || brand.name) + ' (' + colors.length + '色)',
    builtin: true,
    colors,
  });
}

// --- pindou-draw output ---
const dir = path.join(BASE, 'pindou-draw-main/output');
const labelMap = {
  'Coco.json': 'Coco 可可',
  'DMC.json': 'DMC',
  '卡卡家.json': '卡卡家',
  '漫漫家.json': '漫漫家',
  '盼盼拼豆.json': '盼盼拼豆',
};
const files = fs.readdirSync(dir).filter(f => f.endsWith('.json') && f !== 'manifest.json');
for (const f of files) {
  const raw = JSON.parse(fs.readFileSync(path.join(dir, f), 'utf8'));
  const keys = Object.keys(raw);
  const colors = [];
  for (const k of keys) {
    const e = raw[k] || {};
    const code = String(e.num || k).trim();
    const hex = cssToHex(e.color1 || e.color);
    if (!code || !hex) continue;
    const series = (code.match(/^[A-Za-z]+/) || ['?'])[0].toUpperCase() || '?';
    colors.push({
      code,
      name: code,
      hex,
      series,
      type: (typeof e.type === 'string' ? e.type : 'normal'),
      hex2: cssToHex(e.color2),
    });
  }
  let label = labelMap[f];
  if (!label) {
    const base = f.replace(/\.json$/, '');
    const m = base.match(/^MARD-(\d+)$/);
    label = m ? ('MARD ' + m[1] + '色') : base;
  }
  out.push({
    id: 'draw-' + f.replace(/\.json$/, ''),
    name: label + ' (' + colors.length + '色)',
    builtin: true,
    colors,
  });
}

const target = path.join(BASE, 'pindou-android/app/src/main/assets/palettes.json');
fs.mkdirSync(path.dirname(target), { recursive: true });
fs.writeFileSync(target, JSON.stringify({ version: 1, palettes: out }), 'utf8');
console.log('palettes:', out.length, 'total colors:', out.reduce((s, p) => s + p.colors.length, 0));
for (const p of out) console.log('  ', p.id, '|', p.name, '|', p.colors.length);
