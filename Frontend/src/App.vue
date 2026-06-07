<template>
  <main class="shell">
    <section class="toolbar">
      <div>
        <p class="eyebrow">AI Research Knowledge Assistant</p>
        <h1>科研知识助手</h1>
      </div>
      <label class="upload">
        <input type="file" accept=".pdf,.md,.txt,.docx" @change="uploadDocument" />
        上传文档
      </label>
    </section>

    <section class="grid">
      <aside class="panel">
        <div class="panel-head">
          <h2>文档</h2>
          <button @click="loadDocuments">刷新</button>
        </div>
        <article
          v-for="doc in documents"
          :key="doc.id"
          class="doc"
          :class="{ active: selectedDocumentId === doc.id }"
          @click="selectDocument(doc)"
        >
          <strong>{{ doc.title }}</strong>
          <span>{{ doc.fileType }} · {{ doc.parseStatus }}</span>
        </article>
      </aside>

      <section class="panel workbench">
        <template v-if="selectedDocument">
          <header class="document-head">
            <div>
              <p class="eyebrow">当前文章</p>
              <h2>{{ selectedDocument.title }}</h2>
              <p class="muted">{{ selectedDocument.originalFileName || selectedDocument.filePath }}</p>
            </div>
            <button @click="review">生成复盘</button>
          </header>

          <article v-if="reviewReport" class="answer">{{ reviewReport.summary }}</article>

          <section class="chat-zone">
            <div class="session-bar">
              <button
                v-for="session in chatSessions"
                :key="session.id"
                class="session-tab"
                :class="{ active: activeSessionId === session.id }"
                @click="selectSession(session.id)"
              >
                {{ session.title }}
              </button>
              <button @click="openCreateSession">新建提问窗口</button>
            </div>

            <div v-if="activeSession" class="session-actions">
              <span>当前窗口：{{ activeSession.title }}</span>
              <div>
                <button @click="renameSession">重命名</button>
                <button @click="deleteSession">删除</button>
              </div>
            </div>

            <div class="messages">
              <article v-for="message in chatMessages" :key="message.id" class="message" :class="message.role">
                <strong>{{ message.role === 'user' ? '我' : '助手' }}</strong>
                <p>{{ message.content }}</p>
              </article>
            </div>

            <div class="ask-box">
              <textarea v-model="question" :placeholder="questionPlaceholder"></textarea>
              <button @click="chat" :disabled="!activeSessionId || !question.trim()">提问</button>
            </div>
          </section>

          <section class="stack">
            <h2>知识检索</h2>
            <textarea v-model="searchQuery" placeholder="搜索当前文章中的内容..."></textarea>
            <button @click="search">搜索</button>
            <article v-for="item in searchResults" :key="item.chunkId" class="result">
              <strong>{{ item.title }} · chunk {{ item.chunkId }}</strong>
              <p>{{ item.content }}</p>
              <small>score: {{ Number(item.score || 0).toFixed(3) }}</small>
            </article>
          </section>
        </template>

        <div v-else class="empty">
          <h2>请选择或上传一篇文章</h2>
          <p>选中文档后，可以为同一篇文章创建多个独立提问窗口。</p>
        </div>
      </section>
    </section>

    <div v-if="creatingSession" class="modal-backdrop" @click.self="closeCreateSession">
      <section class="modal">
        <h2>新建提问窗口</h2>
        <input
          v-model="newSessionTitle"
          placeholder="例如：实验部分、公式理解、组会汇报"
          @focus="showNameSuggestions = true"
        />
        <div v-if="showNameSuggestions" class="suggestions">
          <button v-for="suggestion in nameSuggestions" :key="suggestion" @click="applySuggestion(suggestion)">
            {{ suggestion }}
          </button>
        </div>
        <div class="modal-actions">
          <button @click="closeCreateSession">取消</button>
          <button @click="createSession" :disabled="!newSessionTitle.trim()">创建</button>
        </div>
      </section>
    </div>
  </main>
</template>

<script setup>
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'

