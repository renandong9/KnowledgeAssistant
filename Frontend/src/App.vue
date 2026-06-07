<template>
  <main :class="['app-shell', { landing: !selectedDocument }]">
    <section v-if="!selectedDocument" class="upload-stage">
      <div class="brand">
        <p class="eyebrow">AI Research Knowledge Assistant</p>
        <h1>科研知识助手</h1>
      </div>

      <label
        class="upload-card"
        :class="{ dragging: isDragging, uploading: uploadState === 'uploading' }"
        @dragenter.prevent="isDragging = true"
        @dragover.prevent="isDragging = true"
        @dragleave.prevent="isDragging = false"
        @drop.prevent="handleDrop"
      >
        <input type="file" accept=".pdf,.docx,.txt,.md,.markdown" @change="handleFileInput" />
        <span class="upload-icon">+</span>
        <strong>{{ uploadTitle }}</strong>
        <small>支持 PDF、DOCX、TXT、Markdown</small>
      </label>

      <p v-if="uploadError" class="error-text">{{ uploadError }}</p>
    </section>

    <section v-else class="workspace">
      <header class="workspace-header">
        <div>
          <p class="eyebrow">Document Workspace</p>
          <h1>{{ selectedDocument.title }}</h1>
        </div>
        <label class="compact-upload">
          <input type="file" accept=".pdf,.docx,.txt,.md,.markdown" @change="handleFileInput" />
          上传新文章
        </label>
      </header>

      <section class="workspace-grid">
        <aside class="insight-panel">
          <section class="panel-section">
            <div class="section-head">
              <h2>文章基本信息</h2>
              <button @click="loadDocuments">刷新</button>
            </div>
            <dl class="meta-list">
              <div>
                <dt>文件名</dt>
                <dd>{{ selectedDocument.originalFileName || selectedDocument.title }}</dd>
              </div>
              <div>
                <dt>上传时间</dt>
                <dd>{{ formatTime(selectedDocument.createTime) }}</dd>
              </div>
              <div>
                <dt>解析状态</dt>
                <dd><span :class="['status-pill', parseStateTone]">{{ parseStateLabel }}</span></dd>
              </div>
              <div>
                <dt>OCR 状态</dt>
                <dd><span class="status-pill muted">{{ ocrStateLabel }}</span></dd>
              </div>
              <div>
                <dt>向量索引</dt>
                <dd><span :class="['status-pill', indexStateTone]">{{ indexStateLabel }}</span></dd>
              </div>
              <div>
                <dt>文件类型</dt>
                <dd>{{ selectedDocument.fileType }}</dd>
              </div>
            </dl>
          </section>

          <section class="panel-section">
            <h2>AI 摘要</h2>
            <p class="summary-text">{{ documentSummary }}</p>
            <button @click="review" :disabled="analysisState === '分析生成中'">生成/更新分析</button>
            <span class="status-line">{{ analysisState }}</span>
          </section>

          <section class="panel-section">
            <h2>文章结构 / 章节</h2>
            <ol class="structure-list">
              <li v-for="item in structureItems" :key="item">{{ item }}</li>
            </ol>
          </section>

          <section class="panel-section">
            <h2>AI 自动分析结果</h2>
            <div v-if="reviewReport" class="analysis-grid">
              <article>
                <strong>总结</strong>
                <p>{{ reviewReport.summary }}</p>
              </article>
            </div>
            <p v-else class="muted-text">上传解析完成后，可生成研究背景、核心方法、实验结果和优缺点分析。</p>
          </section>

          <section class="panel-section">
            <div class="section-head">
              <h2>相关推荐论文</h2>
              <button @click="recommendPapers">推荐</button>
            </div>
            <article v-for="paper in papers" :key="paper.id || paper.url" class="paper-card">
              <strong>{{ paper.title }}</strong>
              <p>{{ paper.abstractText }}</p>
              <a :href="paper.url" target="_blank">查看论文</a>
            </article>
            <p v-if="!papers.length" class="muted-text">根据文章标题、摘要或关键词推荐相关论文。</p>
          </section>
        </aside>

        <section class="chat-panel">
          <div class="section-head">
            <div>
              <p class="eyebrow">Topic Chat Sessions</p>
              <h2>多主题提问区</h2>
            </div>
            <button @click="openCreateSession">新建提问窗口</button>
          </div>

          <div class="session-tabs">
            <button
              v-for="session in chatSessions"
              :key="session.id"
              :class="{ active: activeSessionId === session.id }"
              @click="selectSession(session.id)"
            >
              {{ session.title }}
            </button>
          </div>

          <div v-if="activeSession" class="session-toolbar">
            <span>当前窗口：{{ activeSession.title }}</span>
            <div>
              <button @click="renameSession">重命名</button>
              <button @click="deleteSession">删除</button>
            </div>
          </div>

          <div class="state-strip">
            <span v-for="state in pageStates" :key="state.label" :class="['state-item', state.tone]">
              {{ state.label }}
            </span>
          </div>

          <div class="messages">
            <article v-for="message in chatMessages" :key="message.id" :class="['message', message.role]">
              <strong>{{ message.role === 'user' ? '我' : '助手' }}</strong>
              <p>{{ message.content }}</p>
            </article>
            <p v-if="!chatMessages.length" class="muted-text">这个窗口还没有对话。提问时会优先围绕“{{ activeSession?.title || '默认问答' }}”检索原文 Chunk。</p>
          </div>

          <div class="ask-box">
            <textarea v-model="question" :placeholder="questionPlaceholder"></textarea>
            <button @click="chat" :disabled="!activeSessionId || !question.trim()">提问</button>
          </div>

          <section class="reference-panel">
            <h2>本轮引用 Chunk / 检索结果</h2>
            <div class="search-row">
              <input v-model="searchQuery" placeholder="搜索当前文章中的内容" />
              <button @click="search">检索</button>
            </div>
            <article v-for="item in searchResults" :key="item.chunkId" class="chunk-card">
              <strong>{{ item.title }} · chunk {{ item.chunkId }}</strong>
              <p>{{ item.content }}</p>
              <small>score: {{ Number(item.score || 0).toFixed(3) }}</small>
            </article>
          </section>
        </section>
      </section>
    </section>

    <div v-if="creatingSession" class="modal-backdrop" @click.self="closeCreateSession">
      <section class="modal">
        <h2>新建提问窗口</h2>
        <input
          ref="sessionInput"
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
import { computed, nextTick, onMounted, ref } from 'vue'

