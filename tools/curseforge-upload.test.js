'use strict';
const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const { parseJarName, changelogFor, versionNames, metadataFor, JARS, RELATIONS } = require('./curseforge-upload.js');

test('parses jar names', () => {
	assert.deepStrictEqual(parseJarName('landmark-0.3.1+mc26.1-26.1.2.jar'), { version: '0.3.1', label: '26.1-26.1.2' });
	assert.strictEqual(parseJarName('landmark-0.3.1+mc26.3-sources.jar').label, '26.3-sources');
	assert.strictEqual(parseJarName('other.jar'), null);
});

test('every era has a jar entry and the other way round', () => {
	const eras = fs.readdirSync(path.join(__dirname, '..', 'eras')).map((f) => fs.readFileSync(path.join(__dirname, '..', 'eras', f), 'utf8'));
	const labels = eras.map((t) => /^mc_label=(.+)$/m.exec(t)[1].trim());
	const javas = eras.map((t) => Number(/^java_version=(.+)$/m.exec(t)[1]));
	assert.deepStrictEqual(Object.keys(JARS).sort(), labels.slice().sort());
	labels.forEach((l, i) => assert.strictEqual(JARS[l].java, javas[i], 'java for ' + l));
});

test('version names include loader, environment and Java', () => {
	assert.deepStrictEqual(versionNames('1.21-1.21.1'), ['1.21', '1.21.1', 'Fabric', 'Client', 'Java 21']);
	assert.throws(() => versionNames('9.9'));
});

test('extracts a changelog section', () => {
	const md = '# Changelog\n\n## [0.2.0] - x\n\n### Added\n- b\n\n## [0.1.0] - y\n- a\n\n[0.2.0]: https://x\n';
	assert.strictEqual(changelogFor(md, '0.2.0'), '### Added\n- b');
	assert.strictEqual(changelogFor(md, '0.1.0'), '- a');
	assert.strictEqual(changelogFor(md, '9.9.9'), '');
});

test('the real changelog has a section for the current version', () => {
	const props = fs.readFileSync(path.join(__dirname, '..', 'gradle.properties'), 'utf8');
	const v = /^mod_version=(.+)$/m.exec(props)[1].trim();
	assert.ok(changelogFor(fs.readFileSync(path.join(__dirname, '..', 'CHANGELOG.md'), 'utf8'), v).length > 0);
});

test('Fabric API is required and the optional integrations are listed', () => {
	const meta = metadataFor({ version: '1.0.0', label: '26.3' }, 'x', [1]);
	const by = Object.fromEntries(meta.relations.projects.map((p) => [p.slug, p.type]));
	assert.deepStrictEqual(by, {
		'fabric-api': 'requiredDependency',
		modmenu: 'optionalDependency',
		'xaeros-minimap': 'optionalDependency',
		'xaeros-world-map': 'optionalDependency',
		journeymap: 'optionalDependency'
	});
	assert.strictEqual(RELATIONS.length, 5);
});
