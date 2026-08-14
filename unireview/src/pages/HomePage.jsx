import { Link } from 'react-router-dom';
import { FiEdit3, FiSearch, FiShield, FiZap, FiLock } from 'react-icons/fi';
import { HiAcademicCap } from 'react-icons/hi2';
import { useTeachers } from '../hooks/useTeachers';
import TeacherCard from '../components/teacher/TeacherCard';
import LoadingSpinner from '../components/common/LoadingSpinner';
import './HomePage.css';

export default function HomePage() {
  const { teachers, loading } = useTeachers({ sortBy: 'rating' });
  const topTeachers = teachers.slice(0, 3);

  return (
    <div className="home-page page">
      {/* Hero Section */}
      <section className="hero">
        <div className="container hero-inner">
          <div className="hero-content animate-slide-up">
            <div className="hero-badge badge badge-accent">
              <HiAcademicCap /> Nền tảng chia sẻ đánh giá giảng viên
            </div>
            <h1 className="hero-title">
              Xem nhận xét thực chất từ <span className="hero-title-accent">Sinh viên</span>
            </h1>
            <p className="hero-subtitle">
              UniReview giúp bạn lựa chọn giảng viên phù hợp với phong cách học tập của mình. Hoàn toàn ẩn danh, an toàn và công bằng.
            </p>
            <div className="hero-actions">
              <Link to="/teachers" className="btn btn-primary btn-lg">
                <FiSearch /> Tìm Giảng Viên
              </Link>
              <Link to="/write-review" className="btn btn-secondary btn-lg">
                <FiEdit3 /> Viết Nhận Xét
              </Link>
            </div>
          </div>

          <div className="hero-stats animate-fade-in">
            <div className="stat-card card">
              <span className="stat-num">100%</span>
              <span className="stat-label">Ẩn danh tuyệt đối</span>
            </div>
            <div className="stat-card card">
              <span className="stat-num">10+</span>
              <span className="stat-label">Khoa & Bộ môn</span>
            </div>
            <div className="stat-card card">
              <span className="stat-num">500+</span>
              <span className="stat-label">Lượt đánh giá</span>
            </div>
          </div>
        </div>
      </section>

      {/* Feature Section */}
      <section className="features container">
        <h2 className="section-title">UniReview Hoạt Động Thế Nào?</h2>
        <div className="features-grid">
          <div className="feature-item card">
            <div className="feature-icon"><FiShield /></div>
            <h3>Ẩn danh hoàn toàn</h3>
            <p>Không cần đăng nhập tài khoản. Chúng tôi lưu trữ cookie an toàn giúp bạn giữ credit và quyền mở khóa.</p>
          </div>
          <div className="feature-item card">
            <div className="feature-icon"><FiEdit3 /></div>
            <h3>Viết review nhận credit</h3>
            <p>Mỗi khi bạn chia sẻ 1 review chất lượng được AI phê duyệt, bạn nhận ngay 1 credit để xem giảng viên khác.</p>
          </div>
          <div className="feature-item card">
            <div className="feature-icon"><FiZap /></div>
            <h3>Dùng credit mở khóa</h3>
            <p>Dùng credit đã tích lũy để mở khóa xem toàn bộ nhận xét chi tiết của bất kỳ thầy cô nào bạn quan tâm.</p>
          </div>
        </div>
      </section>

      {/* Top Rated Section */}
      <section className="top-rated container">
        <div className="section-header">
          <h2 className="section-title">Giảng viên nổi bật</h2>
          <Link to="/teachers" className="view-all-link">Xem tất cả</Link>
        </div>

        {loading ? (
          <LoadingSpinner message="Đang tải giảng viên..." />
        ) : (
          <div className="grid grid-cols-3">
            {topTeachers.map(teacher => (
              <TeacherCard key={teacher.id} teacher={teacher} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
