import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import './App.css';

const API_BASE = process.env.REACT_APP_API_BASE || '/api/chat';

function App() {
  const [sessionId] = useState(() => uuidv4());
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: 'Hello! I\'m your Market Analysis assistant. Ask me about stocks, sentiment, or market trends.',
      timestamp: new Date().toISOString(),
    },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!input.trim() || loading) return;

    const userMsg = { role: 'user', content: input, timestamp: new Date().toISOString() };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const response = await axios.post(`${API_BASE}/message`, {
        sessionId,
        message: input,
      });
      const assistantMsg = {
        role: 'assistant',
        content: response.data.content,
        timestamp: response.data.timestamp,
      };
      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Sorry, I encountered an error. Please try again.',
          timestamp: new Date().toISOString(),
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const QUICK_PROMPTS = [
    'What is the current AAPL sentiment?',
    'Summarise today\'s market trends',
    'How is TSLA performing?',
    'Bullish or bearish for tech?',
  ];

  return (
    <div className="app">
      <header className="app-header">
        <div className="logo">📈 Market Analysis</div>
        <div className="session-id">Session: {sessionId.slice(0, 8)}…</div>
      </header>

      <div className="chat-container">
        <div className="messages">
          {messages.map((msg, i) => (
            <div key={i} className={`message ${msg.role}`}>
              <div className="bubble">{msg.content}</div>
              <div className="meta">{msg.role === 'user' ? 'You' : 'Assistant'}</div>
            </div>
          ))}
          {loading && (
            <div className="message assistant">
              <div className="bubble typing">
                <span /><span /><span />
              </div>
            </div>
          )}
          <div ref={bottomRef} />
        </div>

        <div className="quick-prompts">
          {QUICK_PROMPTS.map((p) => (
            <button key={p} className="quick-btn" onClick={() => setInput(p)}>
              {p}
            </button>
          ))}
        </div>

        <form className="input-form" onSubmit={sendMessage}>
          <input
            className="chat-input"
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about stocks, sentiment, trends…"
            disabled={loading}
          />
          <button className="send-btn" type="submit" disabled={loading || !input.trim()}>
            Send
          </button>
        </form>
      </div>
    </div>
  );
}

export default App;
