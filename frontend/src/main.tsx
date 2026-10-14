import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { ThemeToggle } from './shared/ThemeToggle'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
    <ThemeToggle />
  </StrictMode>,
)
