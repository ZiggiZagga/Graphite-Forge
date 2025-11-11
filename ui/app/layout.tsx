import './globals.css'
import React from 'react'

export const metadata = {
  title: 'Graphite UI',
  description: 'Next.js UI for Graphite-Forge'
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <div className="min-h-screen flex flex-col">
          <header className="bg-white shadow-sm">
            <div className="max-w-6xl mx-auto px-4 py-4">Graphite UI</div>
          </header>
          <main className="flex-1 max-w-6xl mx-auto p-6">{children}</main>
          <footer className="bg-white border-t py-4">
            <div className="max-w-6xl mx-auto px-4 text-sm text-gray-600">© Graphite Forge</div>
          </footer>
        </div>
      </body>
    </html>
  )
}
