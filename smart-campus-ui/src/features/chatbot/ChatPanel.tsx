import { useEffect, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { aiChatService, type ChatMessage } from '@/services/aiChatService';
import { Input } from '@/components/ui/input';
import { useAuthStore } from '@/store/authStore';

const markdownComponents = {
  p: (props: React.HTMLAttributes<HTMLParagraphElement>) => (
    <p className="mb-1.5 last:mb-0" {...props} />
  ),
  ul: (props: React.HTMLAttributes<HTMLUListElement>) => (
    <ul className="mb-1.5 list-disc space-y-0.5 pl-4 last:mb-0" {...props} />
  ),
  ol: (props: React.HTMLAttributes<HTMLOListElement>) => (
    <ol className="mb-1.5 list-decimal space-y-0.5 pl-4 last:mb-0" {...props} />
  ),
  li: (props: React.HTMLAttributes<HTMLLIElement>) => (
    <li className="leading-snug" {...props} />
  ),
  strong: (props: React.HTMLAttributes<HTMLElement>) => (
    <strong className="font-semibold text-foreground" {...props} />
  ),
  em: (props: React.HTMLAttributes<HTMLElement>) => <em className="italic" {...props} />,
  code: (props: React.HTMLAttributes<HTMLElement>) => (
    <code
      className="rounded bg-blue-50 px-1 py-0.5 font-mono text-[0.8em] text-blue-700"
      {...props}
    />
  ),
  h1: (props: React.HTMLAttributes<HTMLHeadingElement>) => (
    <h3 className="mb-1 mt-1.5 text-sm font-semibold first:mt-0" {...props} />
  ),
  h2: (props: React.HTMLAttributes<HTMLHeadingElement>) => (
    <h3 className="mb-1 mt-1.5 text-sm font-semibold first:mt-0" {...props} />
  ),
  h3: (props: React.HTMLAttributes<HTMLHeadingElement>) => (
    <h3 className="mb-1 mt-1.5 text-sm font-semibold first:mt-0" {...props} />
  ),
  a: (props: React.AnchorHTMLAttributes<HTMLAnchorElement>) => (
    <a className="text-blue-600 underline hover:text-blue-700" target="_blank" rel="noopener noreferrer" {...props} />
  ),
};

interface Props {
  onClose: () => void;
}

export default function ChatPanel({ onClose }: Props) {
  const user = useAuthStore((s) => s.user);
  const firstName = user?.name?.split(' ')[0] ?? 'there';
  const greeting =
    `Hi ${firstName}! I'm CampusBot. Ask me about your bookings, tickets, ` +
    `courses, or notifications. I read your live data so answers are always current.`;

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages, sending]);

  const send = async (override?: string) => {
    const trimmed = (override ?? input).trim();
    if (!trimmed || sending) return;
    setError(null);
    const next: ChatMessage[] = [...messages, { role: 'user', content: trimmed }];
    setMessages(next);
    setInput('');
    setSending(true);
    try {
      const resp = await aiChatService.chat(next);
      setMessages([...next, { role: 'assistant', content: resp.reply }]);
    } catch (e) {
      const msg = e instanceof Error ? e.message : 'Chat failed';
      setError(msg);
      setMessages(next);
    } finally {
      setSending(false);
    }
  };

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void send();
    }
  };

  const suggestions = [
    'How many open tickets do I have?',
    'Show my enrollments',
    "What's my GPA?",
    'Any unread notifications?',
  ];

  const isEmpty = messages.length === 0;

  return (
    <div className="fixed bottom-24 right-6 z-50 flex h-[34rem] w-[23rem] flex-col overflow-hidden rounded-3xl border border-white/60 bg-white/95 shadow-[0_20px_60px_-15px_rgba(30,58,95,0.35)] backdrop-blur-xl animate-panel-in">
      {/* Gradient header */}
      <div className="relative overflow-hidden bg-gradient-to-br from-sky-400 via-blue-500 to-indigo-600 bg-[length:200%_200%] animate-gradient-shift px-5 py-4 text-white">
        <div className="absolute -top-8 -right-8 h-24 w-24 rounded-full bg-white/20 blur-2xl" />
        <div className="absolute -bottom-6 -left-6 h-20 w-20 rounded-full bg-cyan-200/30 blur-2xl" />
        <div className="relative flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/20 backdrop-blur-sm ring-1 ring-white/40">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 2a7 7 0 0 0-7 7v2.5a4 4 0 0 0 1.17 2.83L7 14.17V16a3 3 0 0 0 3 3v1a2 2 0 0 0 4 0v-1a3 3 0 0 0 3-3v-1.83l1.83-1.84A4 4 0 0 0 20 11.5V9a7 7 0 0 0-8-7z" />
              </svg>
            </div>
            <div>
              <div className="text-sm font-semibold leading-tight">CampusBot</div>
              <div className="flex items-center gap-1.5 text-[11px] text-white/85">
                <span className="relative flex h-1.5 w-1.5">
                  <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-300 opacity-75" />
                  <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-400" />
                </span>
                Live data • Online
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            aria-label="Close chat"
            className="flex h-7 w-7 items-center justify-center rounded-full text-white/85 transition-colors hover:bg-white/20 hover:text-white"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      </div>

      {/* Messages */}
      <div
        ref={scrollRef}
        className="relative flex-1 space-y-3 overflow-y-auto bg-gradient-to-b from-sky-50/40 to-white px-4 py-4"
      >
        {isEmpty && !sending && (
          <div className="flex flex-col gap-3">
            <div className="max-w-[90%] rounded-2xl rounded-bl-md bg-white px-3.5 py-2.5 text-sm text-foreground ring-1 ring-blue-100 shadow-sm">
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                {greeting}
              </ReactMarkdown>
            </div>
            <div className="px-0.5 pt-1 text-[11px] font-medium uppercase tracking-wide text-muted-foreground">
              Try one of these
            </div>
            <div className="flex flex-col gap-1.5">
              {suggestions.map((s) => (
                <button
                  key={s}
                  onClick={() => void send(s)}
                  className="rounded-xl border border-blue-100 bg-white px-3 py-2 text-left text-xs text-foreground shadow-sm transition-all hover:-translate-y-0.5 hover:border-blue-300 hover:shadow-md"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) =>
          m.role === 'user' ? (
            <div
              key={i}
              className="ml-auto max-w-[85%] rounded-2xl rounded-br-md bg-gradient-to-br from-sky-500 to-blue-600 px-3.5 py-2 text-sm text-white shadow-sm animate-fade-in"
            >
              {m.content}
            </div>
          ) : (
            <div
              key={i}
              className="max-w-[85%] rounded-2xl rounded-bl-md bg-white px-3.5 py-2 text-sm text-foreground ring-1 ring-blue-100 shadow-sm animate-fade-in"
            >
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                {m.content}
              </ReactMarkdown>
            </div>
          )
        )}

        {sending && (
          <div className="max-w-[85%] rounded-2xl rounded-bl-md bg-white px-3.5 py-2.5 text-sm ring-1 ring-blue-100 shadow-sm">
            <span className="inline-flex gap-1">
              <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-blue-400 [animation-delay:-0.3s]" />
              <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-blue-500 [animation-delay:-0.15s]" />
              <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-blue-600" />
            </span>
          </div>
        )}

        {error && (
          <div className="rounded-xl bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</div>
        )}
      </div>

      {/* Input */}
      <div className="border-t border-blue-100 bg-white/80 p-3 backdrop-blur">
        <div className="flex items-center gap-2 rounded-full border border-blue-100 bg-white px-2 py-1 shadow-sm focus-within:border-blue-300 focus-within:ring-2 focus-within:ring-blue-200/70">
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKey}
            placeholder="Ask about tickets, courses, bookings…"
            disabled={sending}
            className="flex-1 border-0 bg-transparent shadow-none focus-visible:ring-0"
          />
          <button
            type="button"
            onClick={() => void send()}
            disabled={sending || !input.trim()}
            aria-label="Send message"
            className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-sky-400 to-blue-600 text-white shadow-sm transition-all hover:brightness-110 hover:scale-105 active:scale-95 disabled:cursor-not-allowed disabled:opacity-40"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" />
              <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
}
