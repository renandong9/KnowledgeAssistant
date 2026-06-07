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
            <div class="document-title-card">
              <h2>{{ articleTitle }}</h2>
              <p>{{ articleTitleCn }}</p>
              <time>{{ formatTime(selectedDocument.createTime) }}</time>
            </div>
          </section>

          <section class="panel-section">
            <article class="main-summary-card">
              <span>主要内容</span>
              <ul v-if="figureSummaries.length" class="figure-summary-list">
                <li v-for="item in figureSummaries" :key="item.label">
                  <strong>{{ item.label }}</strong>
                  <p>{{ item.summary }}</p>
                </li>
              </ul>
              <p v-else>暂未从原文中识别到科研图说明。</p>
            </article>
          </section>

          <section class="panel-section">
            <h2>章节结构</h2>
            <ol class="structure-list">
              <li v-for="item in structureItems" :key="item">{{ item }}</li>
            </ol>
          </section>

          <section class="panel-section">
            <div class="section-head">
              <h2>AI 自动分析结果</h2>
              <button @click="rebuildAnalysis" :disabled="analysisLoading">重新生成</button>
            </div>
            <div v-if="analysis" class="analysis-grid">
              <article><strong>研究背景</strong><p>{{ analysis.researchBackground }}</p></article>
              <article><strong>问题定义</strong><p>{{ analysis.problemDefinition }}</p></article>
              <article><strong>核心方法</strong><p>{{ analysis.coreMethod }}</p></article>
              <article><strong>实验结果</strong><p>{{ analysis.experimentResults }}</p></article>
              <article><strong>创新点</strong><p>{{ analysis.innovationPoints }}</p></article>
              <article><strong>优点</strong><p>{{ analysis.strengths }}</p></article>
              <article><strong>缺点</strong><p>{{ analysis.weaknesses }}</p></article>
              <article><strong>一句话总结</strong><p>{{ analysis.oneSentenceSummary }}</p></article>
            </div>
            <p v-else class="muted-text">{{ analysisLoading ? '分析生成中...' : '暂无分析结果，可点击重新生成。' }}</p>
          </section>

          <section class="panel-section">
            <div class="section-head">
              <h2>相关推荐论文</h2>
              <button @click="rebuildRecommendations" :disabled="recommendationLoading">重新推荐</button>
            </div>
            <article v-for="paper in papers" :key="paper.id || paper.url" class="paper-card">
              <strong>{{ paper.title }}</strong>
              <small>{{ paper.authors || '未知作者' }} · {{ paper.publishedYear || '未知年份' }}</small>
              <p>{{ paper.abstractText || paper.reason }}</p>
              <a v-if="paper.url" :href="paper.url" target="_blank">查看论文</a>
            </article>
            <p v-if="recommendationError" class="error-text">推荐服务暂不可用</p>
            <p v-if="!papers.length && !recommendationError" class="muted-text">暂无推荐结果。</p>
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
            <span v-for="state in pageStates" :key="state.label" :class="['state-item', state.tone]">{{ state.label }}</span>
          </div>

          <div class="messages">
            <article v-for="message in chatMessages" :key="message.id" :class="['message', message.role]">
              <strong>{{ message.role === 'user' ? '我' : '助手' }}</strong>
              <p>{{ message.content }}</p>
            </article>
            <p v-if="!chatMessages.length" class="muted-text">
              这个窗口还没有对话。提问时会优先围绕“{{ activeSession?.title || '默认问答' }}”检索当前文章。
            </p>
          </div>

          <div class="ask-box">
            <textarea v-model="question" :placeholder="questionPlaceholder"></textarea>
            <button @click="chat" :disabled="!activeSessionId || !question.trim() || chatLoading">
              {{ chatLoading ? '思考中...' : '提问' }}
            </button>
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
import { computed, nextTick, onMounted, ref } from 'vue'
import { api } from './api'

const documents = ref([])
const selectedDocumentId = ref('')
const chatSessions = ref([])
const activeSessionId = ref('')
const chatMessages = ref([])
const searchQuery = ref('')
const searchResults = ref([])
const question = ref('')
const analysis = ref(null)
const documentChunks = ref([])
const papers = ref([])
const creatingSession = ref(false)
const newSessionTitle = ref('')
const showNameSuggestions = ref(false)
const uploadState = ref('idle')
const uploadError = ref('')
const isDragging = ref(false)
const analysisLoading = ref(false)
const recommendationLoading = ref(false)
const recommendationError = ref(false)
const chatLoading = ref(false)
const sessionInput = ref(null)