const documents = ref([])
const selectedDocumentId = ref('')
const chatSessions = ref([])
const activeSessionId = ref('')
const chatMessages = ref([])
const searchQuery = ref('')
const searchResults = ref([])
const question = ref('')
const reviewReport = ref(null)
const creatingSession = ref(false)
const newSessionTitle = ref('')
const showNameSuggestions = ref(false)

const nameSuggestions = [
  '整体理解',
  '研究背景',
  '核心方法',
  '实验结果',
  '公式理解',
  '创新点',
  '优缺点',
  '相关工作',
  '组会汇报',
  '我的疑问'
]

const selectedDocument = computed(() => documents.value.find(doc => doc.id === selectedDocumentId.value))
const activeSession = computed(() => chatSessions.value.find(session => session.id === activeSessionId.value))
const questionPlaceholder = computed(() => activeSession.value ? `围绕“${activeSession.value.title}”提问...` : '请先创建或选择提问窗口')

async function loadDocuments() {
  const { data } = await axios.get('/api/documents')
  documents.value = data.data || []
  if (!selectedDocumentId.value && documents.value.length) {
    await selectDocument(documents.value[0])
  }
}

async function selectDocument(doc) {
  selectedDocumentId.value = doc.id
  reviewReport.value = null
  searchResults.value = []
  await loadChatSessions()
}

async function uploadDocument(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const form = new FormData()
  form.append('file', file)
  const { data } = await axios.post('/api/documents/upload', form)
  await loadDocuments()
  if (data.data?.id) {
    await selectDocument(data.data)
  }
  event.target.value = ''
}

async function loadChatSessions() {
  if (!selectedDocumentId.value) return
  const { data } = await axios.get(`/api/documents/${selectedDocumentId.value}/chat-sessions`)
  chatSessions.value = data.data || []
  activeSessionId.value = chatSessions.value[0]?.id || ''
  await loadMessages()
}

async function selectSession(sessionId) {
  activeSessionId.value = sessionId
  question.value = ''
  await loadMessages()
}

async function loadMessages() {
  chatMessages.value = []
  if (!activeSessionId.value) return
  const { data } = await axios.get(`/api/chat-sessions/${activeSessionId.value}/messages`)
  chatMessages.value = data.data || []
}

function openCreateSession() {
  newSessionTitle.value = ''
  showNameSuggestions.value = false
  creatingSession.value = true
}

function closeCreateSession() {
  creatingSession.value = false
}

function applySuggestion(suggestion) {
  newSessionTitle.value = suggestion
}

async function createSession() {
  const { data } = await axios.post(`/api/documents/${selectedDocumentId.value}/chat-sessions`, {
    documentId: selectedDocumentId.value,
    title: newSessionTitle.value,
    type: 'custom'
  })
  creatingSession.value = false
  await loadChatSessions()
  activeSessionId.value = data.data?.id || activeSessionId.value
  await loadMessages()
}

async function renameSession() {
  if (!activeSession.value) return
  const title = window.prompt('新的窗口名称', activeSession.value.title)
  if (!title?.trim()) return
  await axios.put(`/api/chat-sessions/${activeSession.value.id}`, {
    title,
    type: activeSession.value.type || 'custom'
  })
  await loadChatSessions()
}

async function deleteSession() {
  if (!activeSession.value) return
  if (!window.confirm(`删除提问窗口“${activeSession.value.title}”？`)) return
  await axios.delete(`/api/chat-sessions/${activeSession.value.id}`)
  await loadChatSessions()
}

async function search() {
  const { data } = await axios.post('/api/search', {
    documentId: selectedDocumentId.value,
    query: searchQuery.value,
    topic: activeSession.value?.title,
    limit: 8
  })
  searchResults.value = data.data || []
}

async function chat() {
  const text = question.value
  question.value = ''
  const { data } = await axios.post('/api/chat', {
    documentId: selectedDocumentId.value,
    sessionId: activeSessionId.value,
    question: text,
    limit: 6
  })
  await loadMessages()
  if (data.data?.references?.length) {
    searchResults.value = data.data.references
  }
}

async function review() {
  const { data } = await axios.post('/api/review/summary', { documentId: Number(selectedDocumentId.value) })
  reviewReport.value = data.data
}

onMounted(loadDocuments)
</script>
