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
    public int importTeachersFromCsv(MultipartFile file) throws Exception {
        List<Teacher> teachers = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            for (CSVRecord record : csvParser) {
                String fullName = record.get("full_name");
                String title = record.isMapped("title") ? record.get("title") : "";
                String faculty = record.get("faculty");
                String department = record.isMapped("department") ? record.get("department") : "";

                if (fullName != null && !fullName.isBlank() && faculty != null && !faculty.isBlank()) {
                    Teacher teacher = Teacher.builder()
                            .fullName(fullName)
                            .title(title)
                            .faculty(faculty)
                            .department(department)
                            .build();
                    teachers.add(teacher);
                }
            }
        }

        if (!teachers.isEmpty()) {
            teacherRepository.saveAll(teachers);
        }

        return teachers.size();
    }
}
