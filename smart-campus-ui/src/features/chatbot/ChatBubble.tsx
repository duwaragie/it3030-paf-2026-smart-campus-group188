import { useState } from 'react';
import { useAuthStore } from '@/store/authStore';
import ChatPanel from './ChatPanel';

export default function ChatBubble() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const user = useAuthStore((s) => s.user);
  const [open, setOpen] = useState(false);

  if (!isAuthenticated || !user || user.profileComplete === false) {
    return null;
  }

  return (
    <>
      {open && <ChatPanel onClose={() => setOpen(false)} />}

      <div className="fixed bottom-6 right-6 z-50">
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          aria-label={open ? 'Close CampusBot' : 'Open CampusBot'}
          className="group relative block animate-float"
        >
          {/* Soft outer glow (always-on) */}
          <span
            aria-hidden
            className="absolute -inset-2 rounded-full bg-gradient-to-br from-sky-300/60 via-blue-400/60 to-indigo-500/60 blur-xl transition-opacity duration-300 group-hover:opacity-100 opacity-80"
          />
          {/* Pulsing ring */}
          <span
            aria-hidden
            className="absolute inset-0 rounded-full bg-gradient-to-br from-sky-400 to-indigo-500 opacity-40 animate-ping"
          />
          {/* Main gradient button */}
          <span
            className="relative flex h-14 w-14 items-center justify-center rounded-full text-white shadow-lg transition-transform duration-200 group-hover:scale-110 group-active:scale-95 bg-gradient-to-br from-sky-300 via-blue-500 to-indigo-600 bg-[length:200%_200%] animate-gradient-shift ring-1 ring-white/40"
          >
            {open ? (
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            ) : (
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2a7 7 0 0 0-7 7v2.5a4 4 0 0 0 1.17 2.83L7 14.17V16a3 3 0 0 0 3 3v1a2 2 0 0 0 4 0v-1a3 3 0 0 0 3-3v-1.83l1.83-1.84A4 4 0 0 0 20 11.5V9a7 7 0 0 0-8-7z" />
                <path d="M9 10h.01M15 10h.01" />
              </svg>
            )}
          </span>
        </button>
      </div>
    </>
  );
}
