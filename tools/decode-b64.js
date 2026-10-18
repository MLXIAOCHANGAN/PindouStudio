
const fs = require('fs');
const dir = 'F:/Agent-Program/项目09_(拼豆)/pindou-android/shots/';
for (const name of ['project', 'thumb']) {
  const b64 = fs.readFileSync(dir + name + '.b64', 'utf8').replace(/\s+/g, '');
  fs.writeFileSync(dir + name + (name === 'thumb' ? '.png' : '.json'), Buffer.from(b64, 'base64'));
}
console.log('decoded');
