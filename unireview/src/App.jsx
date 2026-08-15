import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { GateProvider } from './hooks/useGate';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import TeacherListPage from './pages/TeacherListPage';
import TeacherDetailPage from './pages/TeacherDetailPage';
import WriteReviewPage from './pages/WriteReviewPage';
import AdminLoginPage from './pages/admin/AdminLoginPage';
import AdminModerationQueuePage from './pages/admin/AdminModerationQueuePage';
import AdminRosterImportPage from './pages/admin/AdminRosterImportPage';
import ProtectedAdminRoute from './components/admin/ProtectedAdminRoute';

export default function App() {
  return (
    <GateProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="teachers" element={<TeacherListPage />} />
            <Route path="teachers/:id" element={<TeacherDetailPage />} />
            <Route path="write-review" element={<WriteReviewPage />} />
          </Route>
          <Route path="admin/login" element={<AdminLoginPage />} />
          <Route element={<ProtectedAdminRoute />}>
            <Route path="admin/queue" element={<AdminModerationQueuePage />} />
            <Route path="admin/import" element={<AdminRosterImportPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
      <Toaster
        position="bottom-right"
        toastOptions={{
          style: {
            background: 'var(--color-bg-card)',
            color: 'var(--color-text-primary)',
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            fontFamily: 'var(--font-family)',
          },
        }}
      />
    </GateProvider>
  );
}
