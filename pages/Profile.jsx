// src/pages/Profile.jsx
import { useState, useEffect } from 'react'
import { useAuth } from '../lib/AuthContext'
import { getUserStories } from '../lib/firebase'

const F = { display: "'Playfair Display', serif", ui: "'DM Sans', sans-serif" }

export default function Profile({ onOpenStory, onWriteStory }) {
  const { user, profile, logout } = useAuth()
  const [stories, setStories] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (user) loadMyStories()
  }, [user])

  const loadMyStories = async () => {
    setLoading(true)
    const data = await getUserStories(user.uid)
    setStories(data)
    setLoading(false)
  }

  const initials = (name) => name ? name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() : 'ME'

  return (
    <div style={{ padding: '0 18px 100px', animation: 'fadeIn 0.3s ease' }}>
      <style>{`@keyframes fadeIn{from{opacity:0}to{opacity:1}}`}</style>

      {/* Avatar & name */}
      <div style={{ textAlign: 'center', marginBottom: 24 }}>
        {user?.photoURL
          ? <img src={user.photoURL} alt="avatar" style={{ width: 70, height: 70, borderRadius: '50%', margin: '0 auto 12px', display: 'block', objectFit: 'cover' }} />
          : <div style={{ width: 70, height: 70, borderRadius: '50%', background: 'linear-gradient(135deg,#e94560,#9b59b6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, color: '#fff', fontWeight: 500, margin: '0 auto 12px' }}>{initials(profile?.name)}</div>
        }
        <div style={{ fontFamily: F.display, fontSize: 20, color: '#fff', marginBottom: 3 }}>{profile?.name || user?.displayName || 'Anonymous'}</div>
        <div style={{ color: '#444', fontSize: 12 }}>{user?.email}</div>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 20 }}>
        {[
          [stories.length, 'Stories'],
          [profile?.followers || 0, 'Followers'],
          [profile?.following || 0, 'Following'],
        ].map(([n, l]) => (
          <div key={l} style={{ background: '#111', borderRadius: 11, padding: '13px 8px', textAlign: 'center', border: '0.5px solid #1c1c1c' }}>
            <div style={{ fontSize: 18, fontWeight: 500, color: '#fff', marginBottom: 2 }}>{n}</div>
            <div style={{ fontSize: 10, color: '#444' }}>{l}</div>
          </div>
        ))}
      </div>

      {/* My stories */}
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontFamily: F.display, fontSize: 17, color: '#fff', marginBottom: 12 }}>My stories</div>

        {loading && <div style={{ color: '#444', fontSize: 13, textAlign: 'center', padding: '20px 0' }}>Loading...</div>}

        {!loading && stories.length === 0 && (
          <div style={{ background: '#111', borderRadius: 14, padding: 20, textAlign: 'center', border: '0.5px solid #1c1c1c', marginBottom: 12 }}>
            <div style={{ fontSize: 28, marginBottom: 8 }}>📝</div>
            <div style={{ fontFamily: F.display, fontSize: 15, color: '#fff', marginBottom: 5 }}>No stories yet</div>
            <div style={{ fontSize: 12, color: '#444', marginBottom: 14 }}>Start writing your first story</div>
            <button onClick={onWriteStory} style={{ background: '#e94560', border: 'none', borderRadius: 10, padding: '9px 20px', color: '#fff', fontFamily: F.ui, fontSize: 13, cursor: 'pointer' }}>Write a story</button>
          </div>
        )}

        {stories.map(s => (
          <div key={s.id} onClick={() => onOpenStory(s)} style={{ background: '#111', borderRadius: 12, padding: '12px 14px', marginBottom: 8, border: '0.5px solid #1c1c1c', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontFamily: F.display, fontSize: 14, color: '#fff', marginBottom: 3 }}>{s.title}</div>
              <div style={{ fontSize: 11, color: '#444' }}>{s.lang} · {s.reads || 0} reads · {s.likes || 0} likes</div>
            </div>
            <div style={{ color: '#333', fontSize: 16 }}>›</div>
          </div>
        ))}
      </div>

      {/* Menu */}
      {[
        ['Language settings', 'Set your preferred language'],
        ['Reading history', 'Stories you have read'],
        ['Account settings', 'Profile and notifications'],
      ].map(([t, s]) => (
        <div key={t} style={{ background: '#111', borderRadius: 11, padding: '13px 14px', marginBottom: 8, display: 'flex', alignItems: 'center', border: '0.5px solid #1c1c1c', cursor: 'pointer' }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13, color: '#ccc' }}>{t}</div>
            <div style={{ fontSize: 11, color: '#444', marginTop: 1 }}>{s}</div>
          </div>
          <div style={{ color: '#333', fontSize: 16 }}>›</div>
        </div>
      ))}

      {/* Sign out */}
      <button onClick={logout} style={{ width: '100%', marginTop: 8, background: 'none', border: '0.5px solid #2a2a2a', borderRadius: 11, padding: '13px', color: '#555', fontFamily: F.ui, fontSize: 13, cursor: 'pointer' }}>
        Sign out
      </button>
    </div>
  )
}
