// src/pages/Auth.jsx
import { useAuth } from '../lib/AuthContext'

const F = { display: "'Playfair Display', serif", ui: "'DM Sans', sans-serif" }

export default function Auth() {
  const { loginWithGoogle } = useAuth()

  return (
    <div style={{ background: '#0c0c0f', minHeight: '100vh', fontFamily: F.ui, display: 'flex', flexDirection: 'column' }}>
      <style>{`
        @keyframes fadeUp{from{opacity:0;transform:translateY(16px)}to{opacity:1;transform:translateY(0)}}
        .fu{animation:fadeUp 0.45s ease both}
        .fu2{animation:fadeUp 0.45s 0.15s ease both}
        .fu3{animation:fadeUp 0.45s 0.3s ease both}
        .fu4{animation:fadeUp 0.45s 0.45s ease both}
        .google-btn:hover{background:#f5f5f5!important}
        .email-btn:hover{background:#1a1a1a!important}
      `}</style>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 28px' }}>
        <div className="fu" style={{ textAlign: 'center', marginBottom: 48 }}>
          <div style={{ fontFamily: F.display, fontSize: 42, color: '#fff', lineHeight: 1.15, marginBottom: 14 }}>
            Stories that<br /><em style={{ color: '#e94560' }}>move you</em>
          </div>
          <div style={{ color: '#555', fontSize: 14, lineHeight: 1.7 }}>
            Read, write and share stories<br />in your language
          </div>
        </div>

        <div className="fu2" style={{ width: '100%', maxWidth: 340 }}>
          <button
            className="google-btn"
            onClick={loginWithGoogle}
            style={{ width: '100%', background: '#fff', border: 'none', borderRadius: 14, padding: '15px 20px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, cursor: 'pointer', fontFamily: F.ui, fontSize: 15, fontWeight: 500, color: '#1a1a1a', marginBottom: 12, transition: 'background 0.15s' }}>
            <svg width="20" height="20" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" />
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
            </svg>
            Continue with Google
          </button>
        </div>

        <div className="fu3" style={{ marginTop: 28, color: '#333', fontSize: 11, textAlign: 'center', lineHeight: 1.7 }}>
          By continuing you agree to our Terms of Service<br />and Privacy Policy
        </div>
      </div>

      <div className="fu4" style={{ padding: '0 20px 36px', display: 'flex', gap: 8, justifyContent: 'center', flexWrap: 'wrap' }}>
        {['Hindi', 'English', 'Tamil', 'Bengali', 'Marathi', 'Telugu'].map(l => (
          <div key={l} style={{ background: '#141414', borderRadius: 20, padding: '5px 12px', fontSize: 11, color: '#444' }}>{l}</div>
        ))}
      </div>
    </div>
  )
}
