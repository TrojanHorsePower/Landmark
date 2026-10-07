const test = require('node:test');
const assert = require('node:assert');
const { buildZip, crc32, tileRange } = require('./export.js');

test('crc32 matches the standard check value', () => {
	assert.strictEqual(crc32(new TextEncoder().encode('123456789')), 0xCBF43926);
});

test('zip has expected signatures and entry count', () => {
	const z = buildZip([{ name: 'a.txt', data: new Uint8Array([1, 2, 3]) }, { name: 'b/c.txt', data: new Uint8Array(0) }]);
	const v = new DataView(z.buffer, z.byteOffset, z.byteLength);
	assert.strictEqual(v.getUint32(0, true), 0x04034B50);
	assert.strictEqual(v.getUint32(z.length - 22, true), 0x06054B50);
	assert.strictEqual(v.getUint16(z.length - 22 + 10, true), 2);
});

test('tileRange covers claims plus margin', () => {
	const lands = [{ data: { polylines: [{ points: [{ x: -1, z: 0 }, { x: 4096, z: 5000 }] }] } }];
	assert.deepStrictEqual(tileRange(lands, 4096, 1), { x0: -2, x1: 2, z0: -1, z1: 2 });
	assert.strictEqual(tileRange([], 4096, 1), null);
});

test('a zip built from Blob parts is byte-identical to the buffered one', async () => {
	const when = new Date(Date.UTC(2023, 10, 14, 12, 0, 0));
	const bytes = new Uint8Array([9, 8, 7, 6, 5]);
	const buffered = buildZip([{ name: 'a.txt', data: bytes }, { name: 'tiles/0_0.png', data: bytes }], when);
	const { zipParts } = require('./export.js');
	const parts = zipParts([
		{ name: 'a.txt', data: bytes },
		{ name: 'tiles/0_0.png', data: new Blob([bytes]), size: bytes.length, crc: crc32(bytes) }
	], when);
	const viaBlob = new Uint8Array(await new Blob(parts).arrayBuffer());
	assert.deepStrictEqual(Buffer.from(viaBlob), Buffer.from(buffered));
});

test('the script still parses after every export preset is applied', () => {
	const { execFileSync } = require('child_process');
	const fs = require('fs');
	const os = require('os');
	const path = require('path');
	const src = fs.readFileSync(path.join(__dirname, 'export.js'), 'utf8');
	for (const [folder, mode] of [[3, 'small'], [3, 'full'], [2, 'full'], [1, 'full']]) {
		const out = src.replace(/(tileFolder:\s*)\d+/, '$1' + folder).replace(/(tileMode:\s*')[a-z]+(')/, '$1' + mode + '$2');
		const f = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'lm-')), 'e.js');
		fs.writeFileSync(f, out);
		execFileSync(process.execPath, ['--check', f]);
	}
});
