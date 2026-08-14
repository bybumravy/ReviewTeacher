export const MOCK_TEACHERS = [
  {
    id: 1,
    fullName: "Nguyễn Văn A",
    title: "TS",
    faculty: "Công nghệ thông tin",
    department: "Khoa học máy tính",
    avatarUrl: null,
    avgRating: 4.6,
    totalReviews: 3,
    ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 1, 5: 2 },
    multipleChoiceStats: {
      difficulty: { VERY_EASY: 0, EASY: 1, MEDIUM: 2, HARD: 0, VERY_HARD: 0 },
      attendance: { NEVER: 1, SOMETIMES: 2, OFTEN: 0, STRICT: 0 },
      materialsAllowed: { YES: 3, NO: 0, DEPENDS: 0 },
      wouldRecommend: { YES: 3, NO: 0, MAYBE: 0 },
      workload: { LIGHT: 2, MODERATE: 1, HEAVY: 0, VERY_HEAVY: 0 }
    }
  },
  {
    id: 2,
    fullName: "Trần Thị B",
    title: "ThS",
    faculty: "Công nghệ thông tin",
    department: "Kỹ thuật phần mềm",
    avatarUrl: null,
    avgRating: 3.8,
    totalReviews: 2,
    ratingDistribution: { 1: 0, 2: 0, 3: 1, 4: 1, 5: 0 },
    multipleChoiceStats: {
      difficulty: { VERY_EASY: 0, EASY: 0, MEDIUM: 1, HARD: 1, VERY_HARD: 0 },
      attendance: { NEVER: 0, SOMETIMES: 0, OFTEN: 1, STRICT: 1 },
      materialsAllowed: { YES: 0, NO: 2, DEPENDS: 0 },
      wouldRecommend: { YES: 1, NO: 1, MAYBE: 0 },
      workload: { LIGHT: 0, MODERATE: 1, HEAVY: 1, VERY_HEAVY: 0 }
    }
  },
  {
    id: 3,
    fullName: "Phạm Minh C",
    title: "PGS.TS",
    faculty: "Điện tử viễn thông",
    department: "Hệ thống viễn thông",
    avatarUrl: null,
    avgRating: 4.2,
    totalReviews: 2,
    ratingDistribution: { 1: 0, 2: 0, 3: 0, 4: 2, 5: 0 },
    multipleChoiceStats: {
      difficulty: { VERY_EASY: 0, EASY: 1, MEDIUM: 1, HARD: 0, VERY_HARD: 0 },
      attendance: { NEVER: 0, SOMETIMES: 1, OFTEN: 1, STRICT: 0 },
      materialsAllowed: { YES: 1, NO: 0, DEPENDS: 1 },
      wouldRecommend: { YES: 2, NO: 0, MAYBE: 0 },
      workload: { LIGHT: 1, MODERATE: 1, HEAVY: 0, VERY_HEAVY: 0 }
    }
  },
  {
    id: 4,
    fullName: "Lê Hoàng D",
    title: "TS",
    faculty: "Kinh tế & Quản lý",
    department: "Quản trị kinh doanh",
    avatarUrl: null,
    avgRating: 2.5,
    totalReviews: 2,
    ratingDistribution: { 1: 1, 2: 0, 3: 1, 4: 0, 5: 0 },
    multipleChoiceStats: {
      difficulty: { VERY_EASY: 0, EASY: 0, MEDIUM: 0, HARD: 1, VERY_HARD: 1 },
      attendance: { NEVER: 0, SOMETIMES: 0, OFTEN: 0, STRICT: 2 },
      materialsAllowed: { YES: 0, NO: 2, DEPENDS: 0 },
      wouldRecommend: { YES: 0, NO: 2, MAYBE: 0 },
      workload: { LIGHT: 0, MODERATE: 0, HEAVY: 1, VERY_HEAVY: 1 }
    }
  }
];

