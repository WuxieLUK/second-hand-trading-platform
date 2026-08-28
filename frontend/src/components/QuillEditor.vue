<template>
  <div class="editor-container">
    <!-- md-editor-v3 编辑器容器（根据模式控制显示） -->
    <div class="editor-preview-wrapper" :class="editorMode">
      <!-- 编辑区域（纯编辑/分栏模式显示） -->
      <div class="edit-area" v-show="editorMode !== 'preview'">
        <MdEditor
            ref="editorRef"
            v-model="editorContent"
            theme="light"
            previewTheme="github"
            codeTheme="github"
            :placeholder="props.placeholder"
            :disabled="props.loading || editorMode === 'preview'"
            :onUploadImg="handleUploadImg"
            :height="editAreaHeight"
        />
      </div>
    </div>

    <!-- 可选：Markdown 提示 -->
    <div v-if="props.showMarkdownTip" class="markdown-tip">
      <el-tag size="small" type="info">支持 Markdown 语法</el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, defineProps, defineEmits, computed, onMounted, onUnmounted } from 'vue'
// 导入 md-editor-v3 核心组件和样式
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
// 保留原有的代码高亮、剪贴板、消息提示
import ClipboardJS from 'clipboard'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '@/stores/token.js'
import { getUploadUrl } from '@/utils/serviceUrls.js'
import 'highlight.js/styles/github.css'

// 定义Props（与原Quill组件保持一致）
const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  height: {
    type: [String, Number],
    default: 500
  },
  loading: {
    type: Boolean,
    default: false
  },
  showMarkdownTip: {
    type: Boolean,
    default: true
  },
  placeholder: {
    type: String,
    default: '请输入内容（支持 Markdown 语法）...'
  }
})

// 定义Emits（与原组件保持一致）
const emit = defineEmits(['update:modelValue', 'content-change', 'save'])

// Token存储（复用图片上传的token逻辑）
const tokenStore = useTokenStore()
const uploadUrl = getUploadUrl()

// 编辑器核心数据
const editorContent = ref(props.modelValue)
const editorMode = ref('edit') // edit/preview/split
const editorRef = ref<InstanceType<typeof MdEditor> | null>(null)
let clipboardInstance: ClipboardJS | null = null

// 监听父组件传入值变化（双向绑定）
watch(
    () => props.modelValue,
    (newVal) => {
      editorContent.value = newVal || ''
    },
    {immediate: true}
)

// 监听编辑器内容变化，向父组件派发事件
watch(
    () => editorContent.value,
    (newVal) => {
      emit('update:modelValue', newVal)
      emit('content-change', newVal)
    }
)

// 配置marked+highlight.js（复用原预览高亮逻辑）
onMounted(() => {
  // 配置marked渲染（代码高亮）

  // 初始化剪贴板（代码复制功能）
  clipboardInstance = new ClipboardJS('.copy-btn', {
    text: (trigger) => {
      return trigger.previousElementSibling?.textContent || ''
    }
  })

  // 剪贴板回调
  clipboardInstance.on('success', () => {
    ElMessage.success('代码复制成功')
  })
  clipboardInstance.on('error', () => {
    ElMessage.error('代码复制失败')
  })
})

// 组件卸载时销毁剪贴板
onUnmounted(() => {
  if (clipboardInstance) {
    clipboardInstance.destroy()
  }
})

// 计算属性：编辑区域高度（适配分栏/全屏模式）
const editAreaHeight = computed(() => {
  // 减去工具栏和间距的高度，与原样式保持一致
  return `calc(100% - 40px)`
})
// 图片上传逻辑（复用原Quill的上传逻辑，适配md-editor-v3的API）
const handleUploadImg = async (files: File[], callback: (urls: string[]) => void) => {
  try {
    const urls: string[] = []
    for (const file of files) {
      const res = await uploadFile(file)
      urls.push(res)
    }
    callback(urls)
  } catch (error) {
    ElMessage.error('图片上传失败')
    callback([])
  }
}

