import { User } from 'lucide-react'
import rcLogo from '../assets/rclogo.jpg' // Puxando a logo braba dos assets

export default function Header({ userName = 'Usuário' }) {
  return (
    <header className="bg-white border-b border-slate-200 shadow-sm">
      <div className="flex items-center justify-between px-4 sm:px-6 py-3 sm:py-4">
        
        {/* Lado Esquerdo: Logo da Empresa + Título */}
        <div className="flex items-center gap-2 sm:gap-3">
          <img 
            src={rcLogo} 
            alt="RC Logo" 
            className="h-8 sm:h-10 w-auto object-contain rounded" 
          />
          <h2 className="text-base sm:text-xl font-semibold text-slate-800">
            RC Operations Hub
          </h2>
        </div>

        {/* Lado Direito: Nome do Usuário */}
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-1.5 sm:gap-2 px-3 py-1.5 sm:px-4 sm:py-2 rounded-lg bg-slate-100">
            <User size={16} className="text-slate-600 sm:w-[18px] sm:h-[18px]" />
            <span className="text-xs sm:text-sm font-medium text-slate-700">{userName}</span>
          </div>
        </div>

      </div>
    </header>
  )
}