package com.rps.samaj.matrimony;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "samaj_matrimony_profiles")
public class MatrimonyProfileEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false, length = 16)
    private String gender;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 16)
    private String profileSubject;

    @Column(name = "relative_relation")
    private String relativeRelation;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    private String city;
    private String state;
    private String country;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "photo_urls_json", columnDefinition = "text")
    private String photoUrlsJson;

    @Column(name = "hobbies_json", columnDefinition = "text")
    private String hobbiesJson;

    @Column(name = "detail_json", columnDefinition = "text")
    private String detailJson;

    @Column(nullable = false, length = 16)
    private String status;

    /** When false, profile is hidden from matrimony search/browse lists. */
    @Column(name = "visible_in_search")
    private boolean visibleInSearch = true;

    @Column(name = "draft_step")
    private int draftStep;

    private boolean verified;

    @Column(name = "completion_percent")
    private Integer completionPercent;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected MatrimonyProfileEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getProfileSubject() {
        return profileSubject;
    }

    public void setProfileSubject(String profileSubject) {
        this.profileSubject = profileSubject;
    }

    public String getRelativeRelation() {
        return relativeRelation;
    }

    public void setRelativeRelation(String relativeRelation) {
        this.relativeRelation = relativeRelation;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public Integer getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Integer weightKg) {
        this.weightKg = weightKg;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getPhotoUrlsJson() {
        return photoUrlsJson;
    }

    public void setPhotoUrlsJson(String photoUrlsJson) {
        this.photoUrlsJson = photoUrlsJson;
    }

    public String getHobbiesJson() {
        return hobbiesJson;
    }

    public void setHobbiesJson(String hobbiesJson) {
        this.hobbiesJson = hobbiesJson;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVisibleInSearch() {
        return visibleInSearch;
    }

    public void setVisibleInSearch(boolean visibleInSearch) {
        this.visibleInSearch = visibleInSearch;
    }

    public int getDraftStep() {
        return draftStep;
    }

    public void setDraftStep(int draftStep) {
        this.draftStep = draftStep;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Integer getCompletionPercent() {
        return completionPercent;
    }

    public void setCompletionPercent(Integer completionPercent) {
        this.completionPercent = completionPercent;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
