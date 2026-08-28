<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue';
import { ElMessage, ElPopover } from 'element-plus';
import defaultAvater from '@/assets/default.png'
import { useRouter } from 'vue-router'
import EmojiPicker from 'vue3-emoji-picker';
import 'vue3-emoji-picker/css'

import {
  getMyChatListService,
  getSessionMsgService,
  sendChatMsgService,
  markChatMsgAsReadService,
} from "@/api/chat.js";
import { useTokenStore } from "@/stores/token.js";
import useUserInfoStore from "@/stores/userInfo.js";
import { getGatewayWsBaseUrl } from "@/utils/serviceUrls.js";

// ========== 状态管理 ==========
const userInfoStore = useUserInfoStore();
const tokenStore = useTokenStore();
const router = useRouter()

// ========== 响应式数据 ==========
const chatList = ref([]);
const msgList = ref([]);
const currentSessionId = ref(null);
const inputContent = ref("");
const userId = computed(() => userInfoStore.info?.id);
const pageNum = ref(1);
const pageSize = ref(20);
let ws = null;
const wsAuthenticated = ref(false);

const showEmojiPanel = ref(false);

const currentSessionTradeInfo = computed(() => {
  const session = chatList.value.find(item => item.id === currentSessionId.value);
  if (!session) return null;
  return {
    title: session.lastMsg,
    previewImg: session.friendAvatar,
  };
});

// ========== 方法定义 ==========
const loadChatList = async () => {
  try {
    const res = await getMyChatListService();
    const myId = userId.value;
    chatList.value = res.data.map(session => ({
      ...session,
      unreadCount: session.fromUserId === myId ? session.toUnread : session.fromUnread,
      time: formatTime(session.lastMsgTime)
    }));
  } catch (error) {
    ElMessage.error("加载聊天列表失败");
    console.error(error);
  }
};

const enterChat = async (sessionId) => {
  currentSessionId.value = sessionId;
  pageNum.value = 1;
  await loadChatMsg();
  await markAsRead(sessionId);
};

// 移动端返回列表
const goBackToList = () => {
  currentSessionId.value = null;
  loadChatList(); // 返回时刷新一下列表状态
};

const loadChatMsg = async () => {
  if (!currentSessionId.value) return;
  try {
    const res = await getSessionMsgService(
        currentSessionId.value,
        pageNum.value,
        pageSize.value
    );
    msgList.value = res.data.items;
    await scrollToBottom();
  } catch (error) {
    ElMessage.error("加载消息失败");
    console.error(error);
  }
};

const scrollToBottom = async () => {
  await nextTick(() => {
    const msgListEl = document.querySelector(".msg-list");
    if (msgListEl) {
      msgListEl.scrollTop = msgListEl.scrollHeight;
    }
  });
};

const getReceiverId = () => {
  const currentSession = chatList.value.find(item => item.id === currentSessionId.value);
  if (!currentSession) return null;
  return currentSession.fromUserId === userId.value
      ? currentSession.toUserId
      : currentSession.fromUserId;
};

const insertEmoji = (emoji) => {
  inputContent.value += emoji.i || emoji;
  document.querySelector('.msg-input')?.focus();
};

const sendMsg = async () => {
  if (!inputContent.value.trim()) {
    ElMessage.warning("消息内容不能为空");
    return;
  }

  const receiverId = getReceiverId();
  if (!receiverId) {
    ElMessage.error("获取接收者信息失败");
    return;
  }

  try {
    const res = await sendChatMsgService({
      sessionId: currentSessionId.value,
      receiverId,
      content: inputContent.value.trim()
    });

    msgList.value.push(res.data);
    inputContent.value = "";
    showEmojiPanel.value = false;

    await scrollToBottom();
    await loadChatList(); // 同步更新左侧列表的最后一条消息
  } catch (error) {
    ElMessage.error("发送消息失败");
    console.error(error);
  }
};

const markAsRead = async (sessionId) => {
  try {
    await markChatMsgAsReadService(sessionId);
    await loadChatList();
  } catch (error) {
    console.error("标记已读失败", error);
  }
};

