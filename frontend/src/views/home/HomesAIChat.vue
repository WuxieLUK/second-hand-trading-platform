<script setup lang="js">
import { ref, nextTick, onMounted, computed } from 'vue';
import {
  Loading, Headset, ArrowRight, Delete, CopyDocument, Check,
  Plus, Message, ChatDotRound, Edit,
  ArrowDownBold, Top, MoreFilled
} from '@element-plus/icons-vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { getAiChatUrl } from '@/utils/serviceUrls.js';

// --- 引入 Markdown 解析与高亮 ---
import markdownit from 'markdown-it';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';

const emit = defineEmits(['update:modelValue']);

// 初始化 Markdown 解析器
const md = markdownit({
  html: true, linkify: true, typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
            hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
            '</code></pre>';
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>';
  }
});

const getImageBase = () => {
  const base = import.meta.env.VITE_API_BASE_URL || '';
  const prefix = import.meta.env.VITE_API_PREFIX || import.meta.env.VITE_API_BASENET_URL || '/api/backAll';
  if (base) return base.replace(/\/+$/, '') + prefix;
  return window.location.origin + prefix;
};

function repairJson(raw) {
  if (!raw) return raw;
  let s = raw.trim();
  s = s.replace(/[“”„‟″‶「」＂]/g, '"');
  s = s.replace(/(?<=")"(?=[\w一-鿿])/g, '":"');
  s = s.replace(/([,{])\s*"(\w+)"(\d+(?:\.\d+)?)/g, '$1"$2":$3');
  s = s.replace(/(https?:\/\/[^\s,"}]+)\[/g, '$1');
  s = s.replace(/\s+/g, ' ');
  s = s.replace(/https\/(?!\/)/g, 'https://');
  s = s.replace(/http\/(?!\/)/g, 'http://');
  return s;
}

/** 修正图片 URL（相对路径 → 绝对路径） */
function fixImageUrl(imgSrc) {
  if (!imgSrc) return '';
  if (imgSrc.startsWith('http://') || imgSrc.startsWith('https://') || imgSrc.startsWith('data:')) {
    return imgSrc;
  }
  return getImageBase() + imgSrc;
}

/**
 * 将 AI 回复内容解析为「文本段」和「卡片段」
 * - 文本段：markdown 文本，通过 md.render() 转为 HTML 后用 v-html 渲染
 * - 卡片段：[PRODUCT_CARD] JSON [/PRODUCT_CARD] 块，用于 Vue 模板渲染（解决 v-html 样式穿透问题）
 */
function renderWithCards(content) {
  if (!content) return '';
  let processed = content;
  processed = processed.replace(
      /\[PRODUCT_CARD\]\s*(\{.*?\})\s*\[\/PRODUCT_CARD\]/gs,
      (match, jsonStr) => {
        try {
          const data = JSON.parse(repairJson(jsonStr));
          const imgSrc = fixImageUrl(data.image || '');
          const price = Number(data.price || 0).toFixed(2);
          const originalPrice = data.originalPrice && Number(data.originalPrice) > 0
              ? Number(data.originalPrice).toFixed(2) : null;
          const stock = data.stock !== undefined ? data.stock : null;

          return '<div style="margin:12px 0;max-width:380px">'
            + '<div style="display:flex;gap:14px;padding:14px;background:#f8f9ff;'
            + 'border:1px solid #e8ecf4;border-radius:12px;transition:all 0.2s"'
            + ' onmouseover="this.style.borderColor=\'#667eea\';this.style.boxShadow=\'0 4px 16px rgba(102,126,234,0.15)\';this.style.transform=\'translateY(-2px)\'"'
            + ' onmouseout="this.style.borderColor=\'#e8ecf4\';this.style.boxShadow=\'none\';this.style.transform=\'none\'">'
            + '<div style="width:100px;height:100px;border-radius:8px;overflow:hidden;flex-shrink:0;background:#f0f0f0">'
            + `<img src="${imgSrc}" alt="${data.name || ''}" style="width:100%;height:100%;object-fit:cover" loading="lazy" onerror="this.style.display='none'"/>`
            + '</div>'
            + '<div style="flex:1;display:flex;flex-direction:column;gap:6px;min-width:0">'
            + `<div style="font-size:15px;font-weight:600;color:#1a1a2e;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;line-height:1.4">${data.name || ''}</div>`
            + '<div style="display:flex;gap:6px;flex-wrap:wrap">'
            + (data.category ? `<span style="font-size:11px;background:#f0f2ff;padding:2px 8px;border-radius:4px;color:#667eea;font-weight:500">${data.category}</span>` : '')
            + (data.degree ? `<span style="font-size:11px;background:#f0f2ff;padding:2px 8px;border-radius:4px;color:#667eea;font-weight:500">${data.degree}</span>` : '')
            + (stock !== null ? `<span style="font-size:11px;background:#fff7e6;padding:2px 8px;border-radius:4px;color:#fa8c16;font-weight:500">库存 ${stock}</span>` : '')
            + '</div>'
            + '<div style="display:flex;align-items:baseline;gap:8px;margin-top:2px">'
            + `<span style="font-size:18px;font-weight:700;color:#ff4d4f">&yen;${price}</span>`
            + (originalPrice ? `<span style="font-size:12px;color:#bbb;text-decoration:line-through">&yen;${originalPrice}</span>` : '')
            + '</div>'
            + '<div style="display:flex;gap:8px;margin-top:4px">'
            + `<a href="/goods/detail/${data.id}" target="_blank" style="display:inline-flex;align-items:center;justify-content:center;padding:5px 14px;border-radius:6px;font-size:12px;font-weight:500;text-decoration:none;transition:all 0.2s;cursor:pointer;border:1px solid #d9d9d9;color:#666;background:#fff" onmouseover="this.style.borderColor='#667eea';this.style.color='#667eea'" onmouseout="this.style.borderColor='#d9d9d9';this.style.color='#666'">查看详情</a>`
            + `<a href="/payment?goodsId=${data.id}" target="_blank" style="display:inline-flex;align-items:center;justify-content:center;padding:5px 14px;border-radius:6px;font-size:12px;font-weight:500;text-decoration:none;transition:all 0.2s;cursor:pointer;border:1px solid #667eea;color:#fff;background:linear-gradient(135deg,#667eea,#764ba2)" onmouseover="this.style.opacity='0.9';this.style.transform='translateY(-1px)';this.style.boxShadow='0 2px 8px rgba(102,126,234,0.3)'" onmouseout="this.style.opacity='1';this.style.transform='none';this.style.boxShadow='none'">立即下单</a>`
            + '</div>'
            + '</div>'
            + '</div>'
            + '</div>';
        } catch (e) {
          console.warn('[renderWithCards] JSON parse failed:', e.message, 'raw:', match);
          return match;
        }
      }
  );
  return md.render(processed);
}

// 暴露到 window 方便控制台手动测试
function hasProductCards(content) {
  if (!content) return false;
  return /\[PRODUCT_CARD\]/g.test(content);
}

// ========== 会话管理 ==========

const STORAGE_KEY = 'ai-chat-sessions';
const genId = () => Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 8);

const sessions = ref({});
const currentSessionId = ref('');
const showSidebar = ref(true);
const windowWidth = ref(window.innerWidth);
const openMenuId = ref(null);

// 当前会话消息列表
const chatList = computed({
  get: () => {
    if (!currentSessionId.value || !sessions.value[currentSessionId.value]) return [];
    return sessions.value[currentSessionId.value].messages;
  },
  set: (val) => {
    if (currentSessionId.value && sessions.value[currentSessionId.value]) {
      sessions.value[currentSessionId.value].messages = val;
      saveToStorage();
    }
  }
});

const sessionList = computed(() =>
    Object.values(sessions.value).sort((a, b) => {
      if (a.pinned && !b.pinned) return -1;
      if (!a.pinned && b.pinned) return 1;
      return b.createdAt - a.createdAt;
    })
);

const currentSession = computed(() =>
    currentSessionId.value ? sessions.value[currentSessionId.value] : null
);

const currentTitle = computed(() => currentSession.value?.title || '');

// 获取最近一条消息的预览
function getLastPreview(session) {
  if (!session?.messages) return '';
  const msgs = session.messages;
  for (let i = msgs.length - 1; i >= 0; i--) {
    if (msgs[i].role === 'user' && msgs[i].content) {
      return msgs[i].content.slice(0, 30) + (msgs[i].content.length > 30 ? '...' : '');
    }
  }
  return '';
}

// 计算消息数量
function getMsgCount(session) {
  if (!session?.messages) return 0;
  return session.messages.filter(m => m.role !== 'faq').length;
}

// 当前编辑中的会话ID
const editingSessionId = ref(null);
const editingTitle = ref('');

function startRename(id) {
  const s = sessions.value[id];
  if (!s) return;
  editingSessionId.value = id;
  editingTitle.value = s.title;
  nextTick(() => {
    const el = document.querySelector('.rename-input');
    if (el) el.focus();
  });
}

function confirmRename() {
  if (editingSessionId.value && sessions.value[editingSessionId.value]) {
    sessions.value[editingSessionId.value].title = editingTitle.value.trim() || '新对话';
    saveToStorage();
  }
  editingSessionId.value = null;
  editingTitle.value = '';
}

function saveToStorage() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      currentSessionId: currentSessionId.value,
      sessions: sessions.value
    }));
  } catch (e) { console.warn('保存失败:', e); }
}

function loadFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const data = JSON.parse(raw);
      if (data.sessions) sessions.value = data.sessions;
      if (data.currentSessionId && sessions.value[data.currentSessionId]) {
        currentSessionId.value = data.currentSessionId;
      }
    }
  } catch (e) { console.warn('恢复失败:', e); }
}

function createSession(title) {
  const id = genId();
  sessions.value = {
    ...sessions.value,
    [id]: {
      id, title: title || '新对话',
      messages: [{ role: 'ai', content: 'Hi，我是智能客服吴桐，请问有什么可以帮您~', timestamp: Date.now() }],
      createdAt: Date.now(),
      pinned: false
    }
  };
  currentSessionId.value = id;
  saveToStorage();
  if (window.innerWidth < 768) showSidebar.value = false;
  return id;
}

function switchSession(id) {
  if (id && sessions.value[id]) {
    currentSessionId.value = id;
    saveToStorage();
    if (window.innerWidth < 768) showSidebar.value = false;
  }
}

async function deleteSession(id) {
  if (!sessions.value[id]) return;
  if (sessions.value[id].messages.length > 2) {
    try {
      await ElMessageBox.confirm('确定删除此对话记录吗？此操作不可恢复。', '删除确认', {
        type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
      });
    } catch { return; }
  }
  const { [id]: removed, ...rest } = sessions.value;
  sessions.value = rest;
  if (currentSessionId.value === id) {
    const keys = Object.keys(rest);
    currentSessionId.value = keys.length > 0 ? keys[0] : '';
  }
  saveToStorage();
  if (!currentSessionId.value) createSession();
}

