const jsonHeaders = {
  'Content-Type': 'application/json',
};

async function handleJson(response) {
  if (!response.ok) {
    const payload = await response.json().catch(() => ({ error: 'Request failed.' }));
    throw new Error(payload.error || 'Request failed.');
  }
  return response.json();
}

export async function fetchDirectories() {
  return handleJson(await fetch('/api/directories'));
}

export async function fetchTvSeries() {
  return handleJson(await fetch('/api/tv-series'));
}

export async function previewImport(payload) {
  return handleJson(await fetch('/api/preview', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(payload),
  }));
}

export async function executeImport(payload) {
  return handleJson(await fetch('/api/import', {
    method: 'POST',
    headers: jsonHeaders,
    body: JSON.stringify(payload),
  }));
}
