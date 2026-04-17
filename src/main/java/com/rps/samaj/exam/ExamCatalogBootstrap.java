package com.rps.samaj.exam;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Seeds a small catalog when the exams table is empty so the Exams UI is usable on fresh installs.
 */
@Component
public class ExamCatalogBootstrap implements ApplicationRunner {

    private final ExamRepository examRepository;

    public ExamCatalogBootstrap(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (examRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        examRepository.save(build(
                UUID.randomUUID(),
                "UPSC Civil Services Preliminary 2026",
                "Union Public Service Commission — Civil Services (Preliminary) Examination. Check official notification for syllabus and scheme.",
                "upsc",
                today.plusMonths(1),
                today.plusMonths(4),
                today.plusMonths(6),
                "Graduate from a recognized university.",
                "https://upsc.gov.in",
                false
        ));
        examRepository.save(build(
                UUID.randomUUID(),
                "SSC CGL Tier-I",
                "Staff Selection Commission — Combined Graduate Level examination. Application window and schedule as per SSC notice.",
                "ssc",
                today.plusWeeks(2),
                today.plusMonths(2),
                today.plusMonths(5),
                "Bachelor's degree.",
                "https://ssc.nic.in",
                false
        ));
        examRepository.save(build(
                UUID.randomUUID(),
                "IBPS PO Recruitment",
                "Institute of Banking Personnel Selection — Probationary Officer common recruitment process.",
                "banking",
                today.minusMonths(1),
                today.minusWeeks(2),
                today.plusMonths(1),
                "Graduate; age limit as per official advertisement.",
                "https://ibps.in",
                true
        ));
        examRepository.save(build(
                UUID.randomUUID(),
                "State PSC Combined Competitive",
                "State Public Service Commission combined competitive examination — refer state PSC portal for eligibility.",
                "state",
                today.plusDays(10),
                today.plusMonths(3),
                today.plusMonths(8),
                "As per state notification.",
                null,
                false
        ));
    }

    private static Exam build(
            UUID id,
            String title,
            String description,
            String type,
            LocalDate notificationDate,
            LocalDate lastDate,
            LocalDate examDate,
            String eligibility,
            String applyUrl,
            boolean expired
    ) {
        Exam e = new Exam(id, title, description, type);
        e.setNotificationDate(notificationDate);
        e.setLastDate(lastDate);
        e.setExamDate(examDate);
        e.setEligibility(eligibility);
        e.setApplyUrl(applyUrl);
        e.setExpired(expired);
        return e;
    }
}
