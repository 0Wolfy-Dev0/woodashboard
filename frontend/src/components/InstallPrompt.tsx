import { useEffect, useState } from 'react'
import { Download, X } from 'lucide-react'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

const DISMISS_KEY = 'woodashboard.install-dismissed'

export function InstallPrompt() {
  const [promptEvent, setPromptEvent] = useState<BeforeInstallPromptEvent | null>(null)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    if (window.matchMedia('(display-mode: standalone)').matches) return
    try {
      if (localStorage.getItem(DISMISS_KEY) === '1') return
    } catch {
      // Storage can be unavailable; still allow the prompt.
    }

    const onBeforeInstall = (event: Event) => {
      event.preventDefault()
      setPromptEvent(event as BeforeInstallPromptEvent)
      setVisible(true)
    }
    const onInstalled = () => {
      setPromptEvent(null)
      setVisible(false)
    }

    window.addEventListener('beforeinstallprompt', onBeforeInstall)
    window.addEventListener('appinstalled', onInstalled)
    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall)
      window.removeEventListener('appinstalled', onInstalled)
    }
  }, [])

  async function install() {
    if (!promptEvent) return
    await promptEvent.prompt()
    const choice = await promptEvent.userChoice
    if (choice.outcome === 'accepted') {
      setPromptEvent(null)
      setVisible(false)
    }
  }

  function dismiss() {
    setVisible(false)
    try {
      localStorage.setItem(DISMISS_KEY, '1')
    } catch {
      // Ignore storage failures.
    }
  }

  if (!visible || !promptEvent) return null

  return (
    <div className="install-prompt" role="status">
      <span className="install-prompt-icon"><Download size={18} /></span>
      <span className="install-prompt-copy">
        <strong>Install Woodashboard</strong>
        <small>Add it to your home screen for faster access.</small>
      </span>
      <button className="button button-primary button-small" type="button" onClick={() => void install()}>Install</button>
      <button className="icon-button install-prompt-dismiss" type="button" onClick={dismiss} aria-label="Dismiss install prompt"><X size={16} /></button>
    </div>
  )
}
