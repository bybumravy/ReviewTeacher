import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { FiSearch, FiMenu, FiX } from 'react-icons/fi';
import { HiAcademicCap } from 'react-icons/hi2';
import CreditBadge from '../review/CreditBadge';
import './Navbar.css';

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="container navbar-inner">
        <Link to="/" className="navbar-brand">
          <HiAcademicCap className="navbar-logo-icon" />
          <span className="navbar-logo-text">Uni<span className="navbar-logo-accent">Review</span></span>
        </Link>

        <div className={`navbar-links ${menuOpen ? 'open' : ''}`}>
          <Link
            to="/"
            className={`navbar-link ${isActive('/') ? 'active' : ''}`}
            onClick={() => setMenuOpen(false)}
          >
            Trang chủ
          </Link>
          <Link
            to="/teachers"
            className={`navbar-link ${isActive('/teachers') ? 'active' : ''}`}
            onClick={() => setMenuOpen(false)}
          >
            Giảng viên
          </Link>
          <Link
            to="/write-review"
            className={`navbar-link ${isActive('/write-review') ? 'active' : ''}`}
            onClick={() => setMenuOpen(false)}
          >
            Viết Review
          </Link>
        </div>

        <div className="navbar-actions">
          <CreditBadge />
          <button
            className="navbar-menu-toggle"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            {menuOpen ? <FiX /> : <FiMenu />}
          </button>
        </div>
      </div>
    </nav>
  );
}
