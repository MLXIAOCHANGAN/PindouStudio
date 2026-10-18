
const fs = require('fs');
const dto = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const nonNull = dto.grid.flat().filter(v => v).length;
console.log(JSON.stringify({
  version: dto.version,
  createdAt: dto.createdAt,
  canvas: dto.canvas,
  paletteId: dto.palette && dto.palette.id,
  paletteLabel: dto.palette && dto.palette.label,
  paletteEntries: dto.palette && dto.palette.entries.length,
  firstColor: dto.palette && dto.palette.entries[0],
  gridRows: dto.grid.length,
  gridCols: dto.grid[0].length,
  painted: nonNull,
  sampleRow: dto.grid[2].slice(0, 12),
}, null, 2));
