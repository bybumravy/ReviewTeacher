import { useState, useRef } from 'react';
import { FiUpload } from 'react-icons/fi';
import { importTeachersCsv } from '../../api/adminApi';
import toast from 'react-hot-toast';
import './AdminRosterImportPage.css';

export default function AdminRosterImportPage() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const fileInputRef = useRef(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      toast.error('Vui lòng chọn một file CSV');
      return;
    }
    setSubmitting(true);
    setResult(null);
    try {
      const data = await importTeachersCsv(file);
      setResult(data);
      toast.success(data.message || 'Import thành công');
      setFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (err) {
      toast.error(err.message || 'Không thể import file CSV');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="admin-import-page page container">
      <div className="page-header">
        <h1 className="page-title">Import danh sách giảng viên</h1>
        <p className="page-subtitle">
          Upload file CSV với các cột: full_name, title, faculty, department. Giảng viên trùng
          (tên + khoa) với bản ghi đã có sẽ được cập nhật, không tạo bản ghi trùng lặp.
        </p>
      </div>

      <form className="admin-import-form card" onSubmit={handleSubmit}>
        <div className="input-group">
          <label htmlFor="csv-file">File CSV</label>
          <input
            id="csv-file"
            ref={fileInputRef}
            type="file"
            accept=".csv"
            onChange={(e) => setFile(e.target.files?.[0] || null)}
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          <FiUpload /> {submitting ? 'Đang import...' : 'Import'}
        </button>
      </form>

      {result && (
        <div className="admin-import-result card">
          <div className="admin-import-summary">
            <div className="import-stat">
              <strong>{result.importedCount ?? 0}</strong>
              <span>Giảng viên mới</span>
            </div>
            <div className="import-stat">
              <strong>{result.updatedCount ?? 0}</strong>
              <span>Đã cập nhật</span>
            </div>
            <div className="import-stat">
              <strong>{result.failedRows?.length ?? 0}</strong>
              <span>Dòng lỗi</span>
            </div>
          </div>

          {result.failedRows?.length > 0 && (
            <table className="admin-import-failed-table">
              <thead>
                <tr><th>Dòng</th><th>Lý do</th></tr>
              </thead>
              <tbody>
                {result.failedRows.map((f, idx) => (
                  <tr key={idx}><td>{f.row}</td><td>{f.reason}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
