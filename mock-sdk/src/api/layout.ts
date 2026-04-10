import * as baseAPI from './base'
import { nexusBridge } from '../core/NexusBridge'

export interface Rect {
  left: number
  top: number
  right: number
  bottom: number
  width: number
  height: number
}

export interface SafeArea {
  left: number
  right: number
  top: number
  bottom: number
}

export interface GameViewport {
  x: number
  y: number
  width: number
  height: number
}

const DEFAULT_STATUS_BAR_DP = 24
const DEFAULT_CAPSULE_MARGIN_TOP_DP = 8
const DEFAULT_CAPSULE_HEIGHT_DP = 32
const DEFAULT_CAPSULE_WIDTH_DP = 88
const DEFAULT_CAPSULE_MARGIN_END_DP = 12

function toPx(dp: number, pixelRatio: number): number {
  return dp * pixelRatio
}

export const getMenuButtonBoundingClientRect = (): Rect => {
  if (nexusBridge.isNativeEnvironment()) {
    try {
      const rect = nexusBridge.invokeNativeSync('wx.getMenuButtonBoundingClientRect', {}) as any
      if (rect && typeof rect.top === 'number' && typeof rect.height === 'number') {
        return {
          left: Number(rect.left) || 0,
          top: Number(rect.top) || 0,
          right: Number(rect.right) || 0,
          bottom: Number(rect.bottom) || ((Number(rect.top) || 0) + (Number(rect.height) || 0)),
          width: Number(rect.width) || 0,
          height: Number(rect.height) || 0
        }
      }
    } catch {
      // Fallback to synthesized values.
    }
  }

  const systemInfo = baseAPI.getSystemInfoSync() as any
  const pixelRatio = Number(systemInfo.pixelRatio) || 1
  const statusBarHeight = Number(systemInfo.statusBarHeight) || toPx(DEFAULT_STATUS_BAR_DP, pixelRatio)
  const top = statusBarHeight + toPx(DEFAULT_CAPSULE_MARGIN_TOP_DP, pixelRatio)
  const height = toPx(DEFAULT_CAPSULE_HEIGHT_DP, pixelRatio)
  const width = toPx(DEFAULT_CAPSULE_WIDTH_DP, pixelRatio)
  const marginEnd = toPx(DEFAULT_CAPSULE_MARGIN_END_DP, pixelRatio)
  const windowWidth = Number(systemInfo.windowWidth) || (typeof window !== 'undefined' ? window.innerWidth : 0)
  const left = Math.max(0, windowWidth - marginEnd - width)

  return {
    left,
    top,
    width,
    height,
    right: left + width,
    bottom: top + height
  }
}

export const getSafeArea = (): SafeArea => {
  const systemInfo = baseAPI.getSystemInfoSync() as any
  const fromSystem = systemInfo?.safeArea
  if (fromSystem && typeof fromSystem.top === 'number' && typeof fromSystem.bottom === 'number') {
    return {
      left: Number(fromSystem.left) || 0,
      top: Number(fromSystem.top) || 0,
      right: Number(fromSystem.right) || Number(systemInfo.windowWidth) || 0,
      bottom: Number(fromSystem.bottom) || Number(systemInfo.windowHeight) || 0
    }
  }

  const capsule = getMenuButtonBoundingClientRect()
  const windowWidth = Number(systemInfo.windowWidth) || (typeof window !== 'undefined' ? window.innerWidth : 0)
  const windowHeight = Number(systemInfo.windowHeight) || (typeof window !== 'undefined' ? window.innerHeight : 0)

  return {
    left: 0,
    top: Math.max(Number(systemInfo.statusBarHeight) || 0, capsule.bottom),
    right: windowWidth,
    bottom: windowHeight
  }
}

export const getGameViewport = (): GameViewport => {
  const safeArea = getSafeArea()
  const width = Math.max(0, safeArea.right - safeArea.left)
  const height = Math.max(0, safeArea.bottom - safeArea.top)
  return {
    x: safeArea.left,
    y: safeArea.top,
    width,
    height
  }
}

export const applyCanvasSafeArea = (canvas: HTMLCanvasElement): GameViewport => {
  const viewport = getGameViewport()
  if (!canvas || !canvas.style) {
    return viewport
  }

  canvas.style.position = 'absolute'
  canvas.style.left = `${viewport.x}px`
  canvas.style.top = `${viewport.y}px`
  canvas.style.width = `${viewport.width}px`
  canvas.style.height = `${viewport.height}px`

  return viewport
}
