const headers = { 'Content-Type': 'application/json' }

async function request(path, options = {}) {
  const response = await fetch(`/api${path}`, { headers, ...options })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || 'Something went wrong.')
  return body
}

export const api = {
  dashboard: () => request('/dashboard'),
  trips: () => request('/trips'),
  createTrip: data => request('/trips', { method: 'POST', body: JSON.stringify(data) }),
  readiness: id => request(`/trips/${id}/readiness`),
  runReadiness: id => request(`/trips/${id}/readiness`, { method: 'POST' }),
  cards: () => request('/cards'),
  updateCard: (id, data) => request(`/cards/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),
  transactions: () => request('/transactions'),
  sendPayment: data => request('/demo/payment', { method: 'POST', body: JSON.stringify(data) }),
  alerts: () => request('/alerts'),
  alertAction: (id, data) => request(`/alerts/${id}/actions`, {
    method: 'POST', body: JSON.stringify(data)
  })
}

