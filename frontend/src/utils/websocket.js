import { useTokenStore } from '@/stores/token.js'
import { getGatewayWsBaseUrl } from '@/api/request' // 引入你现有的获取ws地址方法

class WebSocketUtil {
    constructor() {
        this.socket = null
        this.isConnected = false
        this.reconnectTimer = null
        this.heartBeatTimer = null
        this.messageListeners = new Set()
        this.manualClose = false
        this.currentUrl = ''
    }

    // ✅ 只改这里
    buildUrl() {
        const tokenStore = useTokenStore()
        const token = tokenStore.token
        const userId = tokenStore.userId

        const baseWsUrl = getGatewayWsBaseUrl()
        const url = new URL(baseWsUrl + '/ws/chat')

        if (userId) url.searchParams.set('userId', userId)
        if (token) url.searchParams.set('token', token)

        return url.toString()
    }

    connect(force = false) {
        const nextUrl = this.buildUrl()
        const readyState = this.socket?.readyState
        const isActive = readyState === WebSocket.OPEN || readyState === WebSocket.CONNECTING

        if (!force && isActive && this.currentUrl === nextUrl) {
            return
        }

        this.manualClose = false
        if (this.socket) {
            this.closeCurrentSocket()
        }

        this.currentUrl = nextUrl
        this.socket = new WebSocket(nextUrl)

        this.socket.onopen = () => {
            this.isConnected = true
            this.startHeartBeat()
            console.log('✅ WebSocket 连接成功')
        }

        this.socket.onmessage = (event) => {
            const payload = this.parseMessage(event.data)
            if (payload?.type === 'HEARTBEAT' || payload?.msgType === 0) {
                return
            }
            this.messageListeners.forEach((listener) => {
                listener(payload, event.data)
            })
        }

        this.socket.onclose = () => {
            this.isConnected = false
            this.stopHeartBeat()
            this.socket = null
            console.log('❌ 连接断开，尝试重连...')
            if (!this.manualClose) {
                this.reconnect()
            }
        }

        this.socket.onerror = () => {
            console.log('⚠️ WebSocket 错误')
        }
    }

    closeCurrentSocket() {
        this.stopHeartBeat()
        if (this.socket) {
            this.socket.close()
            this.socket = null
        }
        this.isConnected = false
    }

    disconnect() {
        this.manualClose = true
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
        this.closeCurrentSocket()
    }

    sendMessage(message) {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return
        const payload = typeof message === 'string' ? message : JSON.stringify(message)
        this.socket.send(payload)
    }

    onMessage(callback) {
        this.messageListeners.add(callback)
        return () => {
            this.messageListeners.delete(callback)
        }
    }

    startHeartBeat() {
        this.stopHeartBeat()
        this.heartBeatTimer = setInterval(() => {
            this.sendMessage({ type: 'PING' })
        }, 25000)
    }

    stopHeartBeat() {
        clearInterval(this.heartBeatTimer)
        this.heartBeatTimer = null
    }

    reconnect() {
        if (this.reconnectTimer) return
        this.reconnectTimer = setTimeout(() => {
            this.reconnectTimer = null
            this.connect(true)
        }, 3000)
    }

    parseMessage(raw) {
        try {
            return JSON.parse(raw)
        } catch (error) {
            return raw
        }
    }
}

export default new WebSocketUtil()