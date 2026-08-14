import './MultiChoiceStats.css';

const DIFFICULTY_LABELS = { VERY_EASY: 'Rất dễ', EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó', VERY_HARD: 'Rất khó' };
const ATTENDANCE_LABELS = { NEVER: 'Không điểm danh', SOMETIMES: 'Thỉnh thoảng', OFTEN: 'Thường xuyên', STRICT: 'Rất gắt' };
const MATERIALS_LABELS = { YES: 'Được dùng tài liệu', NO: 'Đóng (không tài liệu)', DEPENDS: 'Tùy kỳ thi' };
const RECOMMEND_LABELS = { YES: 'Có recommend', NO: 'Không recommend', MAYBE: 'Không ý kiến' };
const WORKLOAD_LABELS = { LIGHT: 'Ít bài tập', MODERATE: 'Vừa phải', HEAVY: 'Nhiều bài tập', VERY_HEAVY: 'Rất nặng' };

export default function MultiChoiceStats({ stats }) {
  if (!stats) return null;

  const getTopChoice = (statGroup, labels) => {
    let topKey = '';
    let topVal = -1;
    let total = 0;

    Object.entries(statGroup).forEach(([key, val]) => {
      total += val;
      if (val > topVal) {
        topVal = val;
        topKey = key;
      }
    });

    if (total === 0) return { label: 'Chưa có dữ liệu', percentage: 0 };
    const percentage = Math.round((topVal / total) * 100);

    return {
      label: labels[topKey] || topKey,
      percentage,
      total
    };
  };

  const difficulty = getTopChoice(stats.difficulty || {}, DIFFICULTY_LABELS);
  const attendance = getTopChoice(stats.attendance || {}, ATTENDANCE_LABELS);
  const materials = getTopChoice(stats.materialsAllowed || {}, MATERIALS_LABELS);
  const recommend = getTopChoice(stats.wouldRecommend || {}, RECOMMEND_LABELS);
  const workload = getTopChoice(stats.workload || {}, WORKLOAD_LABELS);

  return (
    <div className="mc-stats card animate-fade-in">
      <h3 className="mc-stats-title">📊 Tổng quan ý kiến sinh viên</h3>

      <div className="mc-stats-list">
        <StatRow title="Độ khó môn học" stat={difficulty} color="var(--color-accent)" />
        <StatRow title="Điểm danh" stat={attendance} color="var(--color-warning)" />
        <StatRow title="Sử dụng tài liệu" stat={materials} color="var(--color-info)" />
        <StatRow title="Mức độ recommend" stat={recommend} color="var(--color-success)" />
        <StatRow title="Khối lượng bài tập" stat={workload} color="var(--color-danger)" />
      </div>
    </div>
  );
}

function StatRow({ title, stat, color }) {
  return (
    <div className="stat-row">
      <div className="stat-row-info">
        <span className="stat-row-title">{title}</span>
        <span className="stat-row-val">
          <strong>{stat.label}</strong> ({stat.percentage}%)
        </span>
      </div>
      <div className="rating-bar-track">
        <div
          className="rating-bar-fill animate-shimmer"
          style={{
            width: `${stat.percentage}%`,
            backgroundColor: color,
            boxShadow: `0 0 8px ${color}66`
          }}
        />
      </div>
    </div>
  );
}
