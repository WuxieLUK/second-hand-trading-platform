<script setup lang="js">
import { commentAddService, commentList, commentLikeService } from "@/api/comment.js";
import { ElMessage } from "element-plus";
import { ref, watch } from "vue";
import { ChatLineSquare, Promotion, CaretBottom, CaretTop } from "@element-plus/icons-vue";
import useUserInfoStore from '@/stores/userInfo.js';
import userImg from '@/assets/用户.svg'
import SvgIcon from "@/components/SvgIcon.vue";

const userInfoStore = useUserInfoStore();
const comments = ref([]);
const replyContent = ref(''); // 局部回复框的内容

const props = defineProps({
  goodsId: { type: Number, required: true },
});

// 顶部主评论表单模型
const commentModel = ref({
  goodsId: props.goodsId,
  content: '',
  userUrl: userInfoStore.info.userPic,
  parentId: null,
});

// --- 逻辑处理 ---

const convertToTree = (flatList) => {
  const map = {};
  const tree = [];

  flatList.forEach(item => {
    // 注入前端特有状态：_isExpanded(是否展开子评论), _isReplyVisible(是否显示回复框)
    map[item.id] = {
      ...item,
      children: [],
      _isExpanded: false,
      _isReplyVisible: false
    };
  });

  flatList.forEach(item => {
    const node = map[item.id];
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(node);
    } else {
      tree.push(node);
    }
  });
  return tree;
};

const getCommentList = async () => {
  try {
    let result = await commentList(props.goodsId);
    comments.value = convertToTree(result.data || []);
  } catch (error) {
    ElMessage.error("获取评论失败");
  }
};

// 点赞功能
const handleLike = async (item) => {
  // 逻辑：向后端发送请求，成功后前端数值+1
  const result = await commentLikeService(item.id);
  if (result.data === "1") {
    item.likeCount = item.likeCount + 1;
  } else if (result.data === "0") {
    item.likeCount = item.likeCount - 1;
  } else {
    ElMessage.error("操作失败");
  }
};

const closeAllReplyBoxes = (comments) => {
  comments.forEach(item => {
    item._isReplyVisible = false;
    if (item.children && item.children.length > 0) {
      closeAllReplyBoxes(item.children);
    }
  });
};

// 点击回复：控制内嵌回复框的显示
const toggleReplyBox = (item) => {
  // 关闭其他所有正在打开的回复框（可选）
  closeAllReplyBoxes(comments.value);
  item._isReplyVisible = !item._isReplyVisible;
};

// 提交内嵌回复
const submitInlineReply = async (parentItem) => {
  if (!replyContent.value.trim()) return ElMessage.warning('请输入回复内容');

  const data = {
    goodsId: props.goodsId,
    content: replyContent.value,
    userUrl: userInfoStore.info.userPic,
    // 始终挂载在顶级评论下，保持二级结构
    parentId: parentItem.parentId || parentItem.id,
  };

  await commentAddService(data);
  ElMessage.success('回复已发布');
  parentItem._isReplyVisible = false;
  await getCommentList();
};

// 提交顶部主评论
const submitMainComment = async () => {
  if (!commentModel.value.content.trim()) return ElMessage.warning('请输入评论内容');
  await commentAddService(commentModel.value);
  ElMessage.success('评论已发布');
  commentModel.value.content = '';
  await getCommentList();
};

watch(() => props.goodsId, () => getCommentList(), { immediate: true });
</script>

