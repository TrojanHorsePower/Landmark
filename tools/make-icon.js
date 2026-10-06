// Regenerates src/main/resources/assets/landmark/icon.png (256x256): a claim outline with a pin on a dark tile.
//   node tools/make-icon.js
const fs = require('fs');
const zlib = require('zlib');
const { crc32 } = require('./export.js');

const SIZE = 256, SS = 4;
const BG = [27, 33, 40], BORDER = [60, 74, 88], FILL = [255, 176, 0], EDGE = [255, 226, 130], PIN = [64, 224, 255], PIN_EDGE = [10, 30, 40];

const claim = [[56, 70], [150, 70], [150, 128], [118, 128], [118, 196], [56, 196]]; // an L-shaped land
const gridLines = [64, 128, 192];

function inPoly(x, y, p) {
	let c = false;
	for (let i = 0, j = p.length - 1; i < p.length; j = i++) {
		if ((p[i][1] > y) !== (p[j][1] > y) && x < (p[j][0] - p[i][0]) * (y - p[i][1]) / (p[j][1] - p[i][1]) + p[i][0]) c = !c;
	}
	return c;
}
function distToPoly(x, y, p) {
	let best = Infinity;
	for (let i = 0; i < p.length; i++) {
		const [ax, ay] = p[i], [bx, by] = p[(i + 1) % p.length];
		const dx = bx - ax, dy = by - ay, t = Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
		best = Math.min(best, Math.hypot(x - (ax + t * dx), y - (ay + t * dy)));
	}
	return best;
}
function inRoundRect(x, y, r) {
	const m = 6, lo = m, hi = SIZE - m;
	if (x < lo || x > hi || y < lo || y > hi) return false;
	const cx = Math.max(lo + r, Math.min(hi - r, x)), cy = Math.max(lo + r, Math.min(hi - r, y));
	return Math.hypot(x - cx, y - cy) <= r;
}
const pinCircle = [176, 100, 30];
const pinTip = [[150, 112], [202, 112], [176, 178]];

function sample(x, y) {
	if (!inRoundRect(x, y, 44)) return null;
	let c = BG;
	if (!inRoundRect(x, y, 44) || !inRoundRect(x + 5 * Math.sign(x - 128), y + 5 * Math.sign(y - 128), 40)) c = BORDER;
	for (const g of gridLines) if (Math.abs(x - g) < 1.2 || Math.abs(y - g) < 1.2) c = c === BG ? [38, 47, 57] : c;
	if (inPoly(x, y, claim)) c = distToPoly(x, y, claim) < 4 ? EDGE : FILL;
	else if (distToPoly(x, y, claim) < 4) c = EDGE;
	const dPin = Math.min(Math.hypot(x - pinCircle[0], y - pinCircle[1]) - pinCircle[2], inPoly(x, y, pinTip) ? -distToPoly(x, y, pinTip) : distToPoly(x, y, pinTip));
	if (dPin < 5) c = dPin < 0 ? PIN : PIN_EDGE;
	if (Math.hypot(x - pinCircle[0], y - pinCircle[1]) < 11) c = PIN_EDGE;
	return c;
}

const rgba = Buffer.alloc(SIZE * SIZE * 4);
for (let y = 0; y < SIZE; y++) for (let x = 0; x < SIZE; x++) {
	let r = 0, g = 0, b = 0, a = 0;
	for (let sy = 0; sy < SS; sy++) for (let sx = 0; sx < SS; sx++) {
		const c = sample(x + (sx + 0.5) / SS, y + (sy + 0.5) / SS);
		if (c) { r += c[0]; g += c[1]; b += c[2]; a++; }
	}
	const i = (y * SIZE + x) * 4, n = SS * SS;
	if (a) { rgba[i] = r / a; rgba[i + 1] = g / a; rgba[i + 2] = b / a; }
	rgba[i + 3] = Math.round(255 * a / n);
}
function chunk(type, data) {
	const out = Buffer.alloc(12 + data.length);
	out.writeUInt32BE(data.length, 0); out.write(type, 4, 'latin1'); data.copy(out, 8);
	out.writeUInt32BE(crc32(new Uint8Array(out.subarray(4, 8 + data.length))), 8 + data.length);
	return out;
}
const raw = Buffer.alloc((SIZE * 4 + 1) * SIZE);
for (let y = 0; y < SIZE; y++) rgba.copy(raw, y * (SIZE * 4 + 1) + 1, y * SIZE * 4, (y + 1) * SIZE * 4);
const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(SIZE, 0); ihdr.writeUInt32BE(SIZE, 4); ihdr[8] = 8; ihdr[9] = 6;
fs.mkdirSync('src/main/resources/assets/landmark', { recursive: true });
fs.writeFileSync('src/main/resources/assets/landmark/icon.png', Buffer.concat([
	Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]));
console.log('wrote icon.png');
