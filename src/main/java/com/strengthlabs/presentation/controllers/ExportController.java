package com.strengthlabs.presentation.controllers;

import com.strengthlabs.infrastructure.persistence.jpa.WorkoutExerciseJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaEntity;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutJpaRepository;
import com.strengthlabs.infrastructure.persistence.jpa.WorkoutSetJpaEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/export")
public class ExportController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] HEADERS = {
            "Date", "Workout", "Exercise", "Muscle Group",
            "Set #", "Weight (kg)", "Reps", "RPE", "Volume (kg)"
    };

    private final WorkoutJpaRepository workoutRepo;

    public ExportController(WorkoutJpaRepository workoutRepo) {
        this.workoutRepo = workoutRepo;
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(Authentication auth) {
        List<WorkoutJpaEntity> workouts = workoutRepo.findByUserIdOrderByDateDesc(
                UUID.fromString(auth.getName()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (List<String> row : buildRows(workouts)) {
            sb.append(row.stream().map(this::csvEscape).reduce((a, b) -> a + "," + b).orElse(""))
              .append("\n");
        }

        byte[] bytes = sb.toString().getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"strengthlabs_export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/xlsx")
    public ResponseEntity<byte[]> exportXlsx(Authentication auth) throws IOException {
        List<WorkoutJpaEntity> workouts = workoutRepo.findByUserIdOrderByDateDesc(
                UUID.fromString(auth.getName()));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Workouts");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            List<List<String>> rows = buildRows(workouts);
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> data = rows.get(r);
                for (int c = 0; c < data.size(); c++) {
                    Cell cell = row.createCell(c);
                    String val = data.get(c);
                    try {
                        cell.setCellValue(Double.parseDouble(val));
                    } catch (NumberFormatException e) {
                        cell.setCellValue(val);
                    }
                }
            }

            // Auto-size columns
            for (int c = 0; c < HEADERS.length; c++) sheet.autoSizeColumn(c);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"strengthlabs_export.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private List<List<String>> buildRows(List<WorkoutJpaEntity> workouts) {
        List<List<String>> rows = new ArrayList<>();
        for (WorkoutJpaEntity workout : workouts) {
            String dateStr = DATE_FMT.format(workout.getDate().atZone(ZoneOffset.UTC));
            for (WorkoutExerciseJpaEntity we : workout.getExercises()) {
                String exerciseName = we.getExercise().getName();
                String muscleGroup = we.getExercise().getMuscleGroup();
                List<WorkoutSetJpaEntity> sets = we.getSets();
                for (int i = 0; i < sets.size(); i++) {
                    WorkoutSetJpaEntity s = sets.get(i);
                    double weight = s.getWeightKg() != null ? s.getWeightKg() : 0.0;
                    double rpe = s.getRpe() != null ? s.getRpe() : 0.0;
                    double volume = weight * s.getReps();
                    rows.add(List.of(
                            dateStr,
                            workout.getName(),
                            exerciseName,
                            muscleGroup,
                            String.valueOf(i + 1),
                            String.format("%.2f", weight),
                            String.valueOf(s.getReps()),
                            rpe > 0 ? String.format("%.1f", rpe) : "",
                            String.format("%.2f", volume)
                    ));
                }
            }
        }
        return rows;
    }

    private String csvEscape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
