#!/usr/bin/env node
/*
 * Uploads the jars of a GitHub release to CurseForge.
 *
 *   node tools/curseforge-upload.js <tag> <jar folder> [--dry-run]
 *
 * Needs the CURSEFORGE_TOKEN (an API token from https://www.curseforge.com/account/api-tokens) and CURSEFORGE_PROJECT_ID
 * environment variables; with either missing it says so and does nothing. --dry-run prints what would be sent and does not
 * touch the network. Only reads the jar folder and CHANGELOG.md; the token goes to CurseForge's upload API and nowhere else.
 */
'use strict';
const fs = require('fs');
const path = require('path');

const API = 'https://minecraft.curseforge.com/api';

// Minecraft versions each jar covers, by the label in its file name (landmark-<version>+mc<label>.jar), and the Java it needs.
const JARS = {
	'1.21-1.21.1': { mc: ['1.21', '1.21.1'], java: 21 },
	'1.21.2-1.21.4': { mc: ['1.21.2', '1.21.3', '1.21.4'], java: 21 },
	'1.21.5': { mc: ['1.21.5'], java: 21 },
	'1.21.6-1.21.8': { mc: ['1.21.6', '1.21.7', '1.21.8'], java: 21 },
	'1.21.9-1.21.10': { mc: ['1.21.9', '1.21.10'], java: 21 },
	'1.21.11': { mc: ['1.21.11'], java: 21 },
	'26.1-26.1.2': { mc: ['26.1', '26.1.1', '26.1.2'], java: 25 },
	'26.2': { mc: ['26.2'], java: 25 },
	'26.3': { mc: ['26.3'], java: 25 }
};

// Related CurseForge projects (by slug; the numeric IDs are in the comments). Fabric API is needed; the rest are optional.
const RELATIONS = [
	{ slug: 'fabric-api', type: 'requiredDependency' }, // 306612
	{ slug: 'modmenu', type: 'optionalDependency' }, // 308702
	{ slug: 'xaeros-minimap', type: 'optionalDependency' }, // 263420
	{ slug: 'xaeros-world-map', type: 'optionalDependency' }, // 317780
	{ slug: 'journeymap', type: 'optionalDependency' } // 32274
];

function parseJarName(file) {
	const m = /^landmark-(.+?)\+mc(.+)\.jar$/.exec(file);
	return m ? { version: m[1], label: m[2] } : null;
}

/** The text of the changelog section for a version (without its heading), or '' if there is none. */
function changelogFor(markdown, version) {
	const lines = markdown.split(/\r?\n/);
	const start = lines.findIndex((l) => l.startsWith('## [' + version + ']'));
	if (start < 0) return '';
	let end = lines.findIndex((l, i) => i > start && (l.startsWith('## [') || /^\[[^\]]+\]: /.test(l)));
	if (end < 0) end = lines.length;
	return lines.slice(start + 1, end).join('\n').trim();
}

/** Names of the CurseForge game versions to attach to one jar: its Minecraft versions, the loader, the environment, Java. */
function versionNames(label) {
	const j = JARS[label];
	if (!j) throw new Error('No Minecraft versions known for jar label "' + label + '"; add it to JARS in tools/curseforge-upload.js');
	return [...j.mc, 'Fabric', 'Client', 'Java ' + j.java];
}

function metadataFor(jar, changelog, ids) {
	return {
		changelog: changelog || 'See https://github.com/TrojanHorsePower/Landmark/blob/main/CHANGELOG.md',
		changelogType: 'markdown',
		displayName: 'Landmark ' + jar.version + ' (Minecraft ' + jar.label + ')',
		releaseType: 'release',
		gameVersions: ids,
		relations: { projects: RELATIONS }
	};
}

async function api(token, url, init) {
	const r = await fetch(url, Object.assign({ headers: { 'X-Api-Token': token } }, init));
	const text = await r.text();
	if (!r.ok) throw new Error(r.status + ' ' + url.replace(/\?.*/, '') + ': ' + text.slice(0, 300));
	return text ? JSON.parse(text) : null;
}

async function main(argv) {
	const args = argv.filter((a) => !a.startsWith('--'));
	const dry = argv.includes('--dry-run');
	if (args.length < 2) throw new Error('usage: curseforge-upload.js <tag> <jar folder> [--dry-run]');
	const version = args[0].replace(/^v/, '');
	const dir = args[1];
	const token = process.env.CURSEFORGE_TOKEN;
	const project = process.env.CURSEFORGE_PROJECT_ID;
	if (!dry && (!token || !project)) {
		console.log('CurseForge upload skipped: set the CURSEFORGE_TOKEN secret and the CURSEFORGE_PROJECT_ID variable to enable it.');
		return;
	}
	const jars = fs.readdirSync(dir).map((f) => ({ file: f, ...parseJarName(f) }))
		.filter((j) => j.label && !j.file.endsWith('-sources.jar') && j.version === version);
	if (jars.length === 0) throw new Error('No jars for version ' + version + ' in ' + dir);
	const changelog = changelogFor(fs.readFileSync(path.join(__dirname, '..', 'CHANGELOG.md'), 'utf8'), version);

	let byName = null;
	if (!dry) {
		const list = await api(token, API + '/game/versions');
		byName = new Map();
		for (const v of list) if (!byName.has(v.name)) byName.set(v.name, v.id);
	}
	for (const jar of jars) {
		const names = versionNames(jar.label);
		let ids = names.map((n, i) => i);
		if (!dry) {
			const missing = names.filter((n) => !byName.has(n));
			if (missing.length) throw new Error(jar.file + ': CurseForge has no game version named ' + missing.join(', '));
			ids = names.map((n) => byName.get(n));
		}
		const meta = metadataFor(jar, changelog, ids);
		if (dry) {
			console.log(jar.file + ' -> ' + names.join(', ') + '\n  ' + meta.displayName);
			continue;
		}
		const form = new FormData();
		form.append('metadata', JSON.stringify(meta));
		form.append('file', new Blob([fs.readFileSync(path.join(dir, jar.file))]), jar.file);
		const res = await api(token, API + '/projects/' + encodeURIComponent(project) + '/upload-file', { method: 'POST', body: form });
		console.log('Uploaded ' + jar.file + ' as file ' + (res && res.id));
	}
}

module.exports = { parseJarName, changelogFor, versionNames, metadataFor, JARS, RELATIONS };

if (require.main === module) {
	main(process.argv.slice(2)).catch((e) => {
		console.error('CurseForge upload failed: ' + e.message);
		process.exit(1);
	});
}
