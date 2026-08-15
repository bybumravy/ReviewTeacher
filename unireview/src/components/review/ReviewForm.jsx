import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiSend, FiAlertCircle } from 'react-icons/fi';
import StarRating from './StarRating';
import { setReviewerToken } from '../../utils/cookie';
import { useGate } from '../../hooks/useGate';
import { submitReview } from '../../api/reviewApi';
import { executeReCaptcha } from '../../utils/recaptcha';
import toast from 'react-hot-toast';
import './ReviewForm.css';

const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY || '';

const DIFFICULTY_OPTIONS = [
  { value: 'VERY_EASY', label: 'Rất dễ' },
  { value: 'EASY', label: 'Dễ' },
  { value: 'MEDIUM', label: 'Trung bình' },
  { value: 'HARD', label: 'Khó' },
  { value: 'VERY_HARD', label: 'Rất khó' },
];

const ATTENDANCE_OPTIONS = [
  { value: 'NEVER', label: 'Không bao giờ' },
  { value: 'SOMETIMES', label: 'Thỉnh thoảng' },
  { value: 'OFTEN', label: 'Thường xuyên' },
  { value: 'STRICT', label: 'Rất gắt' },
];

const MATERIALS_OPTIONS = [
  { value: 'YES', label: 'Có' },
  { value: 'NO', label: 'Không' },
  { value: 'DEPENDS', label: 'Tùy kỳ thi' },
];

const RECOMMEND_OPTIONS = [
  { value: 'YES', label: '👍 Có' },
  { value: 'NO', label: '👎 Không' },
  { value: 'MAYBE', label: '🤔 Tùy' },
];

const WORKLOAD_OPTIONS = [
  { value: 'LIGHT', label: 'Ít' },
  { value: 'MODERATE', label: 'Vừa phải' },
  { value: 'HEAVY', label: 'Nhiều' },
  { value: 'VERY_HEAVY', label: 'Rất nhiều' },
];

const SEMESTERS = [
  'HK1 2025-2026', 'HK2 2024-2025', 'HK1 2024-2025',
  'HK2 2023-2024', 'HK1 2023-2024', 'Trước đó',
];