const nameSuggestions = ['整体理解', '研究背景', '核心方法', '实验结果', '公式理解', '创新点', '优缺点', '相关工作', '组会汇报', '我的疑问']

const selectedDocument = computed(() => documents.value.find(doc => doc.id === selectedDocumentId.value))
const activeSession = computed(() => chatSessions.value.find(session => session.id === activeSessionId.value))
const articleTitle = computed(() => selectedDocument.value?.title || selectedDocument.value?.originalFileName || '未命名文章')
const articleTitleCn = computed(() => selectedDocument.value?.titleCn || selectedDocument.value?.titleChinese || translateKnownTitle(articleTitle.value))
const figureSummaries = computed(() => extractFigureSummaries())
const uploadTitle = computed(() => uploadState.value === 'uploading' ? '上传中，请稍候...' : '拖入一篇论文或文章，开始智能理解')
const questionPlaceholder = computed(() => activeSession.value ? `围绕“${activeSession.value.title}”提问，回答会引用原文 Chunk` : '请先创建或选择提问窗口')
const structureItems = computed(() => {
  if (selectedDocument.value?.parseStatus !== 'COMPLETED') return ['等待文档解析完成', '等待 Chunk 索引构建']
  return ['标题与摘要', '正文主要章节', '实验与结论', '参考信息']
})
const pageStates = computed(() => [
  { label: label(selectedDocument.value?.parseStatus), tone: tone(selectedDocument.value?.parseStatus) },
  { label: label(selectedDocument.value?.ocrStatus), tone: tone(selectedDocument.value?.ocrStatus) },
  { label: label(selectedDocument.value?.indexStatus), tone: tone(selectedDocument.value?.indexStatus) },
  { label: label(selectedDocument.value?.analysisStatus), tone: tone(selectedDocument.value?.analysisStatus) }
])

async function loadDocuments() {
  documents.value = await api.listDocuments() || []
  if (selectedDocumentId.value) await refreshSelectedDocument()
}

async function selectDocument(doc) {
  selectedDocumentId.value = doc.id
  searchResults.value = []
  await refreshWorkspace()
}

async function refreshWorkspace() {
  await refreshSelectedDocument()
  await Promise.all([loadAnalysis(), loadChunks(), loadRecommendations(), loadChatSessions()])
}

async function refreshSelectedDocument() {
  if (!selectedDocumentId.value) return
  const detail = await api.getDocument(selectedDocumentId.value)
  const index = documents.value.findIndex(doc => doc.id === detail.id)
  if (index >= 0) documents.value[index] = detail
  else documents.value.unshift(detail)
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
    const doc = await api.uploadDocument(form)
    documents.value = [doc, ...documents.value.filter(item => item.id !== doc.id)]
    selectedDocumentId.value = doc.id
    uploadState.value = 'done'
    await refreshWorkspace()
  } catch (error) {
    uploadError.value = error.message || '上传失败，请稍后重试。'
    uploadState.value = 'idle'
  }
}

function isSupportedFile(file) {
  return /\.(pdf|docx|txt|md|markdown)$/i.test(file.name)
}

async function loadAnalysis() {
  analysis.value = await api.getAnalysis(selectedDocumentId.value)
}

async function loadChunks() {
  documentChunks.value = selectedDocumentId.value ? await api.getDocumentChunks(selectedDocumentId.value) || [] : []
}

async function rebuildAnalysis() {
  analysisLoading.value = true
  try {
    analysis.value = await api.rebuildAnalysis(selectedDocumentId.value)
    await refreshSelectedDocument()
  } finally {
    analysisLoading.value = false
  }
}

async function loadRecommendations() {
  recommendationError.value = false
  try {
    papers.value = await api.listRecommendations(selectedDocumentId.value) || []
  } catch {
    recommendationError.value = true
    papers.value = []
  }
}

async function rebuildRecommendations() {
  recommendationLoading.value = true
  recommendationError.value = false
  try {
    papers.value = await api.rebuildRecommendations(selectedDocumentId.value) || []
    await refreshSelectedDocument()
    if (!papers.value.length) recommendationError.value = true
  } catch {
    recommendationError.value = true
  } finally {
    recommendationLoading.value = false
  }
}