export const MOCK_REVIEWS = {
  1: [
    {
      id: 101,
      teacherId: 1,
      ratingOverall: 5,
      ratingTeaching: 5,
      ratingGrading: 5,
      ratingPersonality: 5,
      difficulty: "EASY",
      attendance: "SOMETIMES",
      materialsAllowed: "YES",
      wouldRecommend: "YES",
      workload: "LIGHT",
      content: "Thầy A dạy siêu nhiệt tình và dễ thương luôn. Bài thi cuối kỳ bám sát slide bài giảng. Điểm danh thỉnh thoảng thôi nhưng đi học đầy đủ sẽ được cộng điểm chuyên cần.",
      semester: "HK1 2025-2026",
      upvoteCount: 15,
      downvoteCount: 1,
      status: "APPROVED"
    },
    {
      id: 102,
      teacherId: 1,
      ratingOverall: 5,
      ratingTeaching: 5,
      ratingGrading: 4,
      ratingPersonality: 5,
      difficulty: "MEDIUM",
      attendance: "NEVER",
      materialsAllowed: "YES",
      wouldRecommend: "YES",
      workload: "LIGHT",
      content: "Thầy giải thích các khái niệm cực kỳ dễ hiểu. Không điểm danh, đề thi cho phép mang tài liệu thoải mái. Rất đáng để đăng ký môn của thầy.",
      semester: "HK2 2024-2025",
      upvoteCount: 8,
      downvoteCount: 0,
      status: "APPROVED"
    },
    {
      id: 103,
      teacherId: 1,
      ratingOverall: 4,
      ratingTeaching: 4,
      ratingGrading: 4,
      ratingPersonality: 4,
      difficulty: "MEDIUM",
      attendance: "SOMETIMES",
      materialsAllowed: "YES",
      wouldRecommend: "YES",
      workload: "MODERATE",
      content: "Môn này lượng kiến thức tương đối ổn. Thầy dạy hay, chấm điểm cũng công bằng và không quá khắt khe. Nên học nhé các bạn.",
      semester: "HK1 2024-2025",
      upvoteCount: 3,
      downvoteCount: 0,
      status: "APPROVED"
    }
  ],
  2: [
    {
      id: 201,
      teacherId: 2,
      ratingOverall: 4,
      ratingTeaching: 4,
      ratingGrading: 3,
      ratingPersonality: 4,
      difficulty: "MEDIUM",
      attendance: "OFTEN",
      materialsAllowed: "NO",
      wouldRecommend: "YES",
      workload: "MODERATE",
      content: "Cô B giảng dạy rất bài bản, nhiều ví dụ thực tế. Tuy nhiên chấm bài thực hành có phần hơi chặt tay, cần làm kỹ report mới được điểm cao.",
      semester: "HK2 2024-2025",
      upvoteCount: 9,
      downvoteCount: 2,
      status: "APPROVED"
    },
    {
      id: 202,
      teacherId: 2,
      ratingOverall: 3,
      ratingTeaching: 3,
      ratingGrading: 3,
      ratingPersonality: 4,
      difficulty: "HARD",
      attendance: "STRICT",
      materialsAllowed: "NO",
      wouldRecommend: "NO",
      workload: "HEAVY",
      content: "Cô điểm danh rất gắt và hay kiểm tra bài cũ đầu giờ. Khối lượng bài tập lớn, deadline liên tục. Đề thi đóng không được xem tài liệu.",
      semester: "HK1 2024-2025",
      upvoteCount: 12,
      downvoteCount: 1,
      status: "APPROVED"
    }
  ],
  3: [
    {
      id: 301,
      teacherId: 3,
      ratingOverall: 4,
      ratingTeaching: 4,
      ratingGrading: 4,
      ratingPersonality: 4,
      difficulty: "EASY",
      attendance: "SOMETIMES",
      materialsAllowed: "YES",
      wouldRecommend: "YES",
      workload: "LIGHT",
      content: "Thầy C vui tính, nhiệt tình, slide rõ ràng. Thi giữa kỳ được mở slide, cuối kỳ đề trắc nghiệm cơ bản. Đi học đầy đủ là auto điểm cao.",
      semester: "HK1 2025-2026",
      upvoteCount: 5,
      downvoteCount: 0,
      status: "APPROVED"
    },
    {
      id: 302,
      teacherId: 3,
      ratingOverall: 4,
      ratingTeaching: 4,
      ratingGrading: 4,
      ratingPersonality: 4,
      difficulty: "MEDIUM",
      attendance: "OFTEN",
      materialsAllowed: "DEPENDS",
      wouldRecommend: "YES",
      workload: "MODERATE",
      content: "Thầy dạy kiến thức nền tảng rất vững. Học thầy sẽ hiểu bản chất vấn đề. Có điểm danh bằng giấy ngẫu nhiên các buổi học.",
      semester: "HK1 2024-2025",
      upvoteCount: 4,
      downvoteCount: 0,
      status: "APPROVED"
    }
  ],
  4: [
    {
      id: 401,
      teacherId: 4,
      ratingOverall: 1,
      ratingTeaching: 2,
      ratingGrading: 1,
      ratingPersonality: 2,
      difficulty: "VERY_HARD",
      attendance: "STRICT",
      materialsAllowed: "NO",
      wouldRecommend: "NO",
      workload: "VERY_HEAVY",
      content: "Né gấp nha mọi người ơi. Thầy D chấm điểm cực kỳ khắt khe, đề thi siêu khó và lắt léo, lớp trượt quá nửa. Điểm danh mỗi buổi không thiếu buổi nào.",
      semester: "HK2 2023-2024",
      upvoteCount: 22,
      downvoteCount: 0,
      status: "APPROVED"
    },
    {
      id: 402,
      teacherId: 4,
      ratingOverall: 3,
      ratingOverall: 4, // typo fix
      ratingOverall: 3,
      ratingTeaching: 3,
      ratingGrading: 2,
      ratingPersonality: 3,
      difficulty: "HARD",
      attendance: "STRICT",
      materialsAllowed: "NO",
      wouldRecommend: "NO",
      workload: "HEAVY",
      content: "Môn này học mệt mỏi lắm. Thầy dạy lý thuyết nhiều, bài tập lớn nặng. Thi cử chấm gắt, ít khi có điểm 9 10 môn của thầy.",
      semester: "HK1 2023-2024",
      upvoteCount: 7,
      downvoteCount: 1,
      status: "APPROVED"
    }
  ]
};
