// src/pages/Editor.jsx
import { useState } from 'react'
import { useAuth } from '../lib/AuthContext'
import { createStory } from '../lib/firebase'

const F = { display: "'Playfair Display', serif", body: "'Lora', serif", ui: "'DM Sans', sans-serif" }
const GENRES = ['Drama', 'Romance', 'Thriller', 'Family', 'Adventure', 'Fantasy', 'Horror', 'Comedy']
const LANGS = ['English', 'Hindi', 'Marathi', 'Bengali', 'Tamil', 'Telugu', 'Kannada', 'Gujarati', 'Punjabi', 'Odia', 'Urdu']

export default function Editor({ onDone, onCancel }) {
  const { user, profile } = useAuth()
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [genre, setGenre] = useState('Drama')
  const [lang, setLang] = useState('English')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const wordCount = content.trim() ? content.trim().split(/\s+/).length : 0

  const publish = async () => {
    if (!title.trim()) { setError('Please add a title'); return }
    if (!content.trim() || wordCount < 10) { setError('Please write at least 10 words'); return }
    setError('')
    setSaving(true)
    try {
      await createStory({
        title: title.trim(),
        content: content.trim(),
        excerpt: content.trim().slice(0, 150) + (content.length > 150 ? '...' : ''),
        genre,
        lang,
        authorId: user.uid,
        authorName: profile?.name || user.displayName || 'Anonymous',
        authorAvatar: user.photoURL || null,
      })
      onDone()
    } catch (e) {
      console.error(e)
      setError('Failed to publish. Please try again.')
    }
    setSaving(false)
  }

  return (
    <div style={{ background: '#0c0c0f', minHeight: '100vh', fontFamily: F.ui, display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', padding: '52px 18px 14px', gap: 10, borderBottom: '0.5px solid #1a1a1a' }}>
        <button onClick={onCancel} style={{ background: 'none', border: 'none', color: '#555', fontSize: 14, cursor: 'pointer', fontFamily: F.ui }}>← Cancel</button>
        <div style={{ flex: 1 }} />
        <div style={{ color: '#444', fontSize: 11 }}>{wordCount} words</div>
        <button onClick={publish} disabled={saving} style={{ background: saving ? '#555' : '#e94560', border: 'none', borderRadius: 10, padding: '8px 20px', color: '#fff', fontFamily: F.ui, fontSize: 13, fontWeight: 500, cursor: saving ? 'default' : 'pointer' }}>
          {saving ? 'Publishing...' : 'Publish'}
        </button>
      </div>

      {/* Genre */}
      <div style={{ padding: '14px 18px 0' }}>
        <div style={{ display: 'flex', gap: 7, overflowX: 'auto', paddingBottom: 10, marginBottom: 4 }}>
          {GENRES.map(g => (
            <button key={g} onClick={() => setGenre(g)} style={{ background: genre === g ? '#e94560' : '#141414', border: 'none', borderRadius: 20, padding: '5px 13px', color: genre === g ? '#fff' : '#555', fontSize: 11, cursor: 'pointer', whiteSpace: 'nowrap', flexShrink: 0, fontFamily: F.ui }}>{g}</button>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 7, overflowX: 'auto', paddingBottom: 10 }}>
          {LANGS.map(l => (
            <button key={l} onClick={() => setLang(l)} style={{ background: lang === l ? '#9b59b6' : '#141414', border: 'none', borderRadius: 20, padding: '5px 13px', color: lang === l ? '#fff' : '#555', fontSize: 11, cursor: 'pointer', whiteSpace: 'nowrap', flexShrink: 0, fontFamily: F.ui }}>{l}</button>
          ))}
        </div>
      </div>

      {/* Writing area */}
      <div style={{ flex: 1, padding: '8px 22px 24px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <input
          value={title}
          onChange={e => setTitle(e.target.value)}
          placeholder="Your story title..."
          maxLength={100}
          style={{ background: 'none', border: 'none', fontFamily: F.display, fontSize: 26, color: '#fff', outline: 'none', width: '100%', caretColor: '#e94560' }}
        />
        <div style={{ width: 38, height: 2, background: '#e94560', borderRadius: 1 }} />
        <textarea
          value={content}
          onChange={e => setContent(e.target.value)}
          placeholder="Begin your story here..."
          style={{ flex: 1, background: 'none', border: 'none', fontFamily: F.body, fontSize: 16, color: '#bbb', outline: 'none', resize: 'none', lineHeight: 1.95, minHeight: 300, caretColor: '#e94560' }}
        />
        {error && <div style={{ color: '#e94560', fontSize: 12 }}>{error}</div>}
      </div>

      {/* Toolbar */}
      <div style={{ padding: '10px 22px 36px', borderTop: '0.5px solid #181818', display: 'flex', gap: 12, alignItems: 'center' }}>
        <span style={{ fontSize: 11, color: '#333', fontFamily: F.ui }}>Writing as {profile?.name || 'you'}</span>
        <div style={{ flex: 1 }} />
        <div style={{ fontSize: 11, color: '#333' }}>AI tools coming in Pro ✦</div>
      </div>
    </div>
  )
}