<template>
  <div class="comment-wrapper">
    <div class="comment-header-title">
      <el-icon><ChatLineSquare /></el-icon>
      <span>全部评论 ({{ comments.length }})</span>
    </div>

    <div class="input-card">
      <textarea
          class="custom-textarea"
          v-model="commentModel.content"
          @keydown.enter.prevent="submitMainComment"
          placeholder="友善发言，共建和谐社区"
      />
      <div class="input-actions">
        <span class="input-tip">分享你的见解...</span>
        <el-button type="primary" :icon="Promotion" round @click="submitMainComment">
          发布评论
        </el-button>
      </div>
    </div>

    <div class="comment-list-container">
      <template v-if="comments.length > 0">
        <!-- 每条评论组之间添加分隔线 -->
        <div
            v-for="(comment, index) in comments"
            :key="comment.id"
            class="comment-item-group"
            :class="{ 'with-divider': index < comments.length - 1 }"
        >
          <div class="main-comment">
            <el-avatar :size="40" :src="comment.userUrl || userImg" />
            <div class="comment-body">
              <div class="user-info">
                <span class="username">{{ comment.username }}</span>
                <span class="comment-time">{{ comment.createTime }}</span>
              </div>
              <div class="comment-text">{{ comment.content }}</div>
              <div class="comment-footer">
                <span class="footer-action">
                  <SvgIcon
                      icon-class="icon-dianzan"
                      @click="handleLike(comment)"
                      style=" width: 2.2em;height: 2.2em; vertical-align: -0.65em; cursor: pointer;"
                  /> {{ comment.likeCount || 0 }}
                  <SvgIcon iconClass="icon-pinglun" @click="toggleReplyBox(comment)" style=" width: 1.5em;height: 1.5em; vertical-align: -0.3em;"/>
                  回复
                </span>
              </div>

              <div v-if="comment._isReplyVisible" class="inline-reply-box">
                <textarea v-model="replyContent" :placeholder="'回复 @' + comment.username"></textarea>
                <div style="display: flex;flex-direction: row;">
                  <el-button type="warning" size="small" @click="comment._isReplyVisible=!comment._isReplyVisible">取消</el-button>
                  <el-button type="primary" size="small" @click="submitInlineReply(comment)">发布</el-button>
                </div>
              </div>

              <div v-if="comment.children && comment.children.length > 0" class="sub-comments-list">
                <div v-for="sub in (comment._isExpanded ? comment.children : comment.children.slice(0, 1))"
                     :key="sub.id" class="sub-comment-item">
                  <el-avatar :size="24" :src="sub.userUrl || userImg" />
                  <div class="comment-body">
                    <div class="user-info">
                      <span class="username">{{ sub.username }}</span>
                      <span class="comment-time">{{ sub.createTime }}</span>
                    </div>
                    <div class="comment-text">
                      <span v-if="sub.parentId !== comment.id" class="at-user">@某人 </span>
                      {{ sub.content }}
                    </div>
                    <div class="comment-footer">
                      <span class="footer-action" >
                        <SvgIcon
                            icon-class="icon-dianzan"
                            @click="handleLike(sub)"
                            style=" width: 2.2em;height: 2.2em; vertical-align: -0.65em; cursor: pointer;"
                        />
                        {{ sub.likeCount || 0 }}
                        <SvgIcon iconClass="icon-pinglun" @click="toggleReplyBox(sub)" style=" width: 1.5em;height: 1.5em; vertical-align: -0.3em;"/>
                        回复</span>
                    </div>

                    <div v-if="sub._isReplyVisible" class="inline-reply-box">
                      <textarea v-model="replyContent" :placeholder="'回复 @' + sub.username"></textarea>
                      <div style="display: flex;flex-direction: row;">
                        <el-button type="warning" size="small" @click="sub._isReplyVisible=!sub._isReplyVisible">取消</el-button>
                        <el-button type="primary" size="small" @click="submitInlineReply(sub)">发布</el-button>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="comment.children.length > 1" class="expand-control" @click="comment._isExpanded = !comment._isExpanded">
                  <template v-if="!comment._isExpanded">
                    展开更多 {{ comment.children.length - 1 }} 条回复 <el-icon><CaretBottom /></el-icon>
                  </template>
                  <template v-else>
                    收起回复 <el-icon><CaretTop /></el-icon>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <el-empty v-else description="暂无评论" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.comment-wrapper {
  max-width: 1000px;
  margin: 40px auto;
}

/* 顶部输入框保留原有样式 */
.input-card {
  background: var(--bg);
  border: 1px solid #e1e2e3;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 30px;
  .custom-textarea {
    background-color: var(--textarea-color);
    color: var(--text);
    resize: none;
    padding: 0.5rem;
    width: 100%;
    height: 20em;
    border-radius: 0.5em;
    line-height: 1.5em;
    border: 1px solid var(--el-border-color);
    margin-bottom: 10px;
    background-image: var(--textarea-bg);
    background-repeat: no-repeat;
    background-position: right center;
    background-size: contain;
    background-attachment: scroll;
  }
  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #f0f0f0;
    padding-top: 10px;
    .input-tip { color: #999; font-size: 12px; }
  }
}

.comment-item-group {
  margin-bottom: 20px;
  padding-bottom: 20px;
  &.with-divider {
    border-bottom: 1px solid #e5e7eb;
  }
}

.main-comment {
  display: flex;
  gap: 15px;
  .comment-body {
    flex: 1;
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;
      .username {
        font-weight: 600;
        color: #18191c;
        font-size: 13px;
      }
      .comment-time {
        font-size: 13px;
        color: #9499a0;
      }
      .original-badge {
        background-color: #00a1d6;
        color: #fff;
        font-size: 12px;
        padding: 1px 6px;
        border-radius: 2px;
      }
      .self-badge {
        background-color: #f4f5f7;
        color: #6d757a;
        font-size: 12px;
        padding: 1px 6px;
        border-radius: 2px;
      }
    }
    .comment-text {
      margin: 8px 0;
      font-size: 15px;
      line-height: 1.5;
      color: #18191c;
    }
  }
}

.comment-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
  .footer-action {
    font-size: 13px;
    color: #9499a0;
    cursor: pointer;
    &:hover { color: #00aeec; }
  }
}

/* 内嵌回复框样式 */
.inline-reply-box {
  margin: 10px 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  background: #f4f5f7;
  padding: 10px;
  border-radius: 4px;
  textarea {
    width: 100%;
    height: 40px;
    border: 1px solid #e3e5e7;
    border-radius: 4px;
    padding: 5px;
    resize: none;
    background: #fff;
    &:focus { outline: none; border-color: #00aeec; background: #fff; }
  }
}

/* 子评论列表样式 - 仿B站 */
.sub-comments-list {
  margin-top: 15px;
  padding: 12px;
  background-color: #f4f5f7;
  border-radius: 4px;
  .sub-comment-item {
    display: flex;
    gap: 10px;
    margin-bottom: 12px;
    .comment-text { font-size: 14px; }
  }
}

.expand-control {
  font-size: 13px;
  color: #6d757a;
  cursor: pointer;
  margin-top: 5px;
  &:hover { color: #00aeec; }
}

.at-user { color: #00aeec; margin-right: 4px; cursor: pointer; }
</style>