import { useState, useEffect } from "react";

const STORIES = [
  {
    id: 1,
    title: "The Last Train to Varanasi",
    author: "Priya Sharma",
    avatar: "PS",
    lang: "English",
    genre: "Drama",
    cover: "#1a1a2e",
    accent: "#e94560",
    reads: "12.4k",
    likes: 843,
    excerpt: "The platform was empty except for an old man with a brass lamp. Meera checked her ticket again — Platform 9, 11:58 PM. The train that shouldn't exist.",
    chapters: 12,
    liked: false,
  },
  {
    id: 2,
    title: "मेरी माँ की रसोई",
    author: "Rahul Verma",
    avatar: "RV",
    lang: "Hindi",
    genre: "Family",
    cover: "#2d1b00",
    accent: "#f5a623",
    reads: "8.1k",
    likes: 621,
    excerpt: "उस रसोई की खुशबू आज भी मुझे याद है — हल्दी, घी, और कुछ ऐसा जो शब्दों में नहीं कह सकता। माँ का प्यार शायद ऐसे ही होता है।",
    chapters: 7,
    liked: false,
  },
  {
    id: 3,
    title: "Echoes of the Western Ghats",
    author: "Ananya Nair",
    avatar: "AN",
    lang: "English",
    genre: "Adventure",
    cover: "#0d2818",
    accent: "#2ecc71",
    reads: "19.2k",
    likes: 1204,
    excerpt: "The forest had a memory. Every tree held whispers of the tribe that once called this mist-draped mountain home. She was the last one who could hear them.",
    chapters: 24,
    liked: false,
  },
  {
    id: 4,
    title: "Neon Nights in Mumbai",
    author: "Arjun Mehta",
    avatar: "AM",
    lang: "English",
    genre: "Thriller",
    cover: "#0a0a1a",
    accent: "#9b59b6",
    reads: "6.7k",
    likes: 389,
    excerpt: "Three men entered the dhaba at midnight. By 1 AM, only one would walk out. Dev had seen too much — and now someone knew it.",
    chapters: 18,
    liked: false,
  },
];

const F = {
  display: "'Playfair Display', Georgia, serif",
  body: "'Lora', Georgia, serif",
  ui: "'DM Sans', system-ui, sans-serif",
};