async function loadChatSessions() {
  chatSessions.value = await api.listChatSessions(selectedDocumentId.value) || []
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
  chatMessages.value = activeSessionId.value ? await api.listMessages(activeSessionId.value) || [] : []
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
  const session = await api.createChatSession(selectedDocumentId.value, { title: newSessionTitle.value, type: 'custom' })
  creatingSession.value = false
  await loadChatSessions()
  activeSessionId.value = session?.id || activeSessionId.value
  await loadMessages()
}

async function renameSession() {
  if (!activeSession.value) return
  const title = window.prompt('新的窗口名称', activeSession.value.title)
  if (!title?.trim()) return
  await api.updateChatSession(activeSession.value.id, { title, type: activeSession.value.type || 'custom' })
  await loadChatSessions()
}

async function deleteSession() {
  if (!activeSession.value) return
  if (!window.confirm(`删除提问窗口“${activeSession.value.title}”？`)) return
  await api.deleteChatSession(activeSession.value.id)
  await loadChatSessions()
}

async function search() {
  if (!searchQuery.value.trim()) return
  searchResults.value = await api.search({
    documentId: selectedDocumentId.value,
    query: searchQuery.value,
    topic: activeSession.value?.title,
    limit: 8
  }) || []
}

async function chat() {
  const text = question.value.trim()
  if (!text) return
  question.value = ''
  chatLoading.value = true
  try {
    const data = await api.chat({
      documentId: selectedDocumentId.value,
      sessionId: activeSessionId.value,
      question: text,
      limit: 6
    })
    await loadMessages()
    if (data?.references?.length) searchResults.value = data.references
  } finally {
    chatLoading.value = false
  }
}

function label(status) {
  const map = {
    PENDING: '等待中',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败',
    SKIPPED: '已跳过',
    NOT_REQUIRED: '无需 OCR',
    UNAVAILABLE: '服务暂不可用'
  }
  return map[status] || status || '未知'
}

function tone(status) {
  if (status === 'COMPLETED' || status === 'NOT_REQUIRED') return 'success'
  if (status === 'FAILED' || status === 'UNAVAILABLE') return 'danger'
  if (status === 'PROCESSING' || status === 'PENDING') return 'working'
  return 'muted'
}

function formatTime(value) {
  if (!value) return '未知'
  return new Date(value).toLocaleString()
}

function translateKnownTitle(title) {
  if (!title) return '中文题名待生成'
  if (/[\u4e00-\u9fff]/.test(title)) return title
  const normalized = title.toLowerCase()
  if (normalized.includes('photonic integrated beam delivery') && normalized.includes('rubidium')) {
    return '用于铷三维磁光阱的光子集成光束传输'
  }
  return '中文题名待生成'
}

function buildMainSummary() {
  if (analysis.value) {
    const parts = [
      cleanText(analysis.value.oneSentenceSummary),
      cleanText(analysis.value.researchBackground),
      cleanText(analysis.value.coreMethod),
      cleanText(analysis.value.experimentResults),
      cleanText(analysis.value.innovationPoints)
    ].filter(Boolean)
    if (parts.length) return parts.join(' ')
  }
  if (selectedDocument.value?.summary) {
    return cleanText(selectedDocument.value.summary)
  }
  return analysisLoading.value ? '智能体正在整理这篇文章的主要内容，请稍候。' : '智能体还没有生成主要内容总结。'
}

function cleanText(text) {
  if (!text || text.includes('原文没有足够信息')) return ''
  return text
    .replace(/<think>[\s\S]*?<\/think>/gi, '')
    .replace(/<\/?think>/gi, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function extractFigureSummaries() {
  const figures = []
  for (const chunk of documentChunks.value) {
    const text = cleanText(chunk.content || '')
    const matches = text.matchAll(/(?:Figure|Fig\.?|图)\s*([0-9]+[a-zA-Z]?)[\s.:：-]*([^。.!?！？]*(?:[。.!?！？][^。.!?！？]*){0,4})/g)
    for (const match of matches) {
      const label = `图 ${match[1]}`
      if (figures.some(item => item.label === label)) continue
      const summary = limitSentences(match[2] || '', 5)
      if (summary) figures.push({ label, summary })
    }
  }
  return figures.slice(0, 8)
}

function limitSentences(text, max) {
  return cleanText(text)
    .split(/(?<=[。.!?！？])\s*/)
    .filter(Boolean)
    .slice(0, max)
    .join(' ')
}

onMounted(loadDocuments)
</script>
