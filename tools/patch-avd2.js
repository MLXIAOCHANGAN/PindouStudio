
const fs = require('fs');
const p = 'C:/Users/MLXIAOCHANGAN/.android/avd/pindou34g.avd/config.ini';
let text = fs.readFileSync(p, 'utf8');
const overrides = {
  'hw.ramSize': '3072M',
  'vm.heapSize': '256M',
  'hw.gpu.enabled': 'yes',
  'hw.gpu.mode': 'swiftshader_indirect',
  'hw.keyboard': 'yes',
  'disk.dataPartition.size': '3G',
  'hw.lcd.density': '420',
};
for (const [k, v] of Object.entries(overrides)) {
  const re = new RegExp('^' + k.replace(/\./g, '\\.') + '=.*$', 'm');
  if (re.test(text)) text = text.replace(re, k + '=' + v);
  else text += (text.endsWith('\n') ? '' : '\n') + k + '=' + v + '\n';
}
fs.writeFileSync(p, text, 'utf8');
console.log('patched pindou34g');
