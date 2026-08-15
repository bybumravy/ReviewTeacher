import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiLock, FiLogIn } from 'react-icons/fi';
import { login, setAdminToken } from '../../api/adminApi';
import toast from 'react-hot-toast';
import './AdminLoginPage.css';

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const data = await login(username, password);
      setAdminToken(data.token);
      toast.success(`Chào mừng, ${data.username}!`);
      navigate('/admin/queue');
    } catch (err) {
      toast.error(err.message || 'Đăng nhập thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="admin-login-page">
      <form className="admin-login-card card" onSubmit={handleSubmit}>
        <div className="admin-login-icon"><FiLock /></div>
        <h1 className="admin-login-title">Đăng nhập Admin</h1>
        <p className="admin-login-subtitle">UniReview — Trang quản trị</p>

        <div className="input-group">
          <label htmlFor="username">Tên đăng nhập</label>
          <input
            id="username"
            className="input"
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoFocus
          />
        </div>

        <div className="input-group">
          <label htmlFor="password">Mật khẩu</label>
          <input
            id="password"
            className="input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <button type="submit" className="btn btn-primary btn-lg" disabled={submitting}>
          <FiLogIn /> {submitting ? 'Đang đăng nhập...' : 'Đăng nhập'}
        </button>
      </form>
    </div>
  );
}
