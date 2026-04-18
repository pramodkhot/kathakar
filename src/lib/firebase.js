// src/lib/firebase.js
// ─────────────────────────────────────────────
// STEP 1: Go to https://console.firebase.google.com
// STEP 2: Create project "kathakar"
// STEP 3: Add a Web App → copy the config below
// STEP 4: Replace every value in firebaseConfig
// ─────────────────────────────────────────────

import { initializeApp } from 'firebase/app'
import { getAuth, GoogleAuthProvider } from 'firebase/auth'
import {
  getFirestore,
  collection,
  doc,
  addDoc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  orderBy,
  limit,
  where,
  onSnapshot,
  serverTimestamp,
  increment,
} from 'firebase/firestore'
import { getStorage } from 'firebase/storage'

// ── REPLACE THIS WITH YOUR FIREBASE CONFIG ──
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

const app = initializeApp(firebaseConfig)

export const auth = getAuth(app)
export const googleProvider = new GoogleAuthProvider()
export const db = getFirestore(app)
export const storage = getStorage(app)

// ── FIRESTORE HELPERS ──

// Stories
export const storiesRef = collection(db, 'stories')

export const getStories = async (filterLang = null) => {
  let q = filterLang && filterLang !== 'For you'
    ? query(storiesRef, where('lang', '==', filterLang), orderBy('createdAt', 'desc'), limit(20))
    : query(storiesRef, orderBy('createdAt', 'desc'), limit(20))
  const snap = await getDocs(q)
  return snap.docs.map(d => ({ id: d.id, ...d.data() }))
}

export const getStory = async (id) => {
  const snap = await getDoc(doc(db, 'stories', id))
  return snap.exists() ? { id: snap.id, ...snap.data() } : null
}

export const createStory = async (data) => {
  return await addDoc(storiesRef, {
    ...data,
    likes: 0,
    reads: 0,
    createdAt: serverTimestamp(),
  })
}

export const incrementReads = (id) =>
  updateDoc(doc(db, 'stories', id), { reads: increment(1) })

// Likes
export const likeStory = async (storyId, userId) => {
  const likeRef = doc(db, 'likes', `${userId}_${storyId}`)
  const likeSnap = await getDoc(likeRef)
  const storyRef = doc(db, 'stories', storyId)
  if (likeSnap.exists()) {
    await deleteDoc(likeRef)
    await updateDoc(storyRef, { likes: increment(-1) })
    return false
  } else {
    await setDoc(likeRef, { userId, storyId, createdAt: serverTimestamp() })
    await updateDoc(storyRef, { likes: increment(1) })
    return true
  }
}

export const getUserLikes = async (userId) => {
  const q = query(collection(db, 'likes'), where('userId', '==', userId))
  const snap = await getDocs(q)
  return new Set(snap.docs.map(d => d.data().storyId))
}

// Users
export const getUser = async (uid) => {
  const snap = await getDoc(doc(db, 'users', uid))
  return snap.exists() ? { id: snap.id, ...snap.data() } : null
}

export const createOrUpdateUser = async (user) => {
  const ref = doc(db, 'users', user.uid)
  const snap = await getDoc(ref)
  if (!snap.exists()) {
    await setDoc(ref, {
      uid: user.uid,
      name: user.displayName || 'Anonymous',
      email: user.email,
      avatar: user.photoURL || null,
      bio: '',
      followers: 0,
      following: 0,
      storiesCount: 0,
      createdAt: serverTimestamp(),
    })
  }
  return getUser(user.uid)
}

// Follow
export const followUser = async (followerId, followingId) => {
  const ref = doc(db, 'follows', `${followerId}_${followingId}`)
  const snap = await getDoc(ref)
  if (snap.exists()) {
    await deleteDoc(ref)
    await updateDoc(doc(db, 'users', followingId), { followers: increment(-1) })
    await updateDoc(doc(db, 'users', followerId), { following: increment(-1) })
    return false
  } else {
    await setDoc(ref, { followerId, followingId, createdAt: serverTimestamp() })
    await updateDoc(doc(db, 'users', followingId), { followers: increment(1) })
    await updateDoc(doc(db, 'users', followerId), { following: increment(1) })
    return true
  }
}

export const getUserStories = async (uid) => {
  const q = query(storiesRef, where('authorId', '==', uid), orderBy('createdAt', 'desc'))
  const snap = await getDocs(q)
  return snap.docs.map(d => ({ id: d.id, ...d.data() }))
}

export { serverTimestamp, onSnapshot }
