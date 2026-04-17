package com.rps.samaj.emergency;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_emergency_helpers")
public class EmergencyHelper {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id")
    private EmergencyCase emergency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "helper_id")
    private User helper;

    @Column(name = "helped_at")
    private Instant helpedAt;

    @Column(columnDefinition = "text")
    private String note;

    protected EmergencyHelper() {
    }

    public EmergencyHelper(UUID id, EmergencyCase emergency, User helper, Instant helpedAt, String note) {
        this.id = id;
        this.emergency = emergency;
        this.helper = helper;
        this.helpedAt = helpedAt;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public EmergencyCase getEmergency() {
        return emergency;
    }

    public User getHelper() {
        return helper;
    }

    public Instant getHelpedAt() {
        return helpedAt;
    }

    public String getNote() {
        return note;
    }
}
