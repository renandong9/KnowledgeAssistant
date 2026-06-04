<template>
  <main class="shell">
    <section class="toolbar">
      <div>
        <p class="eyebrow">Personal Knowledge Base</p>
        <h1>个人知识库助手</h1>
      </div>
      <label class="upload">
        <input type="file" accept=".pdf,.md,.txt,.docx" @change="uploadDocument" />
        上传文档
      </label>
    </section>

    <section class="grid">
      <aside class="panel">
        <h2>文档</h2>
        <button @click="loadDocuments">刷新</button>
        <article v-for="doc in documents" :key="doc.id" class="doc" :class="{ active: selectedDocumentId === doc.id }" @click="selectedDocumentId = doc.id">
          <strong>{{ doc.title }}</strong>
          <span>{{ doc.fileType }} · {{ doc.parseStatus }}</span>
        </article>
      </aside>

      <section class="panel workbench">
        <nav class="tabs">
          <button v-for="tab in tabs" :key="tab.key" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
        </nav>

        <div v-if="activeTab === 'search'" class="stack">
          <textarea v-model="searchQuery" placeholder="搜索你上传过的知识..."></textarea>
          <button @click="search">搜索</button>
          <article v-for="item in searchResults" :key="item.chunkId" class="result">
            <strong>{{ item.title }}</strong>
            <p>{{ item.content }}</p>
            <small>score: {{ Number(item.score || 0).toFixed(3) }}</small>
          </article>
        </div>

        <div v-if="activeTab === 'chat'" class="stack">
          <textarea v-model="question" placeholder="基于知识库提问..."></textarea>
          <button @click="chat">提问</button>
          <article v-if="answer" class="answer">{{ answer }}</article>
        </div>

        <div v-if="activeTab === 'review'" class="stack">
          <input v-model="selectedDocumentId" placeholder="文档 ID" />
          <button @click="review">生成复盘</button>
          <article v-if="reviewReport" class="answer">{{ reviewReport.summary }}</article>
        </div>

        <div v-if="activeTab === 'papers'" class="stack">
          <textarea v-model="paperQuery" placeholder="输入研究主题或论文关键词..."></textarea>
          <button @click="recommendPapers">推荐论文</button>
          <article v-for="paper in papers" :key="paper.id || paper.url" class="result">
            <strong>{{ paper.title }}</strong>
            <p>{{ paper.abstractText }}</p>
            <a :href="paper.url" target="_blank">查看论文</a>
          </article>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import axios from 'axios'
import { onMounted, ref } from 'vue'

const tabs = [
  { key: 'search', label: '搜索' },
  { key: 'chat', label: '问答' },
  { key: 'review', label: '复盘' },
  { key: 'papers', label: '论文' }
]

const activeTab = ref('search')
const documents = ref([])
const selectedDocumentId = ref('')
const searchQuery = ref('')
const searchResults = ref([])
const question = ref('')
const answer = ref('')
const reviewReport = ref(null)
const paperQuery = ref('')
const papers = ref([])

async function loadDocuments() {
  const { data } = await axios.get('/api/documents')
  documents.value = data.data || []
}

async function uploadDocument(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const form = new FormData()
  form.append('file', file)
  await axios.post('/api/documents/upload', form)
  await loadDocuments()
  event.target.value = ''
}

async function search() {
  const { data } = await axios.post('/api/search', { query: searchQuery.value, limit: 8 })
  searchResults.value = data.data || []
}

async function chat() {
  const { data } = await axios.post('/api/chat', { question: question.value, limit: 6 })
  answer.value = data.data?.answer || ''
}

async function review() {
  const { data } = await axios.post('/api/review/summary', { documentId: Number(selectedDocumentId.value) })
  reviewReport.value = data.data
}

async function recommendPapers() {
  const { data } = await axios.post('/api/papers/recommend', { query: paperQuery.value, limit: 5 })
  papers.value = data.data || []
}

onMounted(loadDocuments)
</script>
