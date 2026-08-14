import { HiAcademicCap } from 'react-icons/hi2';
import { FiGithub } from 'react-icons/fi';
import './Footer.css';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div className="footer-brand">
          <HiAcademicCap className="footer-icon" />
          <span>UniReview</span>
        </div>
        <p className="footer-text">
          Nền tảng đánh giá giảng viên ẩn danh — Bởi sinh viên, cho sinh viên.
        </p>
        <p className="footer-copyright">
          © {new Date().getFullYear()} UniReview. Made with 💜
        </p>
      </div>
    </footer>
  );
}