const initWebSocket = () => {
  if (!userId.value) return;
  if (ws) ws.close();

  wsAuthenticated.value = false;
  const wsUrl = `${getGatewayWsBaseUrl()}/ws/chat`;
  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.log("WebSocket连接已建立，发送鉴权消息...");
    // 连接建立后立即发送鉴权消息（不在URL中暴露token）
    ws.send(JSON.stringify({
      msgType: -2,
      content: tokenStore.token,
    }));
  };

  ws.onmessage = (event) => {
    try {
      const msg = JSON.parse(event.data);

      // ---- 处理鉴权响应 ----
      if (msg.msgType === -2) {
        if (msg.content === 'auth_ok') {
          wsAuthenticated.value = true;
          console.log("WebSocket鉴权成功");
        } else if (msg.content === 'auth_fail') {
          wsAuthenticated.value = false;
          console.error("WebSocket鉴权失败");
          ElMessage.error("连接鉴权失败，请重新登录");
        }
        return;
      }

      // ---- 心跳包 ----
      if (msg.msgType === 0 && msg.content === "ping") return;

      // ---- 业务消息 ----
      if (msg.sessionId === currentSessionId.value) {
        msgList.value.push(msg);
        scrollToBottom();
        loadChatList();
        markAsRead(currentSessionId.value);
      } else {
        loadChatList();
        ElMessage.info("你有新的聊天消息");
      }
    } catch (error) {
      console.error("解析WebSocket消息失败：", error);
    }
  };

  ws.onclose = (event) => {
    wsAuthenticated.value = false;
    let reconnectTimer;
    if (event.code !== 1000 && event.code !== 1008) {
      reconnectTimer = setTimeout(() => { initWebSocket(); }, 3000);
    } else {
      if (event.code === 1008) {
        ElMessage.error(`连接失败：${event.reason}，请重新登录`);
      }
      if (reconnectTimer) clearTimeout(reconnectTimer);
    }
  };

  ws.onerror = (error) => console.error("WebSocket错误：", error);
};

const closeWebSocket = () => {
  wsAuthenticated.value = false;
  if (ws) {
    ws.close();
    ws = null;
  }
};

const formatTime = (timeStr) => {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
};

onMounted(() => {
  loadChatList();
  initWebSocket();
});

onUnmounted(() => {
  closeWebSocket();
});

const goToSellerDetail = () => {
  const receiverId = getReceiverId();
  if (!receiverId) {
    ElMessage.warning("获取联系人信息失败");
    return;
  }
  router.push(`/seller/detail/${receiverId}`);
}
</script>

