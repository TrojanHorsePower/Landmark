// Regenerates src/test/resources/export-sample.zip using the real export writer and synthetic data.
// Usage: node tools/make-sample-zip.js
const fs = require('fs');
const { buildZip } = require('./export.js');

const onePixelPng = Buffer.from(
	'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64');
const lands = fs.readFileSync('src/test/resources/pl3x-lands-sample.json');
const manifest = {
	format: 1, world: 'world', dimension: 'minecraft:overworld', source: 'https://map.example.invalid',
	exportedAt: 1700000000000,
	tiles: { blocksPerTile: 4096, pixelSize: 512, extension: 'png', count: 2 }
};
const files = [
	{ name: 'manifest.json', data: new TextEncoder().encode(JSON.stringify(manifest, null, 1)) },
	{ name: 'lands.json', data: new Uint8Array(lands) },
	{ name: 'tiles/0_0.png', data: new Uint8Array(onePixelPng) },
	{ name: 'tiles/-1_0.png', data: new Uint8Array(onePixelPng) }
];
fs.writeFileSync('src/test/resources/export-sample.zip', buildZip(files, new Date(Date.UTC(2023, 10, 14, 12, 0, 0))));
