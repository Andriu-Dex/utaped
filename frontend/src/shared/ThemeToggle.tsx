import { useEffect, useState } from 'react'
import { Icon } from './Icon'
import '../styles/theme.css'

type Theme = 'light' | 'dark'
const storageKey = 'utaped-theme'
function storedTheme(): Theme | null {
  try { const value = localStorage.getItem(storageKey); return value === 'light' || value === 'dark' ? value : null }
  catch { return null }
}
function systemTheme(): Theme { return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light' }

export function ThemeToggle() {
  const [theme, setTheme] = useState<Theme>(() => document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light')
  const [preference, setPreference] = useState<Theme | null>(storedTheme)
  useEffect(() => { document.documentElement.dataset.theme = theme }, [theme])
  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const changed = () => { if (!preference) setTheme(media.matches ? 'dark' : 'light') }
    media.addEventListener('change', changed)
    return () => media.removeEventListener('change', changed)
  }, [preference])
  useEffect(() => {
    const changed = (event: StorageEvent) => {
      if (event.key !== storageKey && event.key !== null) return
      const next = storedTheme(); setPreference(next); setTheme(next ?? systemTheme())
    }
    window.addEventListener('storage', changed)
    return () => window.removeEventListener('storage', changed)
  }, [])
  function toggle() {
    const next = theme === 'dark' ? 'light' : 'dark'
    setPreference(next); setTheme(next)
    try { localStorage.setItem(storageKey, next) } catch { /* The current tab still supports switching. */ }
  }
  const label = theme === 'dark' ? 'Modo claro' : 'Modo oscuro'
  return <div className="theme-switch">
    <button type="button" className="theme-toggle" aria-label={label} title={label} onClick={toggle}>
      <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={22} />
    </button>
  </div>
}