<template>
  <div class="chat-page">
    <div class="chat-container">
      <div class="chat-main" :class="{ 'in-session': currentSessionId }">

        <div class="chat-list">
          <div class="chat-list-header">
            <h2>消息</h2>
          </div>
          <div class="chat-scroll-area">
            <div
                v-for="session in chatList"
                :key="session.id"
                class="chat-item"
                @click="enterChat(session.id)"
                :class="{ active: currentSessionId === session.id }"
            >
              <div class="avatar-wrapper">
                <img :src="session.friendAvatar || defaultAvater " class="avatar" alt="avatar" />
                <div v-if="session.unreadCount > 0" class="unread-badge">
                  {{ session.unreadCount > 99 ? '99+' : session.unreadCount }}
                </div>
              </div>

              <div class="info">
                <div class="info-top">
                  <span class="nickname">{{ session.friendNickname || '未知用户' }}</span>
                  <span class="time">{{ session.time }}</span>
                </div>
                <div class="last-msg">{{ session.lastMsg || '暂无消息' }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-window" :class="{ empty: !currentSessionId }">
          <div v-if="!currentSessionId" class="empty-state">
            <p class="empty-title">尚未选择任何联系人</p>
            <p class="empty-desc">快点左侧列表聊起来吧~</p>
          </div>

          <template v-else>
            <div class="session-header">
              <div class="session-info">
                <div class="session-title-group">
                  <button class="back-btn" @click="goBackToList">
                    <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
                  </button>
                  <span class="session-name">
                    {{ chatList.find(s => s.id === currentSessionId)?.friendNickname || '未知' }}
                  </span>
                </div>

                <div class="session-actions">
                  <button class="action-btn" @click="goToSellerDetail">进入主页</button>
                  <button class="action-btn icon-only" @click="()=>{ElMessage.success('暂未开发相应功能')}">···</button>
                </div>
              </div>

              <div v-if="currentSessionTradeInfo" class="trade-card">
                <img :src="currentSessionTradeInfo.previewImg" class="trade-img" alt="商品图片" />
                <div class="trade-info">
                  <div class="trade-title">{{ currentSessionTradeInfo.title }}</div>
                  <div class="trade-status">与当前用户的交易相关</div>
                </div>
              </div>
            </div>

            <div class="msg-list">
              <div
                  v-for="msg in msgList"
                  :key="msg.id"
                  :class="['msg-item', msg.senderId === userId ? 'send' : 'receive']"
              >
                <img :src="msg.senderAvatar || defaultAvater" class="msg-avatar" alt="头像" />
                <div class="msg-content-wrapper">
                  <div class="msg-bubble">
                    {{ msg.content }}
                  </div>
                  <div class="msg-meta">
                    <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                    <span v-if="msg.isRead === 0 && msg.senderId === userId" class="unread-tag">未读</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="msg-input-wrapper">
              <div class="toolbar">
                <el-popover
                    v-model:visible="showEmojiPanel"
                    placement="top-start"
                    width="320"
                    trigger="click"
                >
                  <div class="emoji-container">
                    <EmojiPicker
                        @select="insertEmoji"
                        :showSearch="false"
                        :showHistory="true"
                        :categories="['recent', 'smileys', 'people', 'animals', 'food']"
                    />
                  </div>
                  <template #reference>
                    <button class="toolbar-btn">😀</button>
                  </template>
                </el-popover>
              </div>

              <div class="input-area">
                <input
                    class="msg-input"
                    v-model="inputContent"
                    type="text"
                    placeholder="发条消息聊聊吧~"
                    @keyup.enter="sendMsg"
                />
                <button @click="sendMsg" class="send-btn" :disabled="!inputContent.trim()">发送</button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* ========== 全局容器 ========== */
.chat-page {
  display: flex;
  height: 100vh;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.chat-container {
  width: 85%;
  height: 85vh;
  max-width: 1200px;
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.chat-main {
  display: flex;
  height: 100%;
  width: 100%;
}

/* ========== 左侧聊天列表 ========== */
.chat-list {
  width: 320px;
  background-color: #fafafa;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.chat-list-header {
  padding: 20px 24px 12px;
  h2 {
    font-size: 20px;
    font-weight: 600;
    color: #1f2329;
    margin: 0;
  }
}

.chat-scroll-area {
  flex: 1;
  overflow-y: auto;
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 14px 24px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  gap: 14px;

  &:hover { background-color: #f0f2f5; }
  &.active { background-color: #e6f0ff; }
}

.avatar-wrapper {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;

  .avatar {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid #eee;
  }

  .unread-badge {
    position: absolute;
    top: -2px;
    right: -2px;
    min-width: 18px;
    height: 18px;
    line-height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    background-color: #f53f3f;
    color: #fff;
    font-size: 11px;
    font-weight: bold;
    text-align: center;
    border: 2px solid #fff;
  }
}

.info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;

  .info-top {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .nickname {
      font-size: 15px;
      font-weight: 500;
      color: #1f2329;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .time {
      font-size: 12px;
      color: #8f959e;
      flex-shrink: 0;
    }
  }

  .last-msg {
    font-size: 13px;
    color: #8f959e;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

/* ========== 右侧聊天窗口 ========== */
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  position: relative;
  min-width: 0; /* 确保子元素能够截断 */
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  .empty-icon { width: 120px; margin-bottom: 20px; opacity: 0.8; }
  .empty-title { font-size: 18px; font-weight: 500; color: #1f2329; margin-bottom: 8px; }
  .empty-desc { font-size: 14px; color: #8f959e; }
}

.session-header {
  padding: 16px 24px;
  border-bottom: 1px solid #ebeef5;
  background-color: #fff;
  z-index: 10;
}

.session-info {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .session-title-group {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .back-btn {
    display: none; /* 默认隐藏，仅在移动端显示 */
    background: transparent;
    border: none;
    padding: 4px;
    cursor: pointer;
    color: #1f2329;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .session-name {
    font-size: 18px;
    font-weight: 600;
    color: #1f2329;
  }

  .session-actions {
    display: flex;
    gap: 12px;

    .action-btn {
      padding: 6px 16px;
      border: 1px solid #dcdfe6;
      border-radius: 6px;
      background-color: #fff;
      color: #1f2329;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background-color: #f5f7fa;
        border-color: #c0c4cc;
      }
      &.icon-only { padding: 6px 12px; }
    }
  }
}

.trade-card {
  display: flex;
  align-items: center;
  margin-top: 16px;
  padding: 12px;
  background-color: #f7f8fa;
  border-radius: 8px;
  gap: 12px;

  .trade-img {
    width: 48px;
    height: 48px;
    border-radius: 6px;
    object-fit: cover;
  }

  .trade-title {
    font-size: 14px;
    font-weight: 500;
    color: #1f2329;
    margin-bottom: 4px;
  }

  .trade-status { font-size: 12px; color: #8f959e; }
}

.msg-list {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: 85%;
}

.msg-item.send {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.msg-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.msg-item.send .msg-content-wrapper {
  align-items: flex-end;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word; /* 避免长文本撑破屏幕 */
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}

.msg-item.receive .msg-bubble {
  background-color: #ffffff;
  color: #1f2329;
  border-top-left-radius: 4px;
}

.msg-item.send .msg-bubble {
  background-color: #165dff;
  color: #ffffff;
  border-top-right-radius: 4px;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;

  .msg-time { color: #8f959e; }
  .unread-tag { color: #165dff; font-weight: 500; }
}

.msg-input-wrapper {
  padding: 12px 24px 24px;
  background-color: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 12px;

  .toolbar-btn {
    background: transparent;
    border: none;
    font-size: 20px;
    cursor: pointer;
    padding: 4px;
    border-radius: 6px;
    transition: background 0.2s;

    &:hover { background-color: #f0f2f5; }
  }
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 16px;

  .msg-input {
    flex: 1;
    border: 1px solid transparent;
    background-color: #f5f7fa;
    border-radius: 8px;
    padding: 12px 16px;
    font-size: 15px;
    color: #1f2329;
    outline: none;
    transition: all 0.2s;
    min-width: 0; /* 防止移动端被撑开 */

    &:focus {
      background-color: #fff;
      border-color: #165dff;
      box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.1);
    }
    &::placeholder { color: #c0c4cc; }
  }

  .send-btn {
    padding: 10px 24px;
    background-color: #165dff;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    height: 44px;
    flex-shrink: 0;

    &:hover:not(:disabled) { background-color: #4080ff; }
    &:disabled {
      background-color: #94bfff;
      cursor: not-allowed;
    }
  }
}

.emoji-container {
  max-height: 300px;
  overflow: hidden;
}
:deep(.emoji-picker) {
  width: 100% !important;
  border: none;
  box-shadow: none;
}

/* ========== 移动端完美适配核心代码 ========== */
@media screen and (max-width: 768px) {
  .back-btn { display: flex !important; }

  .chat-page {
    background-color: #fff;
  }

  .chat-container {
    width: 100%;
    height: 100vh;
    border-radius: 0;
    box-shadow: none;
  }

  /* 默认显示列表，隐藏窗口 */
  .chat-list {
    width: 100%;
    border-right: none;
  }
  .chat-window {
    display: none;
  }

  /* 当选中会话时，隐藏列表，显示窗口 */
  .chat-main.in-session .chat-list {
    display: none;
  }
  .chat-main.in-session .chat-window {
    display: flex;
    width: 100%;
  }

  .session-header { padding: 12px 16px; }
  .msg-list { padding: 16px; }
  .msg-input-wrapper { padding: 12px 16px 16px; }

  .msg-item { max-width: 95%; }

  .input-area { gap: 10px; }
  .input-area .send-btn { padding: 10px 16px; }

  .session-info .session-name { font-size: 16px; }
  .session-info .action-btn { padding: 4px 12px; font-size: 12px; }
}
</style>
