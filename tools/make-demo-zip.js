// Builds a synthetic export zip (fake lands, fake owners, procedural terrain) in the real export format.
// It contains no data from any real server, so it is safe for screenshots, demos and development.
//
//   node tools/make-demo-zip.js demo-export.zip [tileFolder 3|2|1] [lands] [halfWorldBlocks]
//
// Then drop the zip onto the map screen, or import it into a dev run folder.
const fs = require('fs');
const zlib = require('zlib');
const { buildZip, crc32 } = require('./export.js');

function rng(seed) {
	return function () {
		seed |= 0; seed = (seed + 0x6D2B79F5) | 0;
		let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
		t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
		return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
	};
}
const rand = rng(20260601);

// ---- terrain: value noise ----
function hash(ix, iz) {
	let h = Math.imul(ix, 374761393) ^ Math.imul(iz, 668265263);
	h = Math.imul(h ^ (h >>> 13), 1274126177);
	return ((h ^ (h >>> 16)) >>> 0) / 4294967296;
}
function smooth(t) { return t * t * (3 - 2 * t); }
function noise(x, z) {
	const ix = Math.floor(x), iz = Math.floor(z), fx = smooth(x - ix), fz = smooth(z - iz);
	const a = hash(ix, iz), b = hash(ix + 1, iz), c = hash(ix, iz + 1), d = hash(ix + 1, iz + 1);
	return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
}
function height(bx, bz) {
	let v = 0, amp = 0.55, f = 1 / 1500;
	for (let o = 0; o < 5; o++) { v += noise(bx * f + o * 17.3, bz * f - o * 9.1) * amp; amp *= 0.5; f *= 2; }
	return v / 1.0;
}
function colour(h, jitter) {
	let r, g, b;
	if (h < 0.34) { r = 24; g = 52; b = 120; }
	else if (h < 0.42) { const t = (h - 0.34) / 0.08; r = 24 + 30 * t; g = 52 + 60 * t; b = 120 + 40 * t; }
	else if (h < 0.45) { r = 222; g = 211; b = 160; }
	else if (h < 0.7) { const t = (h - 0.45) / 0.25; r = 104 - 36 * t; g = 164 - 44 * t; b = 74 - 22 * t; }
	else if (h < 0.82) { r = 128; g = 124; b = 116; }
	else { r = 238; g = 242; b = 248; }
	const j = (jitter - 0.5) * 14;
	return [Math.max(0, Math.min(255, r + j)), Math.max(0, Math.min(255, g + j)), Math.max(0, Math.min(255, b + j))];
}

// ---- png ----
function pngChunk(type, data) {
	const out = Buffer.alloc(12 + data.length);
	out.writeUInt32BE(data.length, 0);
	out.write(type, 4, 'latin1');
	data.copy(out, 8);
	out.writeUInt32BE(crc32(new Uint8Array(out.subarray(4, 8 + data.length))), 8 + data.length);
	return out;
}
function png(w, h, rgba) {
	const raw = Buffer.alloc((w * 4 + 1) * h);
	for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4); }
	const ihdr = Buffer.alloc(13);
	ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
	return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), pngChunk('IHDR', ihdr), pngChunk('IDAT', zlib.deflateSync(raw, { level: 6 })), pngChunk('IEND', Buffer.alloc(0))]);
}

// tile folder (detail level) 3, 2 or 1 as in the export presets: tiles cover 4096, 2048 or 1024 blocks
const FOLDER = parseInt(process.argv[3] || '3', 10);
const LAND_COUNT = parseInt(process.argv[4] || '130', 10);
const HALF = parseInt(process.argv[5] || '4096', 10); // half the world size in blocks
const PX = 512, BPT = PX * Math.pow(2, FOLDER), BPP = BPT / PX;
const files = [];
const tileCount = (() => {
	let n = 0;
	const T = Math.ceil(HALF / BPT); // whole tiles only, even when the world is not a multiple of the tile size
	for (let tx = -T; tx < T; tx++) for (let tz = -T; tz < T; tz++) {
		const rgba = Buffer.alloc(PX * PX * 4);
		for (let y = 0; y < PX; y++) for (let x = 0; x < PX; x++) {
			const bx = tx * BPT + x * BPP, bz = tz * BPT + y * BPP;
			const i = (y * PX + x) * 4;
			const [r, g, b] = colour(height(bx, bz), hash(bx >> 3, bz >> 3));
			rgba[i] = r; rgba[i + 1] = g; rgba[i + 2] = b; rgba[i + 3] = 255;
		}
		files.push({ name: `tiles/${tx}_${tz}.png`, data: new Uint8Array(png(PX, PX, rgba)) });
		n++;
	}
	return n;
})();

