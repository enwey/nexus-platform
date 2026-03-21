import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();

function readFile(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function normalizeEndpoint(method, endpointPath) {
  return `${method.toUpperCase()} ${endpointPath}`;
}

function normalizePathParameters(endpoint) {
  return endpoint
    .replace(/\$\{[^}]+\}/g, '{param}')
    .replace(/\{[^}]+\}/g, '{param}');
}

function parseBackendContract() {
  const contract = JSON.parse(readFile('contracts/backend-api.json'));
  return {
    contractEndpoints: new Set(
      contract.endpoints.map((entry) =>
        normalizeEndpoint(entry.method, normalizePathParameters(entry.path))
      )
    ),
    devPortalEndpoints: new Set(
      contract.endpoints
        .filter((entry) => entry.consumers.includes('dev-portal'))
        .map((entry) =>
          normalizeEndpoint(entry.method, normalizePathParameters(entry.path))
        )
    )
  };
}

function parseControllerEndpoints() {
  const controllerDir = path.join(root, 'backend/src/main/java/com/nexus/platform/controller');
  const endpoints = new Set();
  const files = fs.readdirSync(controllerDir).filter((file) => file.endsWith('.java'));
  const methodRegex = /@(GetMapping|PostMapping|PutMapping|DeleteMapping)\("([^"]+)"\)/g;

  for (const file of files) {
    const source = fs.readFileSync(path.join(controllerDir, file), 'utf8');
    const classMatch = source.match(/@RequestMapping\("([^"]+)"\)/);
    const basePath = classMatch ? classMatch[1] : '';

    for (const match of source.matchAll(methodRegex)) {
      const method = match[1].replace('Mapping', '').toUpperCase();
      const endpointPath = normalizePathParameters(`${basePath}${match[2]}`);
      endpoints.add(normalizeEndpoint(method, endpointPath));
    }
  }

  return endpoints;
}

function parseDevPortalEndpoints() {
  const source = readFile('dev-portal/src/api/index.js');
  const endpoints = new Set();
  const regex = /return request\(\{\s+url: (?:`([^`]+)`|'([^']+)'),\s+method: '([^']+)'/gms;

  for (const match of source.matchAll(regex)) {
    const rawPath = match[1] ?? match[2];
    const method = match[3].toUpperCase();
    endpoints.add(normalizeEndpoint(method, rawPath));
  }

  return endpoints;
}

function normalizeDynamicEndpoints(set) {
  return new Set([...set].map(normalizePathParameters));
}

function diff(expected, actual) {
  return [...expected].filter((item) => !actual.has(item)).sort();
}

function extras(expected, actual) {
  return [...actual].filter((item) => !expected.has(item)).sort();
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

const { contractEndpoints, devPortalEndpoints } = parseBackendContract();
const backendEndpoints = parseControllerEndpoints();
const portalEndpoints = normalizeDynamicEndpoints(parseDevPortalEndpoints());

const failures = [];

const missingFromBackend = diff(contractEndpoints, backendEndpoints);
if (missingFromBackend.length > 0) {
  failures.push(['Backend contract endpoints missing from controllers:', missingFromBackend]);
}

const backendExtras = extras(contractEndpoints, backendEndpoints);
if (backendExtras.length > 0) {
  failures.push(['Backend controllers expose endpoints missing from contract:', backendExtras]);
}

const portalMissing = diff(devPortalEndpoints, portalEndpoints);
if (portalMissing.length > 0) {
  failures.push(['Dev portal contract endpoints missing from API layer:', portalMissing]);
}

const portalExtras = extras(devPortalEndpoints, portalEndpoints);
if (portalExtras.length > 0) {
  failures.push(['Dev portal API layer calls endpoints missing from contract:', portalExtras]);
}

if (failures.length > 0) {
  for (const [title, items] of failures) {
    printSection(title, items);
  }
  process.exit(1);
}

console.log('Backend contract check passed.');