const documents = ref([])
const selectedDocumentId = ref('')
const chatSessions = ref([])
const activeSessionId = ref('')
const chatMessages = ref([])
const searchQuery = ref('')
const searchResults = ref([])
const question = ref('')
const reviewReport = ref(null)
const papers = ref([])
const creatingSession = ref(false)
const newSessionTitle = ref('')
const showNameSuggestions = ref(false)
const uploadState = ref('idle')
const uploadError = ref('')
const isDragging = ref(false)
const analysisState = ref('待生成')
const sessionInput = ref(null)

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
const uploadTitle = computed(() => uploadState.value === 'uploading' ? '上传中，请稍候...' : '拖入一篇论文或文章，开始智能理解')
const questionPlaceholder = computed(() => activeSession.value ? `围绕“${activeSession.value.title}”提问，回答会尽量引用原文 Chunk` : '请先创建或选择提问窗口')
const parseStatus = computed(() => selectedDocument.value?.parseStatus || 'PENDING')
const parseStateLabel = computed(() => {
  const statusMap = {
    PENDING: '文档解析中',
    PROCESSING: '文档解析中',
    COMPLETED: '可提问状态',
    FAILED: '解析失败'
  }
  return statusMap[parseStatus.value] || parseStatus.value
})
const parseStateTone = computed(() => parseStatus.value === 'FAILED' ? 'danger' : parseStatus.value === 'COMPLETED' ? 'success' : 'working')
const ocrStateLabel = computed(() => selectedDocument.value?.ocrStatus || 'OCR 未启用 / 未触发')
const indexStateLabel = computed(() => parseStatus.value === 'COMPLETED' ? '向量索引构建完成' : '向量索引构建中')
const indexStateTone = computed(() => parseStatus.value === 'COMPLETED' ? 'success' : 'working')
const documentSummary = computed(() => selectedDocument.value?.summary || reviewReport.value?.summary || '解析完成后，这里会展示 AI 生成的文章摘要。')
const structureItems = computed(() => {
  if (parseStatus.value !== 'COMPLETED') {
    return ['等待文档解析完成', '等待章节结构提取', '等待 Chunk 索引构建']
  }
  return ['标题与摘要', '正文主要章节', '实验与结论', '参考信息']
})
const pageStates = computed(() => [
  { label: uploadState.value === 'uploading' ? '文件上传中' : '文件已接收', tone: uploadState.value === 'uploading' ? 'working' : 'success' },
  { label: parseStateLabel.value, tone: parseStateTone.value },
  { label: ocrStateLabel.value, tone: 'muted' },
  { label: indexStateLabel.value, tone: indexStateTone.value },
  { label: analysisState.value === '分析生成中' ? '分析生成中' : '分析待命', tone: analysisState.value === '分析生成中' ? 'working' : 'muted' }
])

