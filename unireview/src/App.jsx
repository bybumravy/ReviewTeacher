import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { GateProvider } from './hooks/useGate';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import TeacherListPage from './pages/TeacherListPage';
import TeacherDetailPage from './pages/TeacherDetailPage';
import WriteReviewPage from './pages/WriteReviewPage';

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