// 图片上传核心方法（复用原XHR逻辑）
const uploadFile = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', uploadUrl, true)
    xhr.setRequestHeader('Authorization', tokenStore.token)

    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          const response = JSON.parse(xhr.responseText)
          resolve(response.data?.url || response.data)
        } catch (error) {
          reject(new Error('解析上传结果失败'))
        }
      } else {
        reject(new Error('上传请求失败'))
      }
    }

    xhr.onerror = () => {
      reject(new Error('网络错误'))
    }

    xhr.send(formData)
  })
}
</script>

<style lang="scss" scoped>
.editor-container {
  width: 100%;
  height: 70vh;
  padding: 10px;
  overflow: hidden;
  position: relative;
  box-sizing: border-box;

  // 模式切换工具栏样式（复用原样式）
  .mode-switch {
    margin-bottom: 8px;
    padding-bottom: 8px;
    border-bottom: 1px solid #e5e7eb;
  }

  // 编辑器与预览区域容器（复用原样式逻辑）
  .editor-preview-wrapper {
    width: 100%;
    height: calc(100% - 40px);
    overflow: hidden;

    // 纯编辑模式
    &.edit {
      .edit-area {
        width: 100%;
        height: 100%;
      }
    }

    // 纯预览模式
    &.preview {
      .preview-area {
        width: 100%;
        height: 100%;
      }
    }

    // 分栏模式
    &.split {
      display: flex;
      gap: 10px;

      .edit-area,
      .preview-area {
        flex: 1;
        height: 100%;
      }
    }
  }

  // 编辑区域样式（适配md-editor-v3）
  .edit-area {
    height: 100%;
    overflow: hidden;

    :deep(.md-editor) {
      height: 100% !important;
      border-radius: 4px;
      border: 1px solid #e5e7eb;
    }

    :deep(.md-editor-toolbar) {
      border-bottom: 1px solid #e5e7eb;
      border-radius: 4px 4px 0 0;
    }

    :deep(.md-editor-content) {
      height: calc(100% - 40px) !important;
    }

    // 禁用状态样式
    :deep(.md-editor-disabled) {
      background-color: rgba(245, 245, 245, 0.5);
      cursor: not-allowed;
    }
  }

  // 预览区域样式（完全复用原样式）
  .preview-area {
    height: 100%;
    padding: 15px;
    border: 1px solid #e5e7eb;
    border-radius: 4px;
    overflow-y: auto;
    background: #fff;
    box-sizing: border-box;

    .preview-content {
      width: 100%;
      color: #333;
      line-height: 1.6;

      // 空内容提示
      .empty-preview {
        color: #999;
        text-align: center;
        padding: 20px;
      }

      // Markdown 样式适配（复用原样式）
      h1, h2, h3, h4, h5, h6 {
        margin: 16px 0;
        font-weight: 600;
      }

      p {
        margin: 8px 0;
      }

      blockquote {
        margin: 16px 0;
        padding: 8px 16px;
        border-left: 4px solid #e5e7eb;
        background: #f9fafb;
        color: #666;
      }

      ul, ol {
        margin: 8px 0 8px 24px;
      }

      img {
        max-width: 100%;
        border-radius: 4px;
      }

      a {
        color: #165dff;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }

      // 代码块容器（包含复制按钮，复用原样式）
      .code-block-wrapper {
        position: relative;
        margin: 16px 0;

        .copy-btn {
          position: absolute;
          top: 8px;
          right: 8px;
          padding: 4px 8px;
          font-size: 12px;
          border: none;
          border-radius: 4px;
          background: #165dff;
          color: #fff;
          cursor: pointer;
          opacity: 0.7;
          transition: opacity 0.2s;

          &:hover {
            opacity: 1;
          }
        }

        pre {
          margin: 0;
          padding: 16px;
          border-radius: 4px;
          background: #f6f8fa;
          overflow-x: auto;

          code {
            font-size: 14px;
            line-height: 1.5;
          }
        }
      }
    }
  }

  // Markdown提示样式（复用原样式）
  .markdown-tip {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 10;
    background: #fff;
    padding: 0 4px;
  }
}
</style>
