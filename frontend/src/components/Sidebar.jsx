import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  FileText,
  Briefcase,
  Users,
  LogOut,
  Menu,
  X
} from 'lucide-react'

export default function Sidebar() {
  const location = useLocation()
  const [isOpen, setIsOpen] = useState(false)

  const menuItems = [
    { path: '/', icon: LayoutDashboard, label: 'Dashboard' },
    { path: '/contratos', icon: FileText, label: 'Contratos' },
    { path: '/projetos', icon: Briefcase, label: 'Projetos' },
    { path: '/funcionarios', icon: Users, label: 'Funcionários' },
  ]

  const isActive = (path) => location.pathname === path

  const handleLogout = () => {
    window.location.href = 'http://localhost:8080/logout'
  }

  return (
    <>
      {/* Mobile Toggle */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="md:hidden fixed top-4 left-4 z-50 p-2 rounded-lg bg-slate-100"
      >
        {isOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      {/* Sidebar */}
      <aside
        className={`${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        } md:translate-x-0 fixed md:static top-0 left-0 h-screen w-64 bg-gradient-to-b from-slate-900 to-slate-800 text-white transition-transform duration-300 z-40 flex flex-col`}
      >
        {/* Logo */}
        <div className="p-6 border-b border-slate-700">
          <h1 className="text-2xl font-bold text-white">RC Hub</h1>
          <p className="text-slate-400 text-sm mt-1">Operations</p>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4">
          <ul className="space-y-2">
            {menuItems.map((item) => (
              <li key={item.path}>
                <Link
                  to={item.path}
                  onClick={() => setIsOpen(false)}
                  className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    isActive(item.path)
                      ? 'bg-blue-600 text-white'
                      : 'text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  <item.icon size={20} />
                  <span>{item.label}</span>
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        {/* Logout */}
        <div className="p-4 border-t border-slate-700">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-4 py-3 rounded-lg text-slate-300 hover:bg-slate-700 transition-colors"
          >
            <LogOut size={20} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Mobile Overlay */}
      {isOpen && (
        <div
          onClick={() => setIsOpen(false)}
          className="fixed inset-0 bg-black/50 md:hidden z-30"
        />
      )}
    </>
  )
}
