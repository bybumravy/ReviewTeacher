package com.unireview.service;

import com.unireview.dto.response.PagedResponse;
import com.unireview.entity.Teacher;
import com.unireview.repository.ReviewRepository;
import com.unireview.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TeacherServiceTest {

    private TeacherRepository teacherRepository;
    private ReviewRepository reviewRepository;
    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        teacherRepository = mock(TeacherRepository.class);
        reviewRepository = mock(ReviewRepository.class);
        teacherService = new TeacherService(teacherRepository, reviewRepository);
    }

    private Page<Teacher> pageOf(Teacher... teachers) {
        return new PageImpl<>(List.of(teachers), PageRequest.of(0, 12), teachers.length);
    }

    @Test
    void getTeachers_sortByRating_sortsOnAvgRatingField() {
        Teacher t = Teacher.builder().id(1L).fullName("A").faculty("CNTT").build();
        when(teacherRepository.findByFilters(any(), any(), any(), any())).thenReturn(pageOf(t));

        teacherService.getTeachers(null, null, null, "rating", "desc", 0, 12);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(teacherRepository).findByFilters(any(), any(), any(), captor.capture());
        Sort.Order order = captor.getValue().getSort().iterator().next();
        assertEquals("avgRating", order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void getTeachers_sortByReviews_sortsOnTotalReviewsField() {
        when(teacherRepository.findByFilters(any(), any(), any(), any())).thenReturn(pageOf());

        teacherService.getTeachers(null, null, null, "reviews", "asc", 0, 12);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(teacherRepository).findByFilters(any(), any(), any(), captor.capture());
        Sort.Order order = captor.getValue().getSort().iterator().next();
        assertEquals("totalReviews", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void getTeachers_defaultSort_fallsBackToFullName() {
        when(teacherRepository.findByFilters(any(), any(), any(), any())).thenReturn(pageOf());

        teacherService.getTeachers(null, null, null, null, null, 0, 12);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(teacherRepository).findByFilters(any(), any(), any(), captor.capture());
        Sort.Order order = captor.getValue().getSort().iterator().next();
        assertEquals("fullName", order.getProperty());
    }

    @Test
    void getTeachers_forwardsSearchFacultyAndMinRatingToRepository() {
        when(teacherRepository.findByFilters(any(), any(), any(), any())).thenReturn(pageOf());

        teacherService.getTeachers("nguyen", "CNTT", BigDecimal.valueOf(4.0), "name", "asc", 0, 12);

        verify(teacherRepository).findByFilters(eq("nguyen"), eq("CNTT"), eq(BigDecimal.valueOf(4.0)), any());
    }

    @Test
    void getTeachers_surfacesPaginationMetadata() {
        Teacher t1 = Teacher.builder().id(1L).fullName("A").faculty("CNTT").build();
        Teacher t2 = Teacher.builder().id(2L).fullName("B").faculty("CNTT").build();
        Page<Teacher> page = new PageImpl<>(List.of(t1, t2), PageRequest.of(1, 2), 5);
        when(teacherRepository.findByFilters(any(), any(), any(), any())).thenReturn(page);

        PagedResponse<?> response = teacherService.getTeachers(null, null, null, "name", "asc", 1, 2);

        assertEquals(2, response.getContent().size());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getSize());
        assertEquals(5, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertFalse(response.isLast());
    }
}