function updateSessionTitle(sessionId) {
  const session = sessions.value[sessionId];
  if (!session) return;
  const firstUserMsg = session.messages.find(m => m.role === 'user');
  if (firstUserMsg) {
    session.title = firstUserMsg.content.slice(0, 20) + (firstUserMsg.content.length > 20 ? '...' : '');
  }
}

// 切换置顶状态
function togglePin(sessionId) {
  const session = sessions.value[sessionId];
  if (session) {
    session.pinned = !session.pinned;
    openMenuId.value = null;
    saveToStorage();
  }
}

// 点击外部关闭菜单
function handleDocClick() {
  openMenuId.value = null;
}

// ========== 聊天逻辑 ==========

const inputMsg = ref('');
const isTyping = ref(false);
const isSearching = ref(false); // 查询阶段（SSE 等待中）
const scrollRef = ref(null);
const inputRef = ref(null);
const isScrolledUp = ref(false);

import { useTokenStore } from '@/stores/token.js'
const tokenStore = useTokenStore();

const agentChatUrl = () => {
  const base = getAiChatUrl();
  return base.replace('/chat', '/agent/chat/stream');
};

const copiedMsgId = ref(null);
function copyMessage(content, index) {
  navigator.clipboard.writeText(content).then(() => {
    copiedMsgId.value = index;
    setTimeout(() => { copiedMsgId.value = null; }, 2000);
  }).catch(() => ElMessage.warning('复制失败'));
}

