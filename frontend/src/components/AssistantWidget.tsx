import { useEffect, useRef, useState } from 'react';
import { api } from '../api';

interface Msg { role: 'user' | 'assistant'; text: string }

export function AssistantWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<Msg[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [speak, setSpeak] = useState(true);
  const [listening, setListening] = useState(false);
  const convoId = useRef<string | undefined>(undefined);
  const recognitionRef = useRef<any>(null);
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [messages, open]);

  const speakText = (text: string) => {
    if (!speak || !('speechSynthesis' in window)) return;
    const u = new SpeechSynthesisUtterance(text);
    window.speechSynthesis.speak(u);
  };

  const send = async (text: string) => {
    const trimmed = text.trim();
    if (!trimmed || sending) return;
    const t = convoId.current;
    setInput('');
    setSending(true);
    setError('');
    setMessages((m) => [...m, { role: 'user', text: trimmed }]);
    try {
      const res = await api.assistant.chat(trimmed, t);
      convoId.current = res.conversationId;
      setMessages((m) => [...m, { role: 'assistant', text: res.reply }]);
      speakText(res.reply);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setSending(false);
    }
  };

  const startListening = () => {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SR) { setError('Speech recognition not supported in this browser.'); return; }
    const rec = new SR();
    rec.lang = 'en-US';
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    rec.onresult = (e: any) => {
      const transcript = e.results[0][0].transcript;
      setListening(false);
      send(transcript);
    };
    rec.onerror = () => setListening(false);
    rec.onend = () => setListening(false);
    recognitionRef.current = rec;
    setListening(true);
    rec.start();
  };

  const stopListening = () => {
    recognitionRef.current?.stop?.();
    setListening(false);
  };

  return (
    <>
      {/* Toggle button */}
      <button
        onClick={() => setOpen((o) => !o)}
        className="fixed bottom-5 right-5 z-50 w-14 h-14 rounded-full bg-primary text-white shadow-card-hover flex items-center justify-center hover:brightness-110 transition-all duration-150"
        title="Vaultx Assistant"
      >
        <span className="material-symbols-outlined" style={{ fontSize: '26px' }}>
          {open ? 'close' : 'smart_toy'}
        </span>
      </button>

      {/* Chat panel */}
      {open && (
        <div className="fixed bottom-24 right-5 z-50 w-[360px] max-w-[calc(100vw-2rem)] h-[520px] max-h-[70vh] bg-white rounded-2xl shadow-card-hover border border-border flex flex-col overflow-hidden overlay-fade">
          <div className="px-4 py-3 border-b border-border flex items-center justify-between bg-gray-50/60">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary" style={{ fontSize: '20px' }}>smart_toy</span>
              <span className="text-sm font-bold text-text-primary">Vaultx Assistant</span>
            </div>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setSpeak((s) => !s)}
                className={`p-1.5 rounded-lg ${speak ? 'text-primary' : 'text-text-muted'} hover:bg-gray-100`}
                title="Toggle voice replies"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>volume_up</span>
              </button>
              <button
                onClick={() => { convoId.current = undefined; setMessages([]); api.assistant.resetConversation(convoId.current ?? '').catch(() => {}); }}
                className="p-1.5 rounded-lg text-text-muted hover:bg-gray-100"
                title="New conversation"
              >
                <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>delete_sweep</span>
              </button>
            </div>
          </div>

          <div ref={listRef} className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
            {messages.length === 0 && (
              <div className="text-center text-xs text-text-muted py-8">
                Ask me things like <span className="text-primary font-medium">"Find me auctions under $100"</span> or{" "}
                <span className="text-primary font-medium">"Tell me about this auction."</span>
              </div>
            )}
            {messages.map((m, i) => (
              <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[85%] px-3 py-2 text-sm rounded-xl ${
                  m.role === 'user' ? 'bg-primary text-white' : 'bg-gray-100 text-text-primary'
                }`}>{m.text}</div>
              </div>
            ))}
            {sending && (
              <div className="flex justify-start">
                <div className="bg-gray-100 text-text-muted text-sm px-3 py-2 rounded-xl">Thinking…</div>
              </div>
            )}
          </div>

          {error && <div className="px-4 pb-1 text-xs text-danger">{error}</div>}

          <div className="p-3 border-t border-border flex items-center gap-2">
            <button
              onClick={listening ? stopListening : startListening}
              className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 transition-colors ${
                listening ? 'bg-danger text-white animate-pulse' : 'bg-gray-100 text-text-secondary hover:bg-gray-200'
              }`}
              title={listening ? 'Stop' : 'Speak'}
            >
              <span className="material-symbols-outlined" style={{ fontSize: '20px' }}>mic</span>
            </button>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') send(input); }}
              placeholder="Ask about auctions…"
              className="input-field flex-1 py-2 text-sm"
            />
            <button
              onClick={() => send(input)}
              disabled={sending || !input.trim()}
              className="btn-primary w-9 h-9 !px-0 flex items-center justify-center shrink-0"
              title="Send"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '18px' }}>send</span>
            </button>
          </div>
        </div>
      )}
    </>
  );
}