export default function App() {
  const [screen, setScreen] = useState("splash");
  const [tab, setTab] = useState("feed");
  const [stories, setStories] = useState(STORIES);
  const [readingId, setReadingId] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [draft, setDraft] = useState({ title: "", content: "", genre: "Drama", lang: "English" });
  const [toast, setToast] = useState(null);
  const [splashReady, setSplashReady] = useState(false);
  const [filterLang, setFilterLang] = useState("For you");

  useEffect(() => {
    const link = document.createElement("link");
    link.rel = "stylesheet";
    link.href = "https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400;0,700;1,400&family=Lora:ital@0;1&family=DM+Sans:wght@300;400;500&display=swap";
    document.head.appendChild(link);

    document.body.style.background = "#0c0c0f";
    document.body.style.margin = "0";

    const timer = setTimeout(() => setSplashReady(true), 2000);
    return () => clearTimeout(timer);
  }, []);

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  };

  const toggleLike = (id, e) => {
    if (e) e.stopPropagation();
    setStories(s => s.map(x => x.id === id ? { ...x, liked: !x.liked, likes: x.liked ? x.likes - 1 : x.likes + 1 } : x));
  };

  const publishStory = () => {
    if (!draft.title.trim() || !draft.content.trim()) { showToast("Add a title and content first"); return; }
    const ns = {
      id: Date.now(), title: draft.title, author: "You", avatar: "ME",
      lang: draft.lang, genre: draft.genre, cover: "#1a0a2e", accent: "#e94560",
      reads: "0", likes: 0, excerpt: draft.content.slice(0, 120) + "...", chapters: 1, liked: false,
    };
    setStories(s => [ns, ...s]);
    setEditorOpen(false);
    setDraft({ title: "", content: "", genre: "Drama", lang: "English" });
    setTab("feed");
    showToast("Story published!");
  };

  const wordCount = draft.content.trim() ? draft.content.trim().split(/\s+/).length : 0;
  const readingStory = stories.find(s => s.id === readingId);

  const css = `
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-font-smoothing: antialiased; }
    body { background: #0c0c0f; overflow-x: hidden; }
    ::-webkit-scrollbar { width: 0; }
    @keyframes fadeUp { from { opacity:0; transform:translateY(18px); } to { opacity:1; transform:translateY(0); } }
    @keyframes fadeIn { from { opacity:0; } to { opacity:1; } }
    @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:0.3} }
    .fu { animation: fadeUp 0.45s ease both; }
    .fu1 { animation: fadeUp 0.45s 0.1s ease both; }
    .fu2 { animation: fadeUp 0.45s 0.22s ease both; }
    .fu3 { animation: fadeUp 0.45s 0.36s ease both; }
    .fu4 { animation: fadeUp 0.45s 0.5s ease both; }
    .fi { animation: fadeIn 0.3s ease; }
    input::placeholder, textarea::placeholder { color: #444; }
    input, textarea { caret-color: #e94560; }
  `;

  // SPLASH
  if (screen === "splash") return (
    <>
      <style>{css}</style>
      <div style={{ background: "#0c0c0f", minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", fontFamily: F.ui, position: "relative" }}>
        <div style={{ textAlign: "center", animation: "fadeIn 0.7s ease" }}>
          <div style={{ width: 76, height: 76, borderRadius: 22, background: "linear-gradient(135deg,#e94560,#9b59b6)", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 22px", fontSize: 34 }}>📖</div>
          <div style={{ fontFamily: F.display, fontSize: 40, color: "#fff", letterSpacing: "-0.5px", marginBottom: 8 }}>Kathakar</div>
          <div style={{ fontSize: 12, color: "#555", letterSpacing: "3px", textTransform: "uppercase" }}>Stories in every language</div>
        </div>
        <div style={{ marginTop: 60, display: "flex", gap: 6 }}>
          {[0, 1, 2].map(i => (
            <div key={i} style={{ width: 6, height: 6, borderRadius: "50%", background: i === 0 ? "#e94560" : "#2a2a2a", animation: `pulse 1.5s ${i * 0.3}s infinite` }} />
          ))}
        </div>
        {splashReady && (
          <div style={{ position: "absolute", bottom: 52, width: "100%", display: "flex", justifyContent: "center", animation: "fadeUp 0.4s ease" }}>
            <button onClick={() => setScreen("auth")} style={{ background: "#e94560", border: "none", borderRadius: 14, padding: "14px 48px", color: "#fff", fontFamily: F.ui, fontSize: 15, fontWeight: 500, cursor: "pointer" }}>
              Get started
            </button>
          </div>
        )}
      </div>
    </>
  );

  // AUTH
  if (screen === "auth") return (
    <>
      <style>{css}</style>
      <div style={{ background: "#0c0c0f", minHeight: "100vh", fontFamily: F.ui, display: "flex", flexDirection: "column" }}>
        <div style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "0 28px" }}>
          <div className="fu" style={{ textAlign: "center", marginBottom: 48 }}>
            <div style={{ fontFamily: F.display, fontSize: 42, color: "#fff", lineHeight: 1.15, marginBottom: 14 }}>
              Stories that<br /><em style={{ color: "#e94560" }}>move you</em>
            </div>
            <div style={{ color: "#555", fontSize: 14, lineHeight: 1.7 }}>Read, write and share stories<br />in your language</div>
          </div>
          <div className="fu2" style={{ width: "100%", maxWidth: 340 }}>
            <button onClick={() => { setScreen("home"); showToast("Welcome to Kathakar!"); }}
              style={{ width: "100%", background: "#fff", border: "none", borderRadius: 14, padding: "15px 20px", display: "flex", alignItems: "center", justifyContent: "center", gap: 12, cursor: "pointer", fontFamily: F.ui, fontSize: 15, fontWeight: 500, color: "#1a1a1a", marginBottom: 12 }}>
              <svg width="20" height="20" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" />
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
              </svg>
              Continue with Google
            </button>
            <div style={{ textAlign: "center", color: "#333", fontSize: 12, margin: "14px 0" }}>or</div>
            <button onClick={() => { setScreen("home"); showToast("Welcome to Kathakar!"); }}
              style={{ width: "100%", background: "transparent", border: "0.5px solid #2a2a2a", borderRadius: 14, padding: "15px 20px", color: "#666", fontFamily: F.ui, fontSize: 15, cursor: "pointer" }}>
              Continue with email
            </button>
          </div>
          <div className="fu3" style={{ marginTop: 28, color: "#333", fontSize: 11, textAlign: "center", lineHeight: 1.7 }}>
            By continuing you agree to our Terms of Service<br />and Privacy Policy
          </div>
        </div>
        <div className="fu4" style={{ padding: "0 20px 36px", display: "flex", gap: 8, justifyContent: "center", flexWrap: "wrap" }}>
          {["Hindi", "English", "Tamil", "Bengali", "Marathi", "Telugu"].map(l => (
            <div key={l} style={{ background: "#141414", borderRadius: 20, padding: "5px 12px", fontSize: 11, color: "#444" }}>{l}</div>
          ))}
        </div>
      </div>
    </>
  );

  // READER
  if (readingStory) {
    const s = readingStory;
    return (
      <>
        <style>{css}</style>
        <div className="fi" style={{ background: "#0c0c0f", minHeight: "100vh", fontFamily: F.ui }}>
          <div style={{ height: 210, background: s.cover, position: "relative" }}>
            <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to bottom, transparent 30%, #0c0c0f 100%)" }} />
            <button onClick={() => setReadingId(null)} style={{ position: "absolute", top: 48, left: 18, background: "rgba(0,0,0,0.6)", border: "none", borderRadius: 10, width: 38, height: 38, color: "#fff", cursor: "pointer", fontSize: 18, display: "flex", alignItems: "center", justifyContent: "center" }}>←</button>
            <div style={{ position: "absolute", top: 48, right: 18, background: s.accent, borderRadius: 8, padding: "5px 11px", color: "#fff", fontSize: 10 }}>{s.genre}</div>
          </div>
          <div style={{ padding: "0 22px 80px", marginTop: -36 }}>
            <div style={{ fontFamily: F.display, fontSize: 28, color: "#fff", lineHeight: 1.2, marginBottom: 10 }}>{s.title}</div>
            <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 24 }}>
              <div style={{ width: 30, height: 30, borderRadius: "50%", background: s.accent, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, color: "#fff", fontWeight: 500 }}>{s.avatar}</div>
              <span style={{ color: "#777", fontSize: 13 }}>{s.author}</span>
              <span style={{ color: "#444", fontSize: 11 }}>· {s.chapters} ch · {s.reads} reads</span>
            </div>
            <div style={{ fontFamily: F.body, fontSize: 16, color: "#bbb", lineHeight: 1.95 }}>
              {s.excerpt}
              <br /><br />
              The night stretched endlessly, and with it came the kind of silence that speaks. She pressed her palm against the cold glass, watching the dark landscape blur past. Every mile away from home felt like a sentence she couldn't finish.
              <br /><br />
              There are journeys you take for reasons you understand only after you arrive. This was one of them.
            </div>
            <div style={{ marginTop: 32, display: "flex", gap: 10 }}>
              <button style={{ flex: 1, background: s.accent, border: "none", borderRadius: 12, padding: "14px 0", color: "#fff", fontFamily: F.ui, fontSize: 14, fontWeight: 500, cursor: "pointer" }}>Continue reading</button>
              <button onClick={(e) => toggleLike(s.id, e)} style={{ width: 50, background: "#1a1a1a", border: "none", borderRadius: 12, color: s.liked ? "#e94560" : "#666", fontSize: 20, cursor: "pointer" }}>
                {s.liked ? "♥" : "♡"}
              </button>
            </div>
          </div>
        </div>
      </>
    );
  }

  // EDITOR
  if (editorOpen) return (
    <>
      <style>{css}</style>
      <div style={{ background: "#0c0c0f", minHeight: "100vh", fontFamily: F.ui, display: "flex", flexDirection: "column" }}>
        <div style={{ display: "flex", alignItems: "center", padding: "52px 18px 14px", gap: 10, borderBottom: "0.5px solid #1a1a1a" }}>
          <button onClick={() => setEditorOpen(false)} style={{ background: "none", border: "none", color: "#555", fontSize: 14, cursor: "pointer" }}>← Cancel</button>
          <div style={{ flex: 1 }} />
          <div style={{ color: "#444", fontSize: 11 }}>{wordCount} words</div>
          <button onClick={publishStory} style={{ background: "#e94560", border: "none", borderRadius: 10, padding: "8px 18px", color: "#fff", fontFamily: F.ui, fontSize: 13, fontWeight: 500, cursor: "pointer" }}>Publish</button>
        </div>
        <div style={{ padding: "16px 20px 8px" }}>
          <div style={{ display: "flex", gap: 7, marginBottom: 12, overflowX: "auto", paddingBottom: 4 }}>
            {["Drama", "Romance", "Thriller", "Family", "Adventure", "Fantasy"].map(g => (
              <button key={g} onClick={() => setDraft(d => ({ ...d, genre: g }))} style={{ background: draft.genre === g ? "#e94560" : "#141414", border: "none", borderRadius: 20, padding: "5px 13px", color: draft.genre === g ? "#fff" : "#555", fontSize: 11, cursor: "pointer", whiteSpace: "nowrap", flexShrink: 0 }}>{g}</button>
            ))}
          </div>
          <div style={{ display: "flex", gap: 7, overflowX: "auto", paddingBottom: 4, marginBottom: 16 }}>
            {["English", "Hindi", "Marathi", "Bengali", "Tamil", "Telugu", "Kannada"].map(l => (
              <button key={l} onClick={() => setDraft(d => ({ ...d, lang: l }))} style={{ background: draft.lang === l ? "#9b59b6" : "#141414", border: "none", borderRadius: 20, padding: "5px 13px", color: draft.lang === l ? "#fff" : "#555", fontSize: 11, cursor: "pointer", whiteSpace: "nowrap", flexShrink: 0 }}>{l}</button>
            ))}
          </div>
        </div>
        <div style={{ flex: 1, padding: "0 22px 24px", display: "flex", flexDirection: "column", gap: 14 }}>
          <input value={draft.title} onChange={e => setDraft(d => ({ ...d, title: e.target.value }))} placeholder="Your story title..." style={{ background: "none", border: "none", fontFamily: F.display, fontSize: 28, color: "#fff", outline: "none", width: "100%" }} />
          <div style={{ width: 38, height: 2, background: "#e94560", borderRadius: 1 }} />
          <textarea value={draft.content} onChange={e => setDraft(d => ({ ...d, content: e.target.value }))} placeholder="Begin your story here..." style={{ flex: 1, background: "none", border: "none", fontFamily: F.body, fontSize: 16, color: "#bbb", outline: "none", resize: "none", lineHeight: 1.95, minHeight: 300 }} />
        </div>
        <div style={{ padding: "10px 22px 36px", borderTop: "0.5px solid #181818", display: "flex", gap: 12, alignItems: "center" }}>
          <button style={{ background: "none", border: "none", color: "#444", fontWeight: 700, fontSize: 15, cursor: "pointer", padding: "6px 10px" }}>B</button>
          <button style={{ background: "none", border: "none", color: "#444", fontStyle: "italic", fontSize: 15, cursor: "pointer", padding: "6px 10px" }}>I</button>
          <button style={{ background: "none", border: "none", color: "#444", fontSize: 15, cursor: "pointer", padding: "6px 10px" }}>"</button>
          <div style={{ flex: 1 }} />
          <div style={{ fontSize: 11, color: "#333" }}>AI tools in Pro ✦</div>
        </div>
      </div>
    </>
  );

  // MAIN HOME
  const langs = ["For you", "Hindi", "English", "Tamil", "Marathi", "Bengali"];

  return (
    <>
      <style>{css}</style>
      <div style={{ background: "#0c0c0f", minHeight: "100vh", fontFamily: F.ui, maxWidth: 480, margin: "0 auto", position: "relative" }}>

        {/* Header */}
        <div style={{ display: "flex", alignItems: "center", padding: "52px 18px 14px", gap: 10 }}>
          <div style={{ fontFamily: F.display, fontSize: 24, color: "#fff", flex: 1 }}>Kathakar</div>
          <div style={{ width: 36, height: 36, borderRadius: 10, background: "#141414", display: "flex", alignItems: "center", justifyContent: "center", color: "#555", fontSize: 15 }}>🔍</div>
          <div style={{ width: 36, height: 36, borderRadius: 10, background: "linear-gradient(135deg,#e94560,#9b59b6)", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 11, fontWeight: 500 }}>ME</div>
        </div>

        {/* FEED TAB */}
        {tab === "feed" && (
          <div className="fi" style={{ padding: "0 18px 100px" }}>
            <div style={{ display: "flex", gap: 7, overflowX: "auto", paddingBottom: 10, marginBottom: 16 }}>
              {langs.map(l => (
                <div key={l} onClick={() => setFilterLang(l)} style={{ background: filterLang === l ? "#e94560" : "#141414", borderRadius: 20, padding: "6px 14px", fontSize: 12, color: filterLang === l ? "#fff" : "#555", whiteSpace: "nowrap", flexShrink: 0, cursor: "pointer" }}>{l}</div>
              ))}
            </div>
            {/* Featured */}
            <div onClick={() => setReadingId(stories[0].id)} style={{ borderRadius: 18, overflow: "hidden", marginBottom: 14, cursor: "pointer", position: "relative", height: 200, background: stories[0].cover }}>
              <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(0,0,0,0.88) 0%, transparent 50%)" }} />
              <div style={{ position: "absolute", top: 12, left: 12, background: stories[0].accent, borderRadius: 7, padding: "3px 9px", fontSize: 10, color: "#fff" }}>Featured</div>
              <div style={{ position: "absolute", bottom: 0, padding: 14 }}>
                <div style={{ fontFamily: F.display, fontSize: 20, color: "#fff", marginBottom: 3, lineHeight: 1.2 }}>{stories[0].title}</div>
                <div style={{ color: "#999", fontSize: 11 }}>{stories[0].author} · {stories[0].reads} reads</div>
              </div>
            </div>
            {/* Story list */}
            {stories.slice(1).map((s, i) => (
              <div key={s.id} onClick={() => setReadingId(s.id)} style={{ background: "#111", borderRadius: 14, marginBottom: 10, padding: 14, cursor: "pointer", border: "0.5px solid #1c1c1c", display: "flex", gap: 12, alignItems: "flex-start", animation: `fadeUp 0.4s ${i * 0.08}s ease both` }}>
                <div style={{ width: 54, height: 68, borderRadius: 9, background: s.cover, flexShrink: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
                  <div style={{ width: 22, height: 22, borderRadius: "50%", background: s.accent, opacity: 0.7 }} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: "flex", gap: 5, marginBottom: 5 }}>
                    <span style={{ background: s.accent + "22", color: s.accent, fontSize: 9, borderRadius: 5, padding: "2px 7px" }}>{s.genre}</span>
                    <span style={{ background: "#1c1c1c", color: "#555", fontSize: 9, borderRadius: 5, padding: "2px 7px" }}>{s.lang}</span>
                  </div>
                  <div style={{ fontFamily: F.display, fontSize: 14, color: "#fff", marginBottom: 5, lineHeight: 1.3 }}>{s.title}</div>
                  <div style={{ fontSize: 11, color: "#555", marginBottom: 8, lineHeight: 1.5, overflow: "hidden", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical" }}>{s.excerpt}</div>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <span style={{ fontSize: 10, color: "#444" }}>{s.author}</span>
                    <span style={{ fontSize: 10, color: "#333" }}>·</span>
                    <span style={{ fontSize: 10, color: "#444" }}>{s.chapters} ch</span>
                    <div style={{ flex: 1 }} />
                    <button onClick={(e) => toggleLike(s.id, e)} style={{ background: "none", border: "none", color: s.liked ? "#e94560" : "#444", fontSize: 13, cursor: "pointer", display: "flex", alignItems: "center", gap: 3 }}>
                      {s.liked ? "♥" : "♡"}<span style={{ fontSize: 10 }}>{s.likes}</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* WRITE TAB */}
        {tab === "write" && (
          <div className="fi" style={{ padding: "0 18px 100px" }}>
            <div style={{ fontFamily: F.display, fontSize: 26, color: "#fff", marginBottom: 6 }}>Your stories</div>
            <div style={{ color: "#444", fontSize: 13, marginBottom: 22 }}>Write & publish free. AI tools with Pro.</div>
            <button onClick={() => setEditorOpen(true)} style={{ width: "100%", background: "linear-gradient(135deg,#e94560,#9b59b6)", border: "none", borderRadius: 16, padding: "16px", display: "flex", alignItems: "center", gap: 14, cursor: "pointer", marginBottom: 20, textAlign: "left" }}>
              <div style={{ width: 42, height: 42, borderRadius: 10, background: "rgba(255,255,255,0.15)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20 }}>✍️</div>
              <div>
                <div style={{ fontSize: 15, fontWeight: 500, color: "#fff", marginBottom: 2 }}>Start a new story</div>
                <div style={{ fontSize: 12, color: "rgba(255,255,255,0.55)" }}>Write in any language</div>
              </div>
            </button>
            <div style={{ background: "#111", borderRadius: 14, padding: 20, border: "0.5px solid #1c1c1c", textAlign: "center", marginBottom: 16 }}>
              <div style={{ fontSize: 30, marginBottom: 8 }}>📝</div>
              <div style={{ fontFamily: F.display, fontSize: 16, color: "#fff", marginBottom: 5 }}>No stories yet</div>
              <div style={{ fontSize: 12, color: "#444", lineHeight: 1.6 }}>Your published stories will appear here.</div>
            </div>
            <div style={{ background: "#0d1a14", borderRadius: 14, padding: 16, border: "0.5px solid #1a3328" }}>
              <div style={{ fontSize: 12, color: "#2ecc71", fontWeight: 500, marginBottom: 5 }}>✦ Upgrade to Pro — ₹149/mo</div>
              <div style={{ fontSize: 12, color: "#666", lineHeight: 1.6, marginBottom: 10 }}>AI writing, translation to 11 Indian languages, and human-voice audio narration</div>
              <button style={{ background: "#2ecc71", border: "none", borderRadius: 9, padding: "8px 18px", color: "#061208", fontSize: 12, fontWeight: 500, cursor: "pointer" }}>Explore Pro</button>
            </div>
          </div>
        )}

        {/* PROFILE TAB */}
        {tab === "profile" && (
          <div className="fi" style={{ padding: "0 18px 100px" }}>
            <div style={{ textAlign: "center", marginBottom: 24 }}>
              <div style={{ width: 70, height: 70, borderRadius: "50%", background: "linear-gradient(135deg,#e94560,#9b59b6)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 22, color: "#fff", fontWeight: 500, margin: "0 auto 12px" }}>ME</div>
              <div style={{ fontFamily: F.display, fontSize: 20, color: "#fff", marginBottom: 3 }}>Your Name</div>
              <div style={{ color: "#444", fontSize: 12 }}>Free · Member since today</div>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 8, marginBottom: 20 }}>
              {[["0", "Stories"], ["0", "Followers"], ["0", "Following"]].map(([n, l]) => (
                <div key={l} style={{ background: "#111", borderRadius: 11, padding: "13px 8px", textAlign: "center", border: "0.5px solid #1c1c1c" }}>
                  <div style={{ fontSize: 18, fontWeight: 500, color: "#fff", marginBottom: 2 }}>{n}</div>
                  <div style={{ fontSize: 10, color: "#444" }}>{l}</div>
                </div>
              ))}
            </div>
            {[["My library", "Saved stories"], ["Reading history", "Recently read"], ["Language settings", "Set your language"], ["Account & notifications", "Settings"]].map(([t, s]) => (
              <div key={t} style={{ background: "#111", borderRadius: 11, padding: "13px 14px", marginBottom: 8, display: "flex", alignItems: "center", border: "0.5px solid #1c1c1c", cursor: "pointer" }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 13, color: "#ccc" }}>{t}</div>
                  <div style={{ fontSize: 11, color: "#444", marginTop: 1 }}>{s}</div>
                </div>
                <div style={{ color: "#333", fontSize: 16 }}>›</div>
              </div>
            ))}
          </div>
        )}

        {/* Bottom Nav */}
        <div style={{ position: "fixed", bottom: 0, left: "50%", transform: "translateX(-50%)", width: "100%", maxWidth: 480, background: "#0c0c0f", borderTop: "0.5px solid #1a1a1a", display: "flex", padding: "10px 0 28px", zIndex: 10 }}>
          {[["feed", "🏠", "Feed"], ["write", "✍️", "Write"], ["profile", "👤", "Profile"]].map(([t, ic, lb]) => (
            <button key={t} onClick={() => setTab(t)} style={{ flex: 1, background: "none", border: "none", display: "flex", flexDirection: "column", alignItems: "center", gap: 3, cursor: "pointer", opacity: tab === t ? 1 : 0.3 }}>
              <span style={{ fontSize: 19 }}>{ic}</span>
              <span style={{ fontSize: 10, color: tab === t ? "#e94560" : "#666", fontFamily: F.ui }}>{lb}</span>
            </button>
          ))}
        </div>

        {/* Toast */}
        {toast && (
          <div style={{ position: "fixed", bottom: 88, left: "50%", transform: "translateX(-50%)", background: "#1e1e1e", border: "0.5px solid #2a2a2a", borderRadius: 12, padding: "9px 18px", color: "#ccc", fontSize: 13, fontFamily: F.ui, animation: "fadeUp 0.3s ease", whiteSpace: "nowrap", zIndex: 20 }}>
            {toast}
          </div>
        )}
      </div>
    </>
  );
}
