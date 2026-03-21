import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

function readFile(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function parseContract() {
  const contract = JSON.parse(readFile('contracts/bridge-api.json'));
  const names = contract.apis.map((api) => api.name);
  return {
    contract,
    names: new Set(names),
    androidRequired: new Set(
      contract.apis.filter((api) => api.android !== 'absent').map((api) => api.name)
    ),
    iosRequired: new Set(
      contract.apis.filter((api) => api.ios !== 'absent').map((api) => api.name)
    )
  };
}

function parseRegisteredApis(relativePath) {
  const source = readFile(relativePath);
  const matches = [
    ...source.matchAll(/apiHandlers\["([^"]+)"\]/g),
    ...source.matchAll(/"([^"]+)"\s+to\s+/g)
  ];
  return new Set(matches.map((match) => match[1]));
}

function parseSdkInvokedApis() {
  const files = [
    'mock-sdk/src/api/base.ts',
    'mock-sdk/src/api/extended.ts'
  ];
  const names = new Set();

  for (const relativePath of files) {
    const source = readFile(relativePath);
    const matches = [...source.matchAll(/invokeNative\('([^']+)'/g)];
    for (const match of matches) {
      names.add(match[1]);
    }
  }

  return names;
}

function diff(expected, actual) {
  return [...expected].filter((item) => !actual.has(item)).sort();
}

function extras(contractNames, actual) {
  return [...actual].filter((item) => !contractNames.has(item)).sort();
}

function printSection(title, items) {
  if (items.length === 0) {
    return;
  }

  console.error(title);
  for (const item of items) {
    console.error(`- ${item}`);
  }
}

const { names, androidRequired, iosRequired } = parseContract();
const androidRegistered = parseRegisteredApis('android-client/app/src/main/java/com/nexus/platform/bridge/NexusApiRegistry.kt');
const iosRegistered = parseRegisteredApis('ios-client/NexusPlatform/Sources/NexusPlatform/Bridge/NexusBridge.swift');
const sdkInvoked = parseSdkInvokedApis();

const failures = [];

const sdkMissing = diff(sdkInvoked, names);
if (sdkMissing.length > 0) {
  failures.push(['SDK invokes APIs missing from contract:', sdkMissing]);
}

const androidMissing = diff(androidRequired, androidRegistered);
if (androidMissing.length > 0) {
  failures.push(['Android bridge is missing contract APIs:', androidMissing]);
}

const iosMissing = diff(iosRequired, iosRegistered);
if (iosMissing.length > 0) {
  failures.push(['iOS bridge is missing contract APIs:', iosMissing]);
}

const androidExtras = extras(names, androidRegistered);
if (androidExtras.length > 0) {
  failures.push(['Android bridge registers APIs missing from contract:', androidExtras]);
}

const iosExtras = extras(names, iosRegistered);
if (iosExtras.length > 0) {
  failures.push(['iOS bridge registers APIs missing from contract:', iosExtras]);
}

if (failures.length > 0) {
  for (const [title, items] of failures) {
    printSection(title, items);
  }
  process.exit(1);
}

console.log('Bridge contract check passed.');
