import { createApp } from 'vue'
import axios from 'axios'
import './style.css'
import App from './App.vue'

axios.defaults.baseURL = 'http://localhost:8080'

createApp(App).mount('#app')
