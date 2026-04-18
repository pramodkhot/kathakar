// src/App.jsx
import { useState } from 'react'
import { AuthProvider, useAuth } from './lib/AuthContext'
import Splash from './pages/Splash'
import Auth from './pages/Auth'
import Feed from './pages/Feed'
import Reader from './pages/Reader'
import Editor from './pages/Editor'
import Profile from './pages/Profile'

const F = { display: "'Playfair Display', serif", ui: "'DM Sans', sans-serif" }

function AppShell() {
  const { user, loading } = useAuth()
  const [screen, setScreen] = useState('splash') // splash | auth | home
  const [tab, setTab] = useState('feed')
  const [readingStory, setReadingStory] = useState(null)
  const [editorOpen, setEditorOpen] = useState(false)
  const [toast, setToast] = useState(null)

  const showToast = (msg) => {
    setToast(msg)
    setTimeout(() => setToast(null), 2500)
  }

  // Loading Firebase auth state
  if (loading) return (
    <div style={{ background: '#0c0c0f', minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ width: 32, height: 32, borderRadius: '50%', border: '2px solid #e94560', borderTopColor: 'transparent', animation: 'spin 0.8s linear infinite' }} />
      <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
    </div>
  )

  // Splash screen
  if (screen === 'splash') return <Splash onReady={() => setScreen(user ? 'home' : 'auth')} />

  // Auth screen — only if not logged in
  if (screen === 'auth' && !user) return <Auth />

  // If user just logged in, go to home
  if (screen === 'auth' && user) {
    setScreen('home')
    return null
  }

  // Story reader overlay
  if (readingStory) return (
    <Reader story={readingStory} onBack={() => setReadingStory(null)} />
  )

  // Editor overlay
  if (editorOpen) return (
    <Editor
      onDone={() => { setEditorOpen(false); setTab('feed'); showToast('Story published! 🎉') }}
      onCancel={() => setEditorOpen(false)}
    />
  )

  // Main app
  return (
    <div style={{ background: '#0c0c0f', minHeight: '100vh', fontFamily: F.ui, maxWidth: 480, margin: '0 auto', position: 'relative' }}>
      <style>{`
        * { box-sizing: border-box; margin: 0; padding: 0; -webkit-font-smoothing: antialiased; }
        ::-webkit-scrollbar { width: 0; }
        body { background: #0c0c0f; }
      `}</style>

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', padding: '52px 18px 14px', gap: 10 }}>
        <div style={{ fontFamily: F.display, fontSize: 22, color: '#fff', flex: 1 }}>Kathakar</div>
        <div style={{ width: 34, height: 34, borderRadius: 10, background: '#141414', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#555', fontSize: 15 }}>🔍</div>
        {user?.photoURL
          ? <img src={user.photoURL} onClick={() => setTab('profile')} style={{ width: 34, height: 34, borderRadius: 10, objectFit: 'cover', cursor: 'pointer' }} alt="avatar" />
          : <div onClick={() => setTab('profile')} style={{ width: 34, height: 34, borderRadius: 10, background: 'linear-gradient(135deg,#e94560,#9b59b6)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 11, fontWeight: 500, cursor: 'pointer' }}>
            {user ? user.displayName?.[0]?.toUpperCase() : '?'}
          </div>
        }
      </div>

      {/* Tab content */}
      {tab === 'feed' && (
        <Feed
          onOpenStory={(s) => setReadingStory(s)}
        />
      )}

      {tab === 'write' && (
        <div style={{ padding: '0 18px 100px' }}>
          <div style={{ fontFamily: F.display, fontSize: 26, color: '#fff', marginBottom: 6 }}>Your stories</div>
          <div style={{ color: '#444', fontSize: 13, marginBottom: 22 }}>Write & publish free. AI tools coming soon.</div>

          <button onClick={() => { if (!user) { setScreen('auth') } else { setEditorOpen(true) } }}
            style={{ width: '100%', background: 'linear-gradient(135deg,#e94560,#9b59b6)', border: 'none', borderRadius: 16, padding: '16px', display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer', marginBottom: 20, textAlign: 'left' }}>
            <div style={{ width: 42, height: 42, borderRadius: 10, background: 'rgba(255,255,255,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>✍️</div>
            <div>
              <div style={{ fontSize: 15, fontWeight: 500, color: '#fff', marginBottom: 2 }}>Start a new story</div>
              <div style={{ fontSize: 12, color: 'rgba(255,255,255,0.55)' }}>Write in any language</div>
            </div>
          </button>

          {!user && (
            <div style={{ background: '#141414', borderRadius: 14, padding: 16, textAlign: 'center', border: '0.5px solid #2a2a2a' }}>
              <div style={{ fontSize: 13, color: '#555', marginBottom: 10 }}>Sign in to write and publish stories</div>
              <button onClick={() => setScreen('auth')} style={{ background: '#e94560', border: 'none', borderRadius: 10, padding: '9px 20px', color: '#fff', fontFamily: F.ui, fontSize: 13, cursor: 'pointer' }}>Sign in</button>
            </div>
          )}

          {user && (
            <Profile
              onOpenStory={(s) => setReadingStory(s)}
              onWriteStory={() => setEditorOpen(true)}
            />
          )}
        </div>
      )}

      {tab === 'profile' && (
        !user
          ? <div style={{ padding: '40px 20px', textAlign: 'center' }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>👤</div>
            <div style={{ fontFamily: F.display, fontSize: 18, color: '#fff', marginBottom: 8 }}>Sign in to view profile</div>
            <button onClick={() => setScreen('auth')} style={{ background: '#e94560', border: 'none', borderRadius: 10, padding: '10px 24px', color: '#fff', fontFamily: F.ui, fontSize: 13, cursor: 'pointer' }}>Sign in with Google</button>
          </div>
          : <Profile
            onOpenStory={(s) => setReadingStory(s)}
            onWriteStory={() => { setTab('write'); setEditorOpen(true) }}
          />
      )}

      {/* Bottom Nav */}
      <div style={{ position: 'fixed', bottom: 0, left: '50%', transform: 'translateX(-50%)', width: '100%', maxWidth: 480, background: '#0c0c0f', borderTop: '0.5px solid #1a1a1a', display: 'flex', padding: '10px 0 28px', zIndex: 10 }}>
        {[['feed', '🏠', 'Feed'], ['write', '✍️', 'Write'], ['profile', '👤', 'Profile']].map(([t, ic, lb]) => (
          <button key={t} onClick={() => setTab(t)} style={{ flex: 1, background: 'none', border: 'none', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, cursor: 'pointer', opacity: tab === t ? 1 : 0.3 }}>
            <span style={{ fontSize: 19 }}>{ic}</span>
            <span style={{ fontSize: 10, color: tab === t ? '#e94560' : '#666', fontFamily: F.ui }}>{lb}</span>
          </button>
        ))}
      </div>

      {/* Toast */}
      {toast && (
        <div style={{ position: 'fixed', bottom: 88, left: '50%', transform: 'translateX(-50%)', background: '#1e1e1e', border: '0.5px solid #2a2a2a', borderRadius: 12, padding: '9px 18px', color: '#ccc', fontSize: 13, fontFamily: F.ui, animation: 'fadeUp 0.3s ease', whiteSpace: 'nowrap', zIndex: 20 }}>
          {toast}
        </div>
      )}
      <style>{`@keyframes fadeUp{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}`}</style>
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  )
}
