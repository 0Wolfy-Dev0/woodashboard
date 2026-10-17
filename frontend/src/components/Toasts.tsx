import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react'
import type { ToastNotice } from '../types'

interface ToastsProps {
  notices: ToastNotice[]
  onDismiss: (id: number) => void
}

export function Toasts({ notices, onDismiss }: ToastsProps) {
  return (
    <div className="toast-stack" aria-live="polite" aria-relevant="additions">
      {notices.map((notice) => (
        <div className={`toast toast-${notice.kind}`} key={notice.id} role={notice.kind === 'error' ? 'alert' : 'status'}>
          <span className="toast-icon">
            {notice.kind === 'success' ? <CheckCircle2 size={18} /> : notice.kind === 'error' ? <AlertCircle size={18} /> : <Info size={18} />}
          </span>
          <span>{notice.message}</span>
          <button className="toast-dismiss" onClick={() => onDismiss(notice.id)} aria-label="Dismiss notification"><X size={15} /></button>
        </div>
      ))}
    </div>
  )
}
