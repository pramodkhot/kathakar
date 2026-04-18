// src/pages/Splash.jsx
const F = { display: "'Playfair Display', serif", ui: "'DM Sans', sans-serif" }

export default function Splash({ onReady }) {
  return (
    <div style={{ background: '#0c0c0f', minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', fontFamily: F.ui, position: 'relative', animation: 'fadeIn 0.6s ease' }}>
      <style>{`
        @keyframes fadeIn{from{opacity:0}to{opacity:1}}
        @keyframes fadeUp{from{opacity:0;transform:translateY(16px)}to{opacity:1;transform:translateY(0)}}
        @keyframes pulse{0%,100%{opacity:1}50%{opacity:0.3}}
      `}</style>
      <div style={{ textAlign: 'center' }}>
        <div style={{ width: 76, height: 76, borderRadius: 22, background: 'linear-gradient(135deg,#e94560,#9b59b6)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 22px', fontSize: 34 }}>📖</div>
        <div style={{ fontFamily: F.display, fontSize: 40, color: '#fff', letterSpacing: '-0.5px', marginBottom: 8 }}>Kathakar</div>
        <div style={{ fontSize: 12, color: '#555', letterSpacing: '3px', textTransform: 'uppercase' }}>Stories in every language</div>
      </div>
      <div style={{ marginTop: 56, display: 'flex', gap: 6 }}>
        {[0, 1, 2].map(i => (
          <div key={i} style={{ width: 6, height: 6, borderRadius: '50%', background: i === 0 ? '#e94560' : '#2a2a2a', animation: `pulse 1.5s ${i * 0.3}s infinite` }} />
        ))}
      </div>
      <div style={{ position: 'absolute', bottom: 52, width: '100%', display: 'flex', justifyContent: 'center', animation: 'fadeUp 0.5s 1.8s ease both' }}>
        <button onClick={onReady} style={{ background: '#e94560', border: 'none', borderRadius: 14, padding: '14px 48px', color: '#fff', fontFamily: F.ui, fontSize: 15, fontWeight: 500, cursor: 'pointer' }}>
          Get started
        </button>
      </div>
    </div>
  )
}