// ---- fake lands in the real Pl3xMap marker shape ----
const A = ['Maple', 'Stone', 'Silver', 'Amber', 'Quiet', 'Golden', 'Misty', 'Iron', 'Willow', 'Crimson', 'Lunar', 'Sunny', 'Frosty', 'Hidden', 'Windy', 'Mossy'];
const N = ['Hollow', 'Bridge', 'Harbor', 'Ridge', 'Meadow', 'Keep', 'Grove', 'Falls', 'Vale', 'Landing', 'Market', 'Orchard', 'Spire', 'Cove', 'Farm', 'Hill'];
const P = ['Aria', 'Bram', 'Cleo', 'Dax', 'Elin', 'Finn', 'Gwen', 'Hugo', 'Isla', 'Jory', 'Kira', 'Leo', 'Mira', 'Nico', 'Odin', 'Pia'];
const used = new Set(), occupied = new Set();
const key = (cx, cz) => cx + ',' + cz;
const sq = (x, z, w, h) => [[x, z], [x + w, z], [x + w, z + h], [x, z + h]].map(([a, b]) => [a * 16, b * 16]);

function shape(cx, cz) {
	const kind = rand();
	const w = 3 + Math.floor(rand() * 12), h = 3 + Math.floor(rand() * 12);
	if (kind < 0.55) return { rings: [sq(cx, cz, w, h)], cells: [[cx, cz, w, h]], area: w * h };
	if (kind < 0.8) { // L shape
		const a = Math.max(1, Math.floor(w / 2)), b = Math.max(1, Math.floor(h / 2));
		const o = [[cx, cz], [cx + w, cz], [cx + w, cz + b], [cx + a, cz + b], [cx + a, cz + h], [cx, cz + h]].map(([x, z]) => [x * 16, z * 16]);
		return { rings: [o], cells: [[cx, cz, w, h]], area: w * h - (w - a) * (h - b) };
	}
	if (w >= 4 && h >= 4) return { rings: [sq(cx, cz, w, h), sq(cx + 1, cz + 1, 1, 1)], cells: [[cx, cz, w, h]], area: w * h - 1 }; // with a hole
	return { rings: [sq(cx, cz, w, h)], cells: [[cx, cz, w, h]], area: w * h };
}
function free(cells) {
	for (const [x, z, w, h] of cells) for (let i = x - 1; i <= x + w; i++) for (let j = z - 1; j <= z + h; j++) if (occupied.has(key(i, j))) return false;
	return true;
}
function occupy(cells) { for (const [x, z, w, h] of cells) for (let i = x; i < x + w; i++) for (let j = z; j < z + h; j++) occupied.add(key(i, j)); }

function tooltip(name, owner, members, chunks) {
	const li = 'class="\\&quot;infowindow\\&quot;"';
	return `<div class="\\&quot;infowindow\\&quot;"><span style="font-size: 200%;"><span style="color: {land_color};">${name}</span><br /></span>of ${owner}.</div> <ul> <li ${li}>Level: None</li> <li ${li}>Balance: $0.00</li> <li ${li}>Chunks: ${chunks}</li> <li ${li}>Players (${members.length}): ${members.join(', ')}</li> </ul>`;
}

const markers = [];
let lands = 0, guard = 0;
while (lands < LAND_COUNT && guard++ < LAND_COUNT * 300) {
	const cx = Math.floor((rand() * 2 - 1) * (HALF / 16 - 14)), cz = Math.floor((rand() * 2 - 1) * (HALF / 16 - 14));
	if (height(cx * 16, cz * 16) < 0.47 || height(cx * 16, cz * 16) > 0.8) continue;
	const s = shape(cx, cz);
	if (!free(s.cells)) continue;
	const name = A[Math.floor(rand() * A.length)] + N[Math.floor(rand() * N.length)] + (used.size > 90 ? Math.floor(rand() * 9000 + 10) : '');
	if (used.has(name)) continue;
	used.add(name); occupy(s.cells); lands++;
	const owner = 'Demo_' + P[Math.floor(rand() * P.length)] + Math.floor(rand() * 90 + 10);
	const members = [owner];
	for (let k = Math.floor(rand() * 4); k > 0; k--) members.push('Demo_' + P[Math.floor(rand() * P.length)] + Math.floor(rand() * 90 + 10));
	const chunks = s.area;
	const id = '01DEMO' + String(lands).padStart(20, '0');
	const tip = tooltip(name, owner, [...new Set(members)], chunks);
	markers.push({
		type: 'poly',
		data: { key: `${id}_world_0`, polylines: s.rings.map((r, i) => ({ key: `${id}_world_0_${i === 0 ? 'outer' : 'hole' + (i - 1)}`, points: r.map(([x, z]) => ({ x, z })) })) },
		options: { stroke: { weight: 2, color: -1 }, fill: { type: 1, color: -1 }, tooltip: { content: tip }, popup: { content: tip } }
	});
}

const enc = new TextEncoder();
const manifest = {
	format: 1, world: 'world', dimension: 'minecraft:overworld', source: 'demo data (synthetic)', exportedAt: Date.now(),
	tiles: { blocksPerTile: BPT, pixelSize: PX, extension: 'png', count: tileCount }
};
const out = process.argv[2] || 'demo-export.zip';
fs.writeFileSync(out, buildZip([
	{ name: 'manifest.json', data: enc.encode(JSON.stringify(manifest, null, 1)) },
	{ name: 'lands.json', data: enc.encode(JSON.stringify(markers)) },
	...files
]));
console.log(`wrote ${out}: ${markers.length} lands, ${tileCount} tiles`);
