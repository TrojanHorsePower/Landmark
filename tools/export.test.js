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
