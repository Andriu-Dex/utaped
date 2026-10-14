import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { ThemeToggle } from './shared/ThemeToggle'
import { ConfirmationDialog } from './shared/ConfirmationDialog'
import { ToastHost } from './shared/ToastHost'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
    <ThemeToggle />
    <ConfirmationDialog />
    <ToastHost />
  </StrictMode>,
)
