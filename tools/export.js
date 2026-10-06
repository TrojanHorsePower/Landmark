/*
 * Landmark map export.
 *
 * Run this in the browser console while the Pl3xMap page is open (and the Cloudflare check has passed).
 * It downloads `landmark-export.zip` containing the claims layer and, optionally, zoomed-out map tiles.
 * Drop that zip onto the Landmark map screen in Minecraft.
 *
 * Nothing is sent anywhere: it only reads files from the page you already have open and saves a zip locally.
 * The zip layout is documented in docs/export-format.md.
 */
(function (root) {
	'use strict';

	var CONFIG = {
		world: 'world',
		dimension: 'minecraft:overworld',
		layer: 'lands',
		tileFolder: 3, // 0 = full detail; each step doubles the blocks covered per tile
		tilePixels: 512,
		tileMode: 'small', // 'full' = keep PNGs as served, 'small' = re-encode to JPEG, 'none' = claims only
		jpegQuality: 0.85,
		concurrency: 4,
		marginTiles: 1
	};

	// ---- zip writer (store-only, no compression, UTF-8 names) ----
	var CRC_TABLE = (function () {
		var t = new Uint32Array(256);
		for (var n = 0; n < 256; n++) {
			var c = n;
			for (var k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
			t[n] = c >>> 0;
		}
		return t;
	})();

	function crc32(bytes) {
		var c = 0xFFFFFFFF;
		for (var i = 0; i < bytes.length; i++) c = CRC_TABLE[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
		return (c ^ 0xFFFFFFFF) >>> 0;
	}

	/** files: [{name: string, data: Uint8Array}] -> Uint8Array of the zip. */
	function buildZip(files, date) {
		date = date || new Date();
		var dosTime = (date.getHours() << 11) | (date.getMinutes() << 5) | (date.getSeconds() >> 1);
		var dosDate = ((date.getFullYear() - 1980) << 9) | ((date.getMonth() + 1) << 5) | date.getDate();
		var enc = new TextEncoder();
		var chunks = [];
		var central = [];
		var offset = 0;
		files.forEach(function (f) {
			var name = enc.encode(f.name);
			var crc = crc32(f.data);
			var local = new DataView(new ArrayBuffer(30));
			local.setUint32(0, 0x04034B50, true);
			local.setUint16(4, 20, true);
			local.setUint16(6, 0x0800, true); // UTF-8 names
			local.setUint16(8, 0, true); // stored
			local.setUint16(10, dosTime, true);
			local.setUint16(12, dosDate, true);
			local.setUint32(14, crc, true);
			local.setUint32(18, f.data.length, true);
			local.setUint32(22, f.data.length, true);
			local.setUint16(26, name.length, true);
			local.setUint16(28, 0, true);
			chunks.push(new Uint8Array(local.buffer), name, f.data);
			var cd = new DataView(new ArrayBuffer(46));
			cd.setUint32(0, 0x02014B50, true);
			cd.setUint16(4, 20, true);
			cd.setUint16(6, 20, true);
			cd.setUint16(8, 0x0800, true);
			cd.setUint16(10, 0, true);
			cd.setUint16(12, dosTime, true);
			cd.setUint16(14, dosDate, true);
			cd.setUint32(16, crc, true);
			cd.setUint32(20, f.data.length, true);
			cd.setUint32(24, f.data.length, true);
			cd.setUint16(28, name.length, true);
			cd.setUint32(42, offset, true);
			central.push(new Uint8Array(cd.buffer), name);
			offset += 30 + name.length + f.data.length;
		});
		var cdSize = 0;
		central.forEach(function (c) { cdSize += c.length; });
		var end = new DataView(new ArrayBuffer(22));
		end.setUint32(0, 0x06054B50, true);
		end.setUint16(8, files.length, true);
		end.setUint16(10, files.length, true);
		end.setUint32(12, cdSize, true);
		end.setUint32(16, offset, true);
		var all = chunks.concat(central, [new Uint8Array(end.buffer)]);
		var total = 0;
		all.forEach(function (c) { total += c.length; });
		var out = new Uint8Array(total);
		var pos = 0;
		all.forEach(function (c) { out.set(c, pos); pos += c.length; });
		return out;
	}

	/** Inclusive tile index range covering the claims' bounding box plus a margin. */
	function tileRange(landsJson, blocksPerTile, margin) {
		var minX = Infinity, minZ = Infinity, maxX = -Infinity, maxZ = -Infinity;
		landsJson.forEach(function (m) {
			((m.data && m.data.polylines) || []).forEach(function (pl) {
				pl.points.forEach(function (p) {
					minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
					minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
				});
			});
		});
		if (minX === Infinity) return null;
		return {
			x0: Math.floor(minX / blocksPerTile) - margin, x1: Math.floor(maxX / blocksPerTile) + margin,
			z0: Math.floor(minZ / blocksPerTile) - margin, z1: Math.floor(maxZ / blocksPerTile) + margin
		};
	}

	// ---- browser part ----
	async function toJpeg(blob, quality) {
		var bmp = await createImageBitmap(blob);
		var canvas = document.createElement('canvas');
		canvas.width = bmp.width;
		canvas.height = bmp.height;
		var ctx = canvas.getContext('2d');
		ctx.fillStyle = '#000';
		ctx.fillRect(0, 0, canvas.width, canvas.height);
		ctx.drawImage(bmp, 0, 0);
		return new Promise(function (res) { canvas.toBlob(res, 'image/jpeg', quality); });
	}

	async function fetchTile(x, z) {
		var url = 'tiles/' + CONFIG.world + '/' + CONFIG.tileFolder + '/basic/' + x + '_' + z + '.png';
		var r = await fetch(url);
		if (!r.ok || (r.headers.get('content-type') || '').indexOf('image/png') < 0) return null;
		var blob = await r.blob();
		if (CONFIG.tileMode === 'small') blob = await toJpeg(blob, CONFIG.jpegQuality);
		return new Uint8Array(await blob.arrayBuffer());
	}

	async function run() {
		var enc = new TextEncoder();
		console.log('Landmark export: fetching claims...');
		var landsResp = await fetch('tiles/' + CONFIG.world + '/markers/' + CONFIG.layer + '.json');
		if (!landsResp.ok) throw new Error('Could not load claims layer: HTTP ' + landsResp.status);
		var landsText = await landsResp.text();
		var lands = JSON.parse(landsText);
		var files = [{ name: 'lands.json', data: enc.encode(landsText) }];
		var manifest = {
			format: 1,
			world: CONFIG.world,
			dimension: CONFIG.dimension,
			source: location.origin,
			exportedAt: Date.now()
		};
		if (CONFIG.tileMode !== 'none') {
			var bpt = CONFIG.tilePixels * Math.pow(2, CONFIG.tileFolder);
			var range = tileRange(lands, bpt, CONFIG.marginTiles);
			var ext = CONFIG.tileMode === 'small' ? 'jpg' : 'png';
			var queue = [];
			if (range) for (var x = range.x0; x <= range.x1; x++) for (var z = range.z0; z <= range.z1; z++) queue.push([x, z]);
			var done = 0, count = 0, bytes = 0;
			console.log('Landmark export: fetching up to ' + queue.length + ' tiles...');
			await Promise.all(Array.from({ length: CONFIG.concurrency }, async function () {
				for (var job; (job = queue.shift());) {
					var data = await fetchTile(job[0], job[1]);
					done++;
					if (data) {
						files.push({ name: 'tiles/' + job[0] + '_' + job[1] + '.' + ext, data: data });
						count++; bytes += data.length;
					}
					if (done % 25 === 0) console.log('Landmark export: ' + done + ' tiles checked, ' + count + ' kept');
				}
			}));
			manifest.tiles = { blocksPerTile: bpt, pixelSize: CONFIG.tilePixels, extension: ext, count: count };
			console.log('Landmark export: ' + count + ' tiles, ' + (bytes / 1048576).toFixed(1) + ' MB');
		}
		files.unshift({ name: 'manifest.json', data: enc.encode(JSON.stringify(manifest, null, 1)) });
		var zip = buildZip(files);
		var a = document.createElement('a');
		a.href = URL.createObjectURL(new Blob([zip], { type: 'application/zip' }));
		a.download = 'landmark-export.zip';
		document.body.appendChild(a);
		a.click();
		a.remove();
		console.log('Landmark export: done, ' + (zip.length / 1048576).toFixed(1) + ' MB. Drop landmark-export.zip onto the map in Minecraft.');
	}

	if (typeof module !== 'undefined' && module.exports) {
		module.exports = { buildZip: buildZip, crc32: crc32, tileRange: tileRange };
	} else {
		run().catch(function (e) { console.error('Landmark export failed:', e); });
	}
})(typeof window !== 'undefined' ? window : globalThis);
