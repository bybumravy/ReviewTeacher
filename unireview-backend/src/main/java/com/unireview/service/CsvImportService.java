package com.unireview.service;

import com.unireview.entity.Teacher;
import com.unireview.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final TeacherRepository teacherRepository;

    @Transactional
    public ImportResult importTeachersFromCsv(MultipartFile file) throws Exception {
        int importedCount = 0;
        int updatedCount = 0;
        List<FailedRow> failedRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            for (CSVRecord record : csvParser) {
                long rowNumber = record.getRecordNumber() + 1; // +1 to account for the header row

                String fullName = record.isMapped("full_name") ? record.get("full_name") : null;
                String faculty = record.isMapped("faculty") ? record.get("faculty") : null;
                String title = record.isMapped("title") ? record.get("title") : "";
                String department = record.isMapped("department") ? record.get("department") : "";
                String avatarUrl = record.isMapped("avatar_url") ? record.get("avatar_url") : null;

                if (fullName == null || fullName.isBlank()) {
                    failedRows.add(new FailedRow((int) rowNumber, "Thiếu họ tên (full_name)"));
                    continue;
                }
                if (faculty == null || faculty.isBlank()) {
                    failedRows.add(new FailedRow((int) rowNumber, "Thiếu khoa (faculty)"));
                    continue;
                }

                var existing = teacherRepository.findByFullNameIgnoreCaseAndFacultyIgnoreCase(fullName, faculty);
                if (existing.isPresent()) {
                    Teacher teacher = existing.get();
                    teacher.setTitle(title);
                    teacher.setDepartment(department);
                    if (avatarUrl != null && !avatarUrl.isBlank()) {
                        teacher.setAvatarUrl(avatarUrl);
                    }
                    teacherRepository.save(teacher);
                    updatedCount++;
                } else {
                    Teacher teacher = Teacher.builder()
                            .fullName(fullName)
                            .title(title)
                            .faculty(faculty)
                            .department(department)
                            .avatarUrl(avatarUrl)
                            .build();
                    teacherRepository.save(teacher);
                    importedCount++;
                }
            }
        }

        return new ImportResult(importedCount, updatedCount, failedRows);
    }

    public record FailedRow(int row, String reason) {}

    public record ImportResult(int importedCount, int updatedCount, List<FailedRow> failedRows) {}
}
