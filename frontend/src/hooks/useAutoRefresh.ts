import { useEffect, useRef } from 'react'

const DEFAULT_INTERVAL_MS = 60_000

type RefreshCallback = () => void | Promise<void>

/**
 * Refresh data periodically while the tab is visible.
 * The callback should handle its own errors so background refreshes stay quiet.
 */
export function useAutoRefresh(callback: RefreshCallback, intervalMs = DEFAULT_INTERVAL_MS): void {
  const callbackRef = useRef(callback)
  const refreshingRef = useRef(false)

  useEffect(() => {
    callbackRef.current = callback
  }, [callback])

  useEffect(() => {
    const refreshIfVisible = async () => {
      if (document.visibilityState !== 'visible' || refreshingRef.current) return

      refreshingRef.current = true
      try {
        await callbackRef.current()
      } catch {
        // The page reports errors for foreground loads; background polls stay silent.
      } finally {
        refreshingRef.current = false
      }
    }

    const intervalId = window.setInterval(() => void refreshIfVisible(), intervalMs)
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') void refreshIfVisible()
    }

    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      window.clearInterval(intervalId)
      document.removeEventListener('visibilitychange', onVisibilityChange)
    }
  }, [intervalMs])
}