async function loadDocuments() {
  const { data } = await axios.get('/api/documents')
  documents.value = data.data || []
  if (selectedDocumentId.value) {
    const current = documents.value.find(doc => doc.id === selectedDocumentId.value)
    if (!current) selectedDocumentId.value = ''
  }
}

async function selectDocument(doc) {
  selectedDocumentId.value = doc.id
  reviewReport.value = null
  papers.value = []
  searchResults.value = []
  await loadChatSessions()
}

function handleFileInput(event) {
  const file = event.target.files?.[0]
  if (file) uploadDocument(file)
  event.target.value = ''
}

function handleDrop(event) {
  isDragging.value = false
  const file = event.dataTransfer.files?.[0]
  if (file) uploadDocument(file)
}

async function uploadDocument(file) {
  uploadError.value = ''
  if (!isSupportedFile(file)) {
    uploadError.value = '仅支持 PDF、DOCX、TXT、Markdown 文件。'
    return
  }
  uploadState.value = 'uploading'
  const form = new FormData()
  form.append('file', file)
  try {
    const { data } = await axios.post('/api/documents/upload', form)
    await loadDocuments()
    if (data.data?.id) {
      documents.value = [data.data, ...documents.value.filter(doc => doc.id !== data.data.id)]
      await selectDocument(data.data)
    }
    uploadState.value = 'done'
  } catch (error) {
    uploadError.value = error.response?.data?.message || '上传失败，请稍后重试。'
    uploadState.value = 'idle'
  }
}

function isSupportedFile(file) {
  return /\.(pdf|docx|txt|md|markdown)$/i.test(file.name)
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
  searchResults.value = []
  await loadMessages()
}

async function loadMessages() {
  chatMessages.value = []
  if (!activeSessionId.value) return
  const { data } = await axios.get(`/api/chat-sessions/${activeSessionId.value}/messages`)
  chatMessages.value = data.data || []
}

async function openCreateSession() {
  newSessionTitle.value = ''
  showNameSuggestions.value = false
  creatingSession.value = true
  await nextTick()
  sessionInput.value?.focus()
}

function closeCreateSession() {
  creatingSession.value = false
}

function applySuggestion(suggestion) {
  newSessionTitle.value = suggestion
}

async function createSession() {
  const { data } = await axios.post(`/api/documents/${selectedDocumentId.value}/chat-sessions`, {
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
  if (!searchQuery.value.trim()) return
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
  analysisState.value = '分析生成中'
  try {
    const { data } = await axios.post('/api/review/summary', { documentId: Number(selectedDocumentId.value) })
    reviewReport.value = data.data
    analysisState.value = '分析已生成'
  } catch (error) {
    analysisState.value = '分析生成失败'
  }
}

async function recommendPapers() {
  const query = [selectedDocument.value?.title, selectedDocument.value?.summary].filter(Boolean).join(' ')
  const { data } = await axios.post('/api/papers/recommend', { query, limit: 5 })
  papers.value = data.data || []
}

function formatTime(value) {
  if (!value) return '未知'
  return new Date(value).toLocaleString()
}

onMounted(loadDocuments)
</script>
