import { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import Sidebar from "./Sidebar";
import Header from "./Header";
import { useAuth } from "../contexts/AuthContext";

export default function Layout({ children }) {
  const [menuOpen, setMenuOpen] = useState(false);
  const { pathname } = useLocation();
  const { usuario } = useAuth();

  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  return (
    <div className="flex h-screen bg-slate-50">
      <Sidebar isOpen={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Header usuario={usuario} onMenuClick={() => setMenuOpen(true)} />
        <main className="flex-1 overflow-auto p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
