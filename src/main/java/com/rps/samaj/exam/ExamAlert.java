package com.rps.samaj.exam;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "samaj_exam_alerts", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "exam_id"}))
public class ExamAlert {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    protected ExamAlert() {
    }

    public ExamAlert(UUID id, User user, Exam exam) {
        this.id = id;
        this.user = user;
        this.exam = exam;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Exam getExam() {
        return exam;
    }
}
