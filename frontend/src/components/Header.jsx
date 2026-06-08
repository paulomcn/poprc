import { User } from 'lucide-react'

export default function Header({ userName = 'Usuário' }) {
  return (
    <header className="bg-white border-b border-slate-200 shadow-sm">
      <div className="flex items-center justify-between px-6 py-4">
        <h2 className="text-xl font-semibold text-slate-800">RC Operations Hub</h2>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 px-4 py-2 rounded-lg bg-slate-100">
            <User size={18} className="text-slate-600" />
            <span className="text-sm font-medium text-slate-700">{userName}</span>
          </div>
        </div>
      </div>
    </header>
  )
}
