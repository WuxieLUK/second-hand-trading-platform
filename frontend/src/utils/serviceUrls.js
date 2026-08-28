const trimTrailingSlash = (value = '') => value.replace(/\/+$/, '')

const env = import.meta.env

const apiPrefix = env.VITE_API_PREFIX || env.VITE_API_BASENET_URL || '/api/backAll'
const gatewayBaseUrl = trimTrailingSlash(env.VITE_API_BASE_URL || '')

const joinUrl = (base, path) => {
  if (!base) {
    return path
  }
  return `${trimTrailingSlash(base)}${path.startsWith('/') ? path : `/${path}`}`
}

export const getApiBasePath = () => {
  if (apiPrefix.startsWith('http://') || apiPrefix.startsWith('https://')) {
    return trimTrailingSlash(apiPrefix)
  }
  return apiPrefix.startsWith('/') ? trimTrailingSlash(apiPrefix) : `/${trimTrailingSlash(apiPrefix)}`
}

export const getGatewayBaseUrl = () => gatewayBaseUrl

export const getUploadUrl = () => {
  const customUploadUrl = env.VITE_UPLOAD_URL
  if (customUploadUrl) {
    return customUploadUrl
  }
  return joinUrl(gatewayBaseUrl, `${getApiBasePath()}/upload`)
}

export const getSwaggerUrl = () => {
  const customSwaggerUrl = env.VITE_SWAGGER_UI_URL
  if (customSwaggerUrl) {
    return customSwaggerUrl
  }
  return joinUrl(gatewayBaseUrl, `${getApiBasePath()}/swagger-ui/index.html`)
}

export const getAiChatUrl = () => {
  const customAiChatUrl = env.VITE_AI_CHAT_URL
  if (customAiChatUrl) {
    return customAiChatUrl
  }
  // 默认拼接 Gateway 的 /api/ai/chat 路径（Gateway 路由规则：/api/ai/** → service-product）
  return joinUrl(gatewayBaseUrl, '/api/ai/chat')
}

export const getGatewayWsBaseUrl = () => {
  if (gatewayBaseUrl) {
    return gatewayBaseUrl.replace(/^http:\/\//, 'ws://').replace(/^https:\/\//, 'wss://')
  }

  if (typeof window !== 'undefined') {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}`
  }

  return 'ws://localhost:9005'
}
