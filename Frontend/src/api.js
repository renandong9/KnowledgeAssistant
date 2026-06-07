import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 120000
})

async function unwrap(request) {
  const { data } = await request
  if (data?.success === false) {
    throw new Error(data.message || '请求失败')
  }
  return data?.data
}

export const api = {
  listDocuments: () => unwrap(http.get('/documents')),
  getDocument: id => unwrap(http.get(`/documents/${id}`)),
  getDocumentStatus: id => unwrap(http.get(`/documents/${id}/status`)),
  uploadDocument: form => unwrap(http.post('/documents/upload', form)),
  getAnalysis: documentId => unwrap(http.get(`/documents/${documentId}/analysis`)),
  rebuildAnalysis: documentId => unwrap(http.post(`/documents/${documentId}/analysis/rebuild`)),
  listRecommendations: documentId => unwrap(http.get(`/documents/${documentId}/recommendations`)),
  rebuildRecommendations: documentId => unwrap(http.post(`/documents/${documentId}/recommendations/rebuild`)),
  listChatSessions: documentId => unwrap(http.get(`/documents/${documentId}/chat-sessions`)),
  createChatSession: (documentId, payload) => unwrap(http.post(`/documents/${documentId}/chat-sessions`, payload)),
  updateChatSession: (sessionId, payload) => unwrap(http.put(`/chat-sessions/${sessionId}`, payload)),
  deleteChatSession: sessionId => unwrap(http.delete(`/chat-sessions/${sessionId}`)),
  listMessages: sessionId => unwrap(http.get(`/chat-sessions/${sessionId}/messages`)),
  chat: payload => unwrap(http.post('/chat', payload)),
  search: payload => unwrap(http.post('/search', payload))
}
