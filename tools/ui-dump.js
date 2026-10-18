
const fs = require('fs');
const path = process.argv[2];
const out = process.argv[3];
const xml = fs.readFileSync(path, 'utf8');
const nodes = [];
const re = /<node\b([^>]*)>/g;
let m;
while ((m = re.exec(xml)) !== null) {
  const attrs = m[1];
  const get = (k) => {
    const mm = attrs.match(new RegExp(k + '="([^"]*)"'));
    return mm ? mm[1] : '';
  };
  const text = get('text');
  const desc = get('content-desc');
  const cls = get('class');
  const bounds = get('bounds');
  if (!bounds) continue;
  const bm = bounds.match(/\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]/);
  if (!bm) continue;
  const [x1, y1, x2, y2] = [ +bm[1], +bm[2], +bm[3], +bm[4] ];
  const label = text || desc;
  if (!label && !/EditText|TextView|Button|Switch|SeekBar|Slider/.test(cls)) continue;
  nodes.push({
    label,
    cls: cls.split('.').pop(),
    cx: Math.round((x1 + x2) / 2),
    cy: Math.round((y1 + y2) / 2),
    w: x2 - x1,
    h: y2 - y1,
  });
}
const lines = nodes.map(n => (n.label ? '[' + n.label + '] ' : '') + '<' + n.cls + '> center=(' + n.cx + ',' + n.cy + ') size=' + n.w + 'x' + n.h);
fs.writeFileSync(out, lines.join('\n'), 'utf8');
console.log('nodes:', nodes.length);