function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  const now = new Date();
  const pad = n => String(n).padStart(2, '0');
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  if (d.toDateString() === now.toDateString()) return time;
  return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${time}`;
}

// 滚动监听
function handleScroll() {
  if (!scrollRef.value) return;
  const { scrollTop, scrollHeight, clientHeight } = scrollRef.value;
  isScrolledUp.value = scrollHeight - scrollTop - clientHeight > 100;
}

function scrollToBottom(smooth = true) {
  nextTick(() => {
    if (scrollRef.value) {
      scrollRef.value.scrollTo({
        top: scrollRef.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      });
    }
  });
}

// 输入框自动增高
function autoResize() {
  const el = inputRef.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = Math.min(el.scrollHeight, 120) + 'px';
}

// 发送消息
const sendMessage = async () => {
  const text = inputMsg.value.trim();
  if (!text || isTyping.value) return;

  if (!currentSessionId.value) createSession();
  const sessionId = currentSessionId.value;
  const session = sessions.value[sessionId];
  if (!session) return;

  session.messages.push({ role: 'user', content: text, timestamp: Date.now() });
  inputMsg.value = '';
  if (inputRef.value) inputRef.value.style.height = 'auto';
  saveToStorage();
  updateSessionTitle(sessionId);
  isSearching.value = true; // 进入"查询中"阶段
  isTyping.value = true;
  scrollToBottom();

  const aiMsgIndex = session.messages.push({
    role: 'ai', content: '', typing: true, timestamp: Date.now()
  }) - 1;
  saveToStorage();

  let rawTextBuffer = '';
  let currentDisplayText = '';
  const typingSpeed = 30;

  const typingTimer = setInterval(() => {
    const msgs = sessions.value[sessionId]?.messages;
    if (!msgs) { clearInterval(typingTimer); return; }
    if (currentDisplayText.length < rawTextBuffer.length) {
      currentDisplayText += rawTextBuffer.charAt(currentDisplayText.length);
      msgs[aiMsgIndex].content = currentDisplayText;
    } else if (!isTyping.value) {
      clearInterval(typingTimer);
      msgs[aiMsgIndex].typing = false;
      saveToStorage();
      scrollToBottom();
    }
  }, typingSpeed);

  try {
    if (!tokenStore.token) {
      ElMessage.warning('请先登录！');
      isTyping.value = false;
      isSearching.value = false;
      clearInterval(typingTimer);
      return;
    }
    const response = await fetch(agentChatUrl(), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': tokenStore.token },
      body: JSON.stringify({
        message: text,
        sessionId: sessionId,
        history: session.messages
            .filter(m => m.role !== 'faq')
            .slice(-20)
            .map(m => ({ role: m.role, content: m.content }))
      }),
    });
    if (!response.ok) throw new Error('网络响应异常');

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let firstChunk = true;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      for (const line of chunk.split('\n')) {
        if (line.startsWith('data:')) {
          const content = line.replace(/^data:/, '').trim();
          if (content === '[DONE]') continue;
          // 收到第一个数据块 → 查询阶段结束，进入打字阶段
          if (firstChunk) {
            firstChunk = false;
            isSearching.value = false;
          }
          rawTextBuffer += content;
        } else if (line.trim() && !line.includes(':')) {
          rawTextBuffer += line;
        }
      }
    }
  } catch (e) {
    console.error('流传输错误:', e);
    clearInterval(typingTimer);
    const msgs = sessions.value[sessionId]?.messages;
    if (msgs) {
      msgs[aiMsgIndex].content = '服务响应异常，请稍后再试。';
      msgs[aiMsgIndex].typing = false;
      saveToStorage();
    }
  } finally {
    isSearching.value = false;
    isTyping.value = false;
    scrollToBottom();
  }
};

// 键盘事件
function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
}

const clearChat = () => {
  if (!currentSessionId.value) return;
  ElMessageBox.confirm('确定清空当前对话记录吗？', '确认操作', { type: 'warning' }).then(() => {
    const session = sessions.value[currentSessionId.value];
    if (session) {
      session.messages = [
        { role: 'ai', content: 'Hi，我是智能客服吴桐，请问有什么可以帮您~', timestamp: Date.now() }
      ];
      session.title = '新对话';
      saveToStorage();
    }
  }).catch(() => {});
};

const handleFaqClick = (content) => {
  inputMsg.value = content;
  sendMessage();
};

// ========== 初始化 ==========

onMounted(() => {
  loadFromStorage();
  if (Object.keys(sessions.value).length === 0) createSession();
  if (!currentSessionId.value || !sessions.value[currentSessionId.value]) {
    const keys = Object.keys(sessions.value);
    if (keys.length > 0) currentSessionId.value = keys[0];
  }
  const handleResize = () => {
    windowWidth.value = window.innerWidth;
    showSidebar.value = window.innerWidth >= 768;
  };
  windowWidth.value = window.innerWidth;
  showSidebar.value = window.innerWidth >= 768;
  window.addEventListener('resize', handleResize);
  document.addEventListener('click', handleDocClick);
});
</script>

<template>
  <div class="ai-chat-layout">
    <!-- 侧边栏遮罩（移动端） -->
    <div v-if="showSidebar && windowWidth < 768" class="sidebar-overlay" @click="showSidebar = false"></div>

    <!-- 左侧：历史会话列表 -->
    <aside :class="['sidebar', { 'sidebar-open': showSidebar }]">
      <div class="sidebar-header">
        <div class="sidebar-top">
          <span class="sidebar-title">历史记录</span>
          <span class="sidebar-count">{{ Object.keys(sessions).length }}</span>
        </div>
        <button class="new-chat-btn" @click="createSession()">
          <el-icon><Plus /></el-icon>
          <span>新建对话</span>
        </button>
      </div>
      <div class="sidebar-scroll">
        <div
          v-for="session in sessionList"
          :key="session.id"
          :class="['session-item', { active: session.id === currentSessionId }]"
          @click="switchSession(session.id)"
        >
          <div class="session-content">
            <div class="session-title-row">
              <el-icon class="session-icon"><Message /></el-icon>
              <el-icon v-if="session.pinned" class="pin-icon"><Top /></el-icon>
              <!-- 编辑模式 -->
              <input
                v-if="editingSessionId === session.id"
                v-model="editingTitle"
                class="rename-input"
                @blur="confirmRename"
                @keyup.enter="confirmRename"
                @click.stop
              />
              <span v-else class="session-title" :title="session.title">{{ session.title }}</span>
              <div class="session-more" @click.stop>
                <el-icon class="more-trigger" :class="{ active: openMenuId === session.id }" @click="openMenuId = openMenuId === session.id ? null : session.id">
                  <MoreFilled />
                </el-icon>
                <transition name="drop-down">
                  <div v-if="openMenuId === session.id" class="more-dropdown">
                    <div class="more-item" @click="togglePin(session.id)">
                      <el-icon><Top /></el-icon>
                      <span>{{ sessions[session.id]?.pinned ? '取消置顶' : '置顶' }}</span>
                    </div>
                    <div class="more-item" @click="startRename(session.id); openMenuId = null">
                      <el-icon><Edit /></el-icon>
                      <span>重命名</span>
                    </div>
                    <div class="more-divider"></div>
                    <div class="more-item danger" @click="deleteSession(session.id); openMenuId = null">
                      <el-icon><Delete /></el-icon>
                      <span>删除</span>
                    </div>
                  </div>
                </transition>
              </div>
            </div>
            <div class="session-meta">
              <span class="session-preview">{{ getLastPreview(session) }}</span>
              <span class="session-info">
                <span class="msg-count">{{ getMsgCount(session) }} 条</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右侧：聊天主区域 -->
    <main class="main-area">
      <!-- 顶栏 -->
      <header class="chat-header">
        <div class="header-left">
          <button class="menu-toggle" @click="showSidebar = !showSidebar">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
              <rect x="3" y="6" width="18" height="2" rx="1"/>
              <rect x="3" y="11" width="18" height="2" rx="1"/>
              <rect x="3" y="16" width="18" height="2" rx="1"/>
            </svg>
          </button>
          <div class="header-info">
            <span class="chat-title">AI 智能客服</span>
            <span v-if="currentTitle && currentTitle !== '新对话'" class="chat-subtitle">{{ currentTitle }}</span>
          </div>
        </div>
        <button class="header-clear" @click="clearChat">清空对话</button>
      </header>

      <!-- 消息区 -->
      <div class="chat-messages" ref="scrollRef" @scroll="handleScroll">
        <div v-if="chatList.length === 0" class="empty-state">
          <el-icon class="empty-icon"><ChatDotRound /></el-icon>
          <p>开始一段新的对话吧</p>
          <p class="empty-hint">可以试试问我：有哪些二手教材？便宜的数码产品？</p>
        </div>

        <div v-for="(msg, index) in chatList" :key="index" :class="['msg-row', msg.role]">
          <!-- AI 消息 -->
          <div v-if="msg.role === 'ai'" class="msg-ai">
            <div class="msg-avatar">
              <el-icon><Headset /></el-icon>
            </div>
            <div class="msg-content">
              <span class="msg-author">吴桐</span>

              <!-- 打字中：单行气泡 -->
              <div v-if="msg.typing" class="msg-bubble ai-bubble typing-bubble">
                {{ msg.content || (isSearching ? '正在为你查询...' : '') }}<span class="cursor">|</span>
              </div>

              <!-- 打字完成：markdown + 商品卡片（内联样式） -->
              <div v-if="!msg.typing" class="msg-bubble ai-bubble markdown-body" v-html="renderWithCards(msg.content)"></div>

              <div v-if="!msg.typing && msg.content" class="msg-tools">
                <button class="tool-btn" @click="copyMessage(msg.content, index)" :title="copiedMsgId === index ? '已复制' : '复制'">
                  <el-icon v-if="copiedMsgId !== index"><CopyDocument /></el-icon>
                  <el-icon v-else style="color:#67c23a"><Check /></el-icon>
                </button>
                <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
              </div>

              <!-- 无结果引导 -->
              <div v-if="!msg.typing && msg.content && !hasProductCards(msg.content) && msg.content.indexOf('异常') === -1" class="no-result-tip">
                <span>没有找到想要的商品？试试换个关键词，或</span>
                <a href="/homes/home" target="_blank" class="tip-link">去分类浏览</a>
              </div>
            </div>
          </div>

          <!-- 常见问题 -->
          <div v-else-if="msg.role === 'faq'" class="msg-faq" @click="handleFaqClick(msg.content)">
            <span>{{ msg.content }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>

          <!-- 用户消息 -->
          <div v-else class="msg-user">
            <div class="msg-bubble user-bubble">{{ msg.content }}</div>
            <span class="msg-time user-time">{{ formatTime(msg.timestamp) }}</span>
          </div>
        </div>

        <!-- 底部留白 -->
        <div class="scroll-anchor"></div>
      </div>

      <!-- 滚动到底部按钮 -->
      <transition name="fade">
        <button v-if="isScrolledUp" class="scroll-down-btn" @click="scrollToBottom()">
          <el-icon><ArrowDownBold /></el-icon>
        </button>
      </transition>

      <!-- 输入区 -->
      <footer class="chat-footer">
        <div v-if="isTyping" class="typing-bar">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>{{ isSearching ? '正在为你查询商品...' : 'AI 正在回复...' }}</span>
        </div>
        <div class="input-area">
          <div class="input-wrap">
            <textarea
              ref="inputRef"
              v-model="inputMsg"
              class="msg-input"
              placeholder="输入您的问题，Enter 发送，Shift+Enter 换行"
              rows="1"
              @keydown="handleKeydown"
              @input="autoResize"
            ></textarea>
            <button
              class="send-btn"
              :disabled="!inputMsg.trim() || isTyping"
              @click="sendMessage"
            >
              <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </button>
          </div>
        </div>
      </footer>
    </main>
  </div>
</template>

<style scoped lang="scss">
/* ====== 全局布局 ====== */
.ai-chat-layout {
  display: flex;
  height: 100vh;
  padding-top: 56px;
  box-sizing: border-box;
  background: #f0f2f5;
  overflow: hidden;
}

/* ====== 侧边栏 ====== */
.sidebar-overlay {
  display: none;
}
.sidebar {
  width: 300px;
  min-width: 300px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease, width 0.3s ease;

  .sidebar-header {
    padding: 16px 16px 12px;
    border-bottom: 1px solid #f0f0f0;
    .sidebar-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 12px;
      .sidebar-title { font-size: 17px; font-weight: 700; color: #1a1a2e; }
      .sidebar-count {
        font-size: 12px; color: #999; background: #f0f2f5;
        padding: 2px 8px; border-radius: 10px;
      }
    }
    .new-chat-btn {
      width: 100%; display: flex; align-items: center; justify-content: center;
      gap: 6px; padding: 9px 0; border: 1px dashed #d9d9d9;
      border-radius: 8px; background: transparent; color: #666;
      font-size: 13px; cursor: pointer; transition: all 0.2s;
      &:hover { border-color: #667eea; color: #667eea; background: #f8f9ff; }
    }
  }

  .sidebar-scroll {
    flex: 1; overflow-y: auto; padding: 8px;
    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }
  }

  .session-item {
    padding: 12px; border-radius: 10px; cursor: pointer;
    transition: all 0.2s; margin-bottom: 4px;
    &:hover {
      background: #f5f7fa;
      .session-preview { color: #555; }
    }
    &.active {
      background: #eef2ff;
      .session-title { color: #4f46e5; font-weight: 600; }
    }
    .session-content { min-width: 0; }
    .session-title-row {
      display: flex; align-items: center; gap: 8px;
      margin-bottom: 4px;
      .session-icon {
        font-size: 14px; color: #999; flex-shrink: 0;
        .active & { color: #4f46e5; }
      }
      .pin-icon {
        font-size: 12px; color: #4f46e5; flex-shrink: 0;
        transform: rotate(45deg); margin-left: -4px;
      }
      .session-title {
        flex: 1; font-size: 14px; color: #333; font-weight: 500;
        overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
      }
      .rename-input {
        flex: 1; font-size: 14px; border: 1px solid #4f46e5;
        border-radius: 4px; padding: 2px 6px; outline: none;
      }
      .session-more {
        position: relative; flex-shrink: 0;
        .more-trigger {
          font-size: 16px; border-radius: 4px;
          cursor: pointer; color: #bbb; transition: all 0.2s;
          display: flex; align-items: center; justify-content: center;
          &:hover, &.active { background: rgb(181 179 179 / 0.66); color: #4f46e5; }
        }
        .more-dropdown {
          position: absolute; right: 0; top: 100%; z-index: 20;
          min-width: 130px; background: #fff; border-radius: 8px;
          box-shadow: 0 4px 16px rgba(0,0,0,0.12); padding: 4px;
          margin-top: 4px;
          .more-item {
            display: flex; align-items: center; gap: 8px;
            padding: 8px 12px; font-size: 13px; color: #333;
            border-radius: 6px; cursor: pointer; transition: all 0.15s;
            .el-icon { font-size: 15px; color: #999; }
            &:hover { background: #f5f7fa; color: #4f46e5;
              .el-icon { color: #4f46e5; }
            }
            &.danger:hover { background: #fef2f2; color: #ef4444;
              .el-icon { color: #ef4444; }
            }
          }
          .more-divider { height: 1px; background: #f0f0f0; margin: 4px 8px; }
        }
      }
    }
    .session-meta {
      padding-left: 22px; display: flex; flex-direction: column; gap: 2px;
      .session-preview {
        font-size: 12px; color: #bbb; overflow: hidden;
        text-overflow: ellipsis; white-space: nowrap; transition: color 0.2s;
      }
      .session-info {
        display: flex; align-items: center; gap: 8px;
        .msg-count { font-size: 11px; color: #ccc; }
      }
    }
  }
}

/* ====== 主区域 ====== */
.main-area {
  flex: 1; display: flex; flex-direction: column; min-width: 0;
  position: relative; background: #f5f7fa;
}

.chat-header {
  padding: 14px 20px; background: #fff;
  border-bottom: 1px solid #f0f0f0;
  display: flex; align-items: center; justify-content: space-between;

  .header-left {
    display: flex; align-items: center; gap: 12px;
    .menu-toggle {
      display: none; padding: 6px; border: none; background: none;
      cursor: pointer; color: #666; border-radius: 6px;
      &:hover { background: #f0f0f0; }
    }
    .header-info {
      display: flex; align-items: baseline; gap: 8px;
      .chat-title { font-size: 16px; font-weight: 700; color: #1a1a2e; white-space: nowrap; }
      .chat-subtitle {
        font-size: 13px; color: #aaa; overflow: hidden;
        text-overflow: ellipsis; white-space: nowrap; max-width: 300px;
      }
    }
  }

  .header-clear {
    font-size: 13px; color: #999; background: none; border: none;
    cursor: pointer; padding: 6px 12px; border-radius: 6px;
    transition: all 0.2s; white-space: nowrap;
    &:hover { background: #f5f5f5; color: #ef4444; }
  }
}

/* ====== 消息区 ====== */
.chat-messages {
  flex: 1; overflow-y: auto; padding: 20px 24px;
  display: flex; flex-direction: column; gap: 20px;
  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background: #d0d0d0; border-radius: 3px; }

  .scroll-anchor { height: 1px; flex-shrink: 0; }
}

.empty-state {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: center; color: #ccc; gap: 12px;
  .empty-icon { font-size: 48px; }
  p { font-size: 15px; margin: 0; }
  .empty-hint { font-size: 13px; color: #d0d0d0; margin-top: 4px; }
}

/* 无结果引导 */
.no-result-tip {
  margin-top: 6px; padding-left: 4px; font-size: 12px; color: #aaa;
  display: flex; align-items: center; gap: 4px; flex-wrap: wrap;
  .tip-link { color: #667eea; text-decoration: none; font-weight: 500;
    &:hover { text-decoration: underline; }
  }
}

/* 消息行 */
.msg-row {
  display: flex; flex-direction: column;
  &.ai { align-items: flex-start; }
  &.user { align-items: flex-end; }
}

.msg-ai {
  display: flex; gap: 10px; max-width: 78%;
  .msg-avatar {
    flex-shrink: 0; width: 34px; height: 34px;
    background: linear-gradient(135deg, #f59e0b, #f97316);
    border-radius: 50%; display: flex; align-items: center;
    justify-content: center; color: #fff; font-size: 18px;
    box-shadow: 0 2px 6px rgba(245, 158, 11, 0.3);
  }
  .msg-content { min-width: 0; }
  .msg-author { font-size: 12px; color: #999; margin-bottom: 4px; display: block; }
  .ai-bubble {
    background: #fff; color: #333; padding: 14px 18px;
    border-radius: 4px 16px 16px 16px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.04);
    line-height: 1.65; font-size: 14px;
  }
  .typing-bubble { min-width: 60px; }
  .cursor { animation: blink 0.8s step-end infinite; color: #667eea; font-weight: 700; }
  @keyframes blink { 50% { opacity: 0; } }
  .msg-tools {
    display: flex; align-items: center; gap: 8px; margin-top: 4px; padding-left: 4px;
    .tool-btn {
      width: 26px; height: 26px; display: flex; align-items: center;
      justify-content: center; border: none; background: transparent;
      border-radius: 6px; cursor: pointer; color: #bbb; font-size: 13px;
      transition: all 0.2s;
      &:hover { background: #f0f0f0; color: #667eea; }
    }
    .msg-time { font-size: 11px; color: #ccc; }
  }
}

.msg-user {
  max-width: 70%; display: flex; flex-direction: column; align-items: flex-end; gap: 2px;
  .user-bubble {
    background: linear-gradient(135deg, #4f46e5, #6366f1);
    color: #fff; padding: 12px 18px; border-radius: 16px 4px 16px 16px;
    line-height: 1.65; font-size: 14px;
    box-shadow: 0 2px 8px rgba(79, 70, 229, 0.2);
  }
  .user-time { font-size: 11px; color: #ccc; }
}

.msg-faq {
  background: #fff; color: #333; padding: 12px 16px; border-radius: 10px;
  display: flex; align-items: center; justify-content: space-between;
  cursor: pointer; transition: all 0.2s; max-width: 320px;
  border: 1px solid #eee; font-size: 14px;
  &:hover { background: #f9f9f9; border-color: #ddd; }
}

/* 滚动到底部按钮 */
.scroll-down-btn {
  position: absolute; bottom: 100px; left: 50%; transform: translateX(-50%);
  width: 36px; height: 36px; border-radius: 50%; border: 1px solid #e0e0e0;
  background: #fff; display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.08); color: #666;
  z-index: 10; transition: all 0.2s;
  &:hover { background: #f5f5f5; color: #4f46e5; transform: translateX(-50%) translateY(-2px); }
}
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.drop-down-enter-active, .drop-down-leave-active { transition: opacity 0.15s, transform 0.15s; }
.drop-down-enter-from, .drop-down-leave-to { opacity: 0; transform: translateY(-4px); }

/* ====== 输入区 ====== */
.chat-footer {
  background: #fff; border-top: 1px solid #f0f0f0;
  .typing-bar {
    padding: 6px 20px; font-size: 12px; color: #999;
    display: flex; align-items: center; gap: 6px;
    border-bottom: 1px solid #f5f5f5;
  }
  .input-area { padding: 12px 20px 16px; }
  .input-wrap {
    display: flex; align-items: center; gap: 10px;
    background: #f5f7fa; border-radius: 12px;
    padding: 8px 8px 8px 16px;
    border: 1px solid transparent; transition: all 0.2s;
    &:focus-within {
      border-color: #6366f1; background: #fff;
      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
    }
    .msg-input {
      flex: 1; border: none; background: transparent; outline: none;
      font-size: 14px; color: #333; line-height: 1.5;
      resize: none; max-height: 120px; font-family: inherit;
      &::placeholder { color: #bbb; }
      &::-webkit-scrollbar { width: 3px; }
      &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }
    }
    .send-btn {
      width: 38px; height: 38px; flex-shrink: 0;
      display: flex; align-items: center; justify-content: center;
      border: none; border-radius: 10px; cursor: pointer;
      background: linear-gradient(135deg, #6366f1, #4f46e5);
      color: #fff; transition: all 0.2s;
      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
      }
      &:disabled { background: #d9d9d9; cursor: not-allowed; }
    }
  }
}

/* ====== Markdown ====== */
:deep(.markdown-body) {
  p { margin: 0 0 8px 0; &:last-child { margin-bottom: 0; } }
  ul, ol { padding-left: 1.5em; margin: 8px 0; }
  li { margin-bottom: 4px; }
  pre.hljs {
    margin: 10px 0; padding: 12px; border-radius: 8px;
    background: #1e293b; color: #e2e8f0; overflow-x: auto;
    code { font-family: 'Fira Code', monospace; font-size: 13px; padding: 0; background: none; color: inherit; }
  }
  code { background: #f1f5f9; color: #ef4444; padding: 2px 4px; border-radius: 4px; font-size: 0.9em; }
  strong { font-weight: 700; color: #0f172a; }
  h1, h2, h3, h4 { font-size: 1.05em; margin: 10px 0 6px 0; font-weight: 600; color: #0f172a; border-bottom: none; }
  blockquote { margin: 8px 0; padding: 6px 12px; border-left: 3px solid #667eea; background: #f8f9ff; color: #555; }
  table { border-collapse: collapse; margin: 8px 0; width: 100%;
    th, td { border: 1px solid #e2e8f0; padding: 6px 10px; font-size: 13px; }
    th { background: #f1f5f9; font-weight: 600; }
  }
  a { color: #667eea; text-decoration: none; &:hover { text-decoration: underline; } }
  hr { border: none; border-top: 1px solid #e2e8f0; margin: 12px 0; }
}

/* ====== 响应式 ====== */
@media (max-width: 768px) {
  .sidebar {
    position: fixed; left: 0; top: 56px; bottom: 0; z-index: 100;
    transform: translateX(-100%); width: 280px; min-width: 280px;
  }
  .sidebar-open { transform: translateX(0); }
  .sidebar-overlay {
    display: block; position: fixed; inset: 0; background: rgba(0,0,0,0.3); z-index: 99;
  }
  .chat-header .header-left .menu-toggle { display: inline-flex !important; }
  .chat-subtitle { display: none; }
  .msg-ai, .msg-user { max-width: 90%; }
}
</style>

