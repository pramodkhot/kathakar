// src/pages/Feed.jsx
import { useState, useEffect } from 'react'
import { useAuth } from '../lib/AuthContext'
import { getStories, likeStory, getUserLikes, incrementReads } from '../lib/firebase'

const F = { display: "'Playfair Display', serif", ui: "'DM Sans', sans-serif" }
const LANGS = ['For you', 'Hindi', 'English', 'Tamil', 'Marathi', 'Bengali', 'Telugu', 'Kannada']
const ACCENTS = ['#e94560', '#f5a623', '#2ecc71', '#9b59b6', '#3498db', '#e67e22', '#1abc9c', '#e91e8c']

function accent(i) { return ACCENTS[i % ACCENTS.length] }

export default function Feed({ onOpenStory }) {
  const { user } = useAuth()
  const [stories, setStories] = useState([])
  const [likedIds, setLikedIds] = useState(new Set())
  const [filterLang, setFilterLang] = useState('For you')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadStories()
    if (user) loadLikes()
  }, [filterLang, user])

  const loadStories = async () => {
    setLoading(true)
    try {
      const data = await getStories(filterLang)
      setStories(data)
    } catch (e) {
      console.error(e)
    }
    setLoading(false)
  }

  const loadLikes = async () => {
    const ids = await getUserLikes(user.uid)
    setLikedIds(ids)
  }

  const handleLike = async (e, storyId) => {
    e.stopPropagation()
    if (!user) return
    const liked = await likeStory(storyId, user.uid)
    setLikedIds(prev => {
      const next = new Set(prev)
      liked ? next.add(storyId) : next.delete(storyId)
      return next
    })
    setStories(prev => prev.map(s => s.id === storyId
      ? { ...s, likes: s.likes + (liked ? 1 : -1) }
      : s))
  }

  const handleOpen = (story) => {
    incrementReads(story.id)
    onOpenStory(story)
  }

  const initials = (name) => name ? name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() : '??'

  return (
    <div style={{ padding: '0 16px 100px', animation: 'fadeIn 0.3s ease' }}>
      <style>{`@keyframes fadeIn{from{opacity:0}to{opacity:1}} @keyframes fadeUp{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}`}</style>

      {/* Language filter */}
      <div style={{ display: 'flex', gap: 7, overflowX: 'auto', paddingBottom: 12, marginBottom: 16 }}>
        {LANGS.map(l => (
          <div key={l} onClick={() => setFilterLang(l)} style={{ background: filterLang === l ? '#e94560' : '#141414', borderRadius: 20, padding: '6px 14px', fontSize: 12, color: filterLang === l ? '#fff' : '#555', whiteSpace: 'nowrap', flexShrink: 0, cursor: 'pointer' }}>{l}</div>
        ))}
      </div>

      {loading && (
        <div style={{ textAlign: 'center', padding: '40px 0', color: '#444', fontSize: 13 }}>Loading stories...</div>
      )}

      {!loading && stories.length === 0 && (
        <div style={{ textAlign: 'center', padding: '60px 20px' }}>
          <div style={{ fontSize: 36, marginBottom: 12 }}>📖</div>
          <div style={{ fontFamily: F.display, fontSize: 18, color: '#fff', marginBottom: 6 }}>No stories yet</div>
          <div style={{ color: '#444', fontSize: 13 }}>Be the first to write one!</div>
        </div>
      )}

      {/* Featured first story */}
      {!loading && stories[0] && (
        <div onClick={() => handleOpen(stories[0])} style={{ borderRadius: 18, overflow: 'hidden', marginBottom: 14, cursor: 'pointer', position: 'relative', height: 200, background: accent(0) + '33', border: '0.5px solid ' + accent(0) + '44' }}>
          <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, rgba(0,0,0,0.88) 0%, transparent 50%)' }} />
          <div style={{ position: 'absolute', top: 12, left: 12, background: accent(0), borderRadius: 7, padding: '3px 9px', fontSize: 10, color: '#fff' }}>Featured</div>
          <div style={{ position: 'absolute', bottom: 0, padding: 14, left: 0, right: 0 }}>
            <div style={{ fontFamily: F.display, fontSize: 20, color: '#fff', marginBottom: 3, lineHeight: 1.2 }}>{stories[0].title}</div>
            <div style={{ color: '#999', fontSize: 11 }}>{stories[0].authorName} · {stories[0].reads || 0} reads</div>
          </div>
        </div>
      )}

      {/* Story list */}
      {!loading && stories.slice(1).map((s, i) => (
        <div key={s.id} onClick={() => handleOpen(s)} style={{ background: '#111', borderRadius: 14, marginBottom: 10, padding: 14, cursor: 'pointer', border: '0.5px solid #1c1c1c', display: 'flex', gap: 12, alignItems: 'flex-start', animation: `fadeUp 0.4s ${i * 0.06}s ease both` }}>
          <div style={{ width: 54, height: 68, borderRadius: 9, background: accent(i + 1) + '22', border: '0.5px solid ' + accent(i + 1) + '33', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <span style={{ fontFamily: F.display, fontSize: 11, color: accent(i + 1) }}>{initials(s.authorName)}</span>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', gap: 5, marginBottom: 5 }}>
              {s.genre && <span style={{ background: accent(i + 1) + '22', color: accent(i + 1), fontSize: 9, borderRadius: 5, padding: '2px 7px' }}>{s.genre}</span>}
              {s.lang && <span style={{ background: '#1c1c1c', color: '#555', fontSize: 9, borderRadius: 5, padding: '2px 7px' }}>{s.lang}</span>}
            </div>
            <div style={{ fontFamily: F.display, fontSize: 14, color: '#fff', marginBottom: 5, lineHeight: 1.3 }}>{s.title}</div>
            <div style={{ fontSize: 11, color: '#555', marginBottom: 8, lineHeight: 1.5, overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>{s.excerpt || s.content?.slice(0, 100)}</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span style={{ fontSize: 10, color: '#444' }}>{s.authorName}</span>
              <span style={{ fontSize: 10, color: '#333' }}>·</span>
              <span style={{ fontSize: 10, color: '#444' }}>{s.reads || 0} reads</span>
              <div style={{ flex: 1 }} />
              <button onClick={(e) => handleLike(e, s.id)} style={{ background: 'none', border: 'none', color: likedIds.has(s.id) ? '#e94560' : '#444', fontSize: 13, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 3 }}>
                {likedIds.has(s.id) ? '♥' : '♡'}<span style={{ fontSize: 10 }}>{s.likes || 0}</span>
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
