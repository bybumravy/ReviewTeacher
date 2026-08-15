package com.unireview.service;

import com.unireview.entity.Teacher;
import com.unireview.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CsvImportServiceTest {

    private TeacherRepository teacherRepository;
    private CsvImportService csvImportService;

    @BeforeEach
    void setUp() {
        teacherRepository = mock(TeacherRepository.class);
        csvImportService = new CsvImportService(teacherRepository);
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "teachers.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importTeachersFromCsv_newRow_isInserted() throws Exception {
        when(teacherRepository.findByFullNameIgnoreCaseAndFacultyIgnoreCase(anyString(), anyString()))
                .thenReturn(Optional.empty());
        String csv = "full_name,title,faculty,department\nNguyen Van A,TS,CNTT,KHMT\n";

        CsvImportService.ImportResult result = csvImportService.importTeachersFromCsv(csvFile(csv));

        assertEquals(1, result.importedCount());
        assertEquals(0, result.updatedCount());
        assertTrue(result.failedRows().isEmpty());
        verify(teacherRepository).save(argThat(t -> "Nguyen Van A".equals(t.getFullName())));
    }

    @Test
    void importTeachersFromCsv_matchingExistingTeacher_updatesInPlace() throws Exception {
        Teacher existing = Teacher.builder()
                .id(5L).fullName("Nguyen Van A").faculty("CNTT")
                .avgRating(BigDecimal.valueOf(4.5)).totalReviews(10)
                .build();
        when(teacherRepository.findByFullNameIgnoreCaseAndFacultyIgnoreCase("Nguyen Van A", "CNTT"))
                .thenReturn(Optional.of(existing));
        String csv = "full_name,title,faculty,department\nNguyen Van A,PGS.TS,CNTT,Ky thuat phan mem\n";

        CsvImportService.ImportResult result = csvImportService.importTeachersFromCsv(csvFile(csv));

        assertEquals(0, result.importedCount());
        assertEquals(1, result.updatedCount());
        assertEquals("PGS.TS", existing.getTitle());
        assertEquals("Ky thuat phan mem", existing.getDepartment());
        // Rating/review history untouched by import
        assertEquals(BigDecimal.valueOf(4.5), existing.getAvgRating());
        assertEquals(10, existing.getTotalReviews());
        verify(teacherRepository).save(existing);
    }

    @Test
    void importTeachersFromCsv_missingFullName_reportedAsFailedRow() throws Exception {
        String csv = "full_name,title,faculty,department\n,TS,CNTT,KHMT\n";

        CsvImportService.ImportResult result = csvImportService.importTeachersFromCsv(csvFile(csv));

        assertEquals(0, result.importedCount());
        assertEquals(1, result.failedRows().size());
        assertTrue(result.failedRows().get(0).reason().contains("full_name"));
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void importTeachersFromCsv_missingFaculty_reportedAsFailedRow() throws Exception {
        String csv = "full_name,title,faculty,department\nNguyen Van A,TS,,KHMT\n";

        CsvImportService.ImportResult result = csvImportService.importTeachersFromCsv(csvFile(csv));

        assertEquals(1, result.failedRows().size());
        assertTrue(result.failedRows().get(0).reason().contains("faculty"));
    }

    @Test
    void importTeachersFromCsv_mixedBatch_reportsCorrectCounts() throws Exception {
        when(teacherRepository.findByFullNameIgnoreCaseAndFacultyIgnoreCase("Nguyen Van A", "CNTT"))
                .thenReturn(Optional.of(Teacher.builder().id(1L).fullName("Nguyen Van A").faculty("CNTT").build()));
        when(teacherRepository.findByFullNameIgnoreCaseAndFacultyIgnoreCase("Tran Thi B", "CNTT"))
                .thenReturn(Optional.empty());
        String csv = "full_name,title,faculty,department\n"
                + "Nguyen Van A,TS,CNTT,KHMT\n"
                + "Tran Thi B,ThS,CNTT,KTPM\n"
                + ",TS,CNTT,KHMT\n";

        CsvImportService.ImportResult result = csvImportService.importTeachersFromCsv(csvFile(csv));

        assertEquals(1, result.importedCount());
        assertEquals(1, result.updatedCount());
        assertEquals(1, result.failedRows().size());
    }
}
