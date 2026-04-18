// src/pages/Reader.jsx
import { useState } from 'react'
import { useAuth } from '../lib/AuthContext'
import { likeStory } from '../lib/firebase'

const F = { display: "'Playfair Display', serif", body: "'Lora', serif", ui: "'DM Sans', sans-serif" }

export default function Reader({ story, onBack }) {
  const { user } = useAuth()
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(story.likes || 0)

  const handleLike = async () => {
    if (!user) return
    const isLiked = await likeStory(story.id, user.uid)
    setLiked(isLiked)
    setLikeCount(c => c + (isLiked ? 1 : -1))
  }

  const accent = '#e94560'

  return (
    <div style={{ background: '#0c0c0f', minHeight: '100vh', fontFamily: F.ui, animation: 'fadeIn 0.3s ease' }}>
      <style>{`@keyframes fadeIn{from{opacity:0}to{opacity:1}}`}</style>

      {/* Cover */}
      <div style={{ height: 200, background: 'linear-gradient(135deg, #1a1a2e, #2d1b69)', position: 'relative' }}>
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to bottom, transparent 30%, #0c0c0f 100%)' }} />
        <button onClick={onBack} style={{ position: 'absolute', top: 48, left: 18, background: 'rgba(0,0,0,0.6)', border: 'none', borderRadius: 10, width: 38, height: 38, color: '#fff', cursor: 'pointer', fontSize: 18, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>←</button>
        {story.genre && (
          <div style={{ position: 'absolute', top: 48, right: 18, background: accent, borderRadius: 8, padding: '5px 11px', color: '#fff', fontSize: 10 }}>{story.genre}</div>
        )}
      </div>

      {/* Content */}
      <div style={{ padding: '0 22px 80px', marginTop: -36 }}>
        <div style={{ fontFamily: F.display, fontSize: 28, color: '#fff', lineHeight: 1.2, marginBottom: 10 }}>{story.title}</div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 24 }}>
          <div style={{ width: 30, height: 30, borderRadius: '50%', background: accent, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: '#fff', fontWeight: 500 }}>
            {story.authorName?.[0]?.toUpperCase() || '?'}
          </div>
          <span style={{ color: '#777', fontSize: 13 }}>{story.authorName}</span>
          <span style={{ color: '#444', fontSize: 11 }}>· {story.lang} · {story.reads || 0} reads</span>
        </div>

        {story.lang && story.lang !== 'English' && (
          <div style={{ background: '#141414', borderRadius: 10, padding: '8px 14px', marginBottom: 20, fontSize: 12, color: '#555', display: 'flex', alignItems: 'center', gap: 8 }}>
            <span>🌐</span> Written in {story.lang}
          </div>
        )}

        <div style={{ fontFamily: F.body, fontSize: 16, color: '#bbb', lineHeight: 1.95, whiteSpace: 'pre-wrap' }}>
          {story.content}
        </div>

        <div style={{ marginTop: 32, display: 'flex', gap: 10 }}>
          <div style={{ flex: 1, background: '#141414', borderRadius: 12, padding: '14px', textAlign: 'center', fontSize: 13, color: '#555' }}>
            {story.reads || 0} reads
          </div>
          <button onClick={handleLike} style={{ width: 56, background: liked ? '#e9456015' : '#141414', border: liked ? '0.5px solid #e9456044' : '0.5px solid #1c1c1c', borderRadius: 12, color: liked ? '#e94560' : '#555', fontSize: 20, cursor: user ? 'pointer' : 'default', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 2 }}>
            <span>{liked ? '♥' : '♡'}</span>
            <span style={{ fontSize: 10 }}>{likeCount}</span>
          </button>
        </div>

        {!user && (
          <div style={{ marginTop: 16, background: '#141414', borderRadius: 12, padding: 14, textAlign: 'center', fontSize: 13, color: '#555' }}>
            Sign in to like and save stories
          </div>
        )}
      </div>
    </div>
  )
}