export default function ReviewForm({ teacher, onSuccess }) {
  const navigate = useNavigate();
  const { refresh } = useGate();

  const [form, setForm] = useState({
    ratingOverall: 0,
    ratingTeaching: 0,
    ratingGrading: 0,
    ratingPersonality: 0,
    difficulty: '',
    attendance: '',
    materialsAllowed: '',
    wouldRecommend: '',
    workload: '',
    content: '',
    semester: '',
  });

  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const updateField = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: null }));
    }
  };

  const validate = () => {
    const errs = {};
    if (!form.ratingOverall) errs.ratingOverall = 'Vui lòng chọn rating tổng quan';
    if (!form.ratingTeaching) errs.ratingTeaching = 'Vui lòng chọn rating giảng dạy';
    if (!form.ratingGrading) errs.ratingGrading = 'Vui lòng chọn rating chấm điểm';
    if (!form.ratingPersonality) errs.ratingPersonality = 'Vui lòng chọn rating tính cách';
    if (!form.difficulty) errs.difficulty = 'Vui lòng chọn độ khó';
    if (!form.attendance) errs.attendance = 'Vui lòng chọn điểm danh';
    if (!form.materialsAllowed) errs.materialsAllowed = 'Vui lòng chọn';
    if (!form.wouldRecommend) errs.wouldRecommend = 'Vui lòng chọn';
    if (!form.workload) errs.workload = 'Vui lòng chọn';
    if (!form.semester) errs.semester = 'Vui lòng chọn học kỳ';
    if (form.content.length < 50) errs.content = `Cần ít nhất 50 ký tự (hiện tại: ${form.content.length})`;
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) {
      toast.error('Vui lòng điền đầy đủ thông tin');
      return;
    }

    setSubmitting(true);
    try {
      const captchaToken = await executeReCaptcha(RECAPTCHA_SITE_KEY, 'submit_review');

      const data = await submitReview({
        ...form,
        teacherId: teacher.id,
        captchaToken,
      });

      if (data.reviewerToken) {
        setReviewerToken(data.reviewerToken);
      }

      await refresh();

      if (data.status === 'FLAGGED') {
        toast('Review đang được kiểm duyệt. Credit sẽ được cộng sau khi xác nhận.', { icon: '⏳', duration: 5000 });
      } else {
        toast.success(data.message || 'Review đã được đăng! +1 credit 🎉', { duration: 4000 });
      }
      onSuccess?.();
    } catch (err) {
      toast.error(err.message || 'Không thể gửi review. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="review-form" onSubmit={handleSubmit}>
      {/* Star Ratings */}
      <div className="review-form-section">
        <h3 className="review-form-section-title">⭐ Đánh giá</h3>
        <div className="review-form-ratings">
          <div className={errors.ratingOverall ? 'has-error' : ''}>
            <StarRating label="Tổng quan *" value={form.ratingOverall} onChange={(v) => updateField('ratingOverall', v)} size={28} />
            {errors.ratingOverall && <span className="error-text">{errors.ratingOverall}</span>}
          </div>
          <div className={errors.ratingTeaching ? 'has-error' : ''}>
            <StarRating label="Giảng dạy *" value={form.ratingTeaching} onChange={(v) => updateField('ratingTeaching', v)} />
            {errors.ratingTeaching && <span className="error-text">{errors.ratingTeaching}</span>}
          </div>
          <div className={errors.ratingGrading ? 'has-error' : ''}>
            <StarRating label="Chấm điểm *" value={form.ratingGrading} onChange={(v) => updateField('ratingGrading', v)} />
            {errors.ratingGrading && <span className="error-text">{errors.ratingGrading}</span>}
          </div>
          <div className={errors.ratingPersonality ? 'has-error' : ''}>
            <StarRating label="Tính cách *" value={form.ratingPersonality} onChange={(v) => updateField('ratingPersonality', v)} />
            {errors.ratingPersonality && <span className="error-text">{errors.ratingPersonality}</span>}
          </div>
        </div>
      </div>

      {/* Multiple Choice */}
      <div className="review-form-section">
        <h3 className="review-form-section-title">📋 Thông tin môn học</h3>

        <div className="mc-grid">
          <MCQuestion label="Độ khó môn học *" options={DIFFICULTY_OPTIONS} value={form.difficulty}
            onChange={(v) => updateField('difficulty', v)} error={errors.difficulty} />
          <MCQuestion label="Điểm danh *" options={ATTENDANCE_OPTIONS} value={form.attendance}
            onChange={(v) => updateField('attendance', v)} error={errors.attendance} />
          <MCQuestion label="Tài liệu khi thi *" options={MATERIALS_OPTIONS} value={form.materialsAllowed}
            onChange={(v) => updateField('materialsAllowed', v)} error={errors.materialsAllowed} />
          <MCQuestion label="Bạn có recommend? *" options={RECOMMEND_OPTIONS} value={form.wouldRecommend}
            onChange={(v) => updateField('wouldRecommend', v)} error={errors.wouldRecommend} />
          <MCQuestion label="Khối lượng bài tập *" options={WORKLOAD_OPTIONS} value={form.workload}
            onChange={(v) => updateField('workload', v)} error={errors.workload} />
        </div>
      </div>

      {/* Semester */}
      <div className="review-form-section">
        <div className="input-group">
          <label>📅 Học kỳ đã học *</label>
          <select className={`select ${errors.semester ? 'input-error' : ''}`}
            value={form.semester} onChange={(e) => updateField('semester', e.target.value)}>
            <option value="">Chọn học kỳ...</option>
            {SEMESTERS.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          {errors.semester && <span className="error-text">{errors.semester}</span>}
        </div>
      </div>

      {/* Text Review */}
      <div className="review-form-section">
        <div className="input-group">
          <label>📝 Nhận xét chi tiết * <span className="char-count">({form.content.length}/50 ký tự tối thiểu)</span></label>
          <textarea
            className={`textarea ${errors.content ? 'input-error' : ''}`}
            value={form.content}
            onChange={(e) => updateField('content', e.target.value)}
            placeholder="Chia sẻ trải nghiệm của bạn về giảng viên này. Cách giảng dạy, thi cử, tính cách, lời khuyên cho sinh viên sau..."
            rows={5}
          />
          {errors.content && <span className="error-text"><FiAlertCircle /> {errors.content}</span>}
        </div>
      </div>

      {/* Submit */}
      <button type="submit" className="btn btn-primary btn-lg review-submit-btn" disabled={submitting}>
        {submitting ? (
          <>Đang kiểm tra AI... ⏳</>
        ) : (
          <><FiSend /> Gửi Review</>
        )}
      </button>
    </form>
  );
}

function MCQuestion({ label, options, value, onChange, error }) {
  return (
    <div className={`mc-question ${error ? 'has-error' : ''}`}>
      <span className="mc-label">{label}</span>
      <div className="mc-options">
        {options.map(opt => (
          <button
            key={opt.value}
            type="button"
            className={`mc-option ${value === opt.value ? 'selected' : ''}`}
            onClick={() => onChange(opt.value)}
          >
            {opt.label}
          </button>
        ))}
      </div>
      {error && <span className="error-text">{error}</span>}
    </div>
  );
}
