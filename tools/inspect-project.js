
const fs = require('fs');
const zlib = require('zlib');
const p = process.argv[2];
const dto = JSON.parse(fs.readFileSync(p, 'utf8'));
const raw = zlib.gunzipSync(Buffer.from(dto.cells, 'base64'));
const w = dto.width, h = dto.height;
const cells = new Int16Array(w * h);
for (let i = 0; i < w * h && i * 2 + 1 < raw.length; i++) {
  cells[i] = raw.readUInt16LE(i * 2);
}
let painted = 0;
const byColor = new Map();
for (const v of cells) { if (v) { painted++; byColor.set(v, (byColor.get(v) || 0) + 1); } }
console.log(JSON.stringify({
  name: dto.name, width: w, height: h, paletteId: dto.paletteId, paletteColors: dto.paletteColors.length,
  beadCountField: dto.beadCount, paintedFromCells: painted,
  colors: [...byColor.entries()].map(([k,v]) => (dto.paletteColors[k-1] || {}).code + ':' + v),
}, null, 2));
// 打印前 12 行画布 ASCII（用 # 表示有豆子）
const rows = [];
for (let r = 0; r < Math.min(h, 14); r++) {
  let s = '';
  for (let c = 0; c < Math.min(w, 48); c++) s += cells[r * w + c] ? '#' : '.';
  rows.push(s);
}
console.log(rows.join('\n'));
