import { useState, useEffect, useCallback } from 'react';
import { FiCheck, FiX, FiEyeOff, FiFlag } from 'react-icons/fi';
import ReviewCard from '../../components/review/ReviewCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  getFlaggedReviews, approveReview, rejectReview, hideReview,
  getReports, dismissReport,
} from '../../api/adminApi';
import toast from 'react-hot-toast';
import './AdminModerationQueuePage.css';

export default function AdminModerationQueuePage() {
  const [tab, setTab] = useState('flagged');
  const [flagged, setFlagged] = useState([]);
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadFlagged = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getFlaggedReviews();
      setFlagged(data.content || []);
    } catch (err) {
      toast.error(err.message || 'Không thể tải danh sách reviews bị gắn cờ');
    } finally {
      setLoading(false);
    }
  }, []);

  const loadReports = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getReports();
      setReports(data.content || []);
    } catch (err) {
      toast.error(err.message || 'Không thể tải danh sách báo cáo');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (tab === 'flagged') loadFlagged();
    else loadReports();
  }, [tab, loadFlagged, loadReports]);

  const handleApprove = async (id) => {
    try {
      await approveReview(id);
      toast.success('Đã duyệt review');
      loadFlagged();
    } catch (err) {
      toast.error(err.message || 'Không thể duyệt review');
    }
  };

  const handleReject = async (id) => {
    try {
      await rejectReview(id);
      toast.success('Đã từ chối review');
      loadFlagged();
    } catch (err) {
      toast.error(err.message || 'Không thể từ chối review');
    }
  };

  const handleHide = async (reviewId) => {
    try {
      await hideReview(reviewId);
      toast.success('Đã ẩn review');
      if (tab === 'flagged') loadFlagged();
      else loadReports();
    } catch (err) {
      toast.error(err.message || 'Không thể ẩn review');
    }
  };

  const handleDismissReport = async (reportId) => {
    try {
      await dismissReport(reportId);
      toast.success('Đã bỏ qua báo cáo');
      loadReports();
    } catch (err) {
      toast.error(err.message || 'Không thể bỏ qua báo cáo');
    }
  };

  return (
    <div className="admin-queue-page page container">
      <div className="page-header">
        <h1 className="page-title">Kiểm duyệt nội dung</h1>
        <p className="page-subtitle">Xử lý reviews bị AI gắn cờ và báo cáo từ sinh viên</p>
      </div>

      <div className="admin-tabs">
        <button className={`admin-tab ${tab === 'flagged' ? 'active' : ''}`} onClick={() => setTab('flagged')}>
          Reviews bị gắn cờ
        </button>
        <button className={`admin-tab ${tab === 'reports' ? 'active' : ''}`} onClick={() => setTab('reports')}>
          <FiFlag /> Báo cáo từ sinh viên
        </button>
      </div>

      {loading ? (
        <LoadingSpinner message="Đang tải..." />
      ) : tab === 'flagged' ? (
        flagged.length === 0 ? (
          <EmptyState title="Không có review nào cần duyệt" message="Hàng đợi kiểm duyệt hiện đang trống." />
        ) : (
          <div className="admin-review-list">
            {flagged.map((review) => (
              <div key={review.id} className="admin-review-item">
                <ReviewCard review={review} />
                <div className="admin-review-actions">
                  <button className="btn btn-primary btn-sm" onClick={() => handleApprove(review.id)}>
                    <FiCheck /> Duyệt
                  </button>
                  <button className="btn btn-secondary btn-sm" onClick={() => handleReject(review.id)}>
                    <FiX /> Từ chối
                  </button>
                </div>
              </div>
            ))}
          </div>
        )
      ) : reports.length === 0 ? (
        <EmptyState title="Không có báo cáo nào" message="Chưa có báo cáo nào đang chờ xử lý." />
      ) : (
        <div className="admin-review-list">
          {reports.map((report) => (
            <div key={report.id} className="admin-report-item card">
              <div className="admin-report-meta">
                <span className="badge badge-warning">{report.reason}</span>
                <span className="admin-report-date">{new Date(report.createdAt).toLocaleString('vi-VN')}</span>
              </div>
              {report.description && <p className="admin-report-description">{report.description}</p>}
              <p className="admin-report-review-content">"{report.reviewContent}"</p>
              <div className="admin-review-actions">
                <button className="btn btn-secondary btn-sm" onClick={() => handleDismissReport(report.id)}>
                  Bỏ qua
                </button>
                <button className="btn btn-danger btn-sm" onClick={() => handleHide(report.reviewId)}>
                  <FiEyeOff /> Ẩn review này
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
