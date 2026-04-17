package com.rps.samaj.emergency;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "samaj_emergencies")
public class EmergencyCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    private String area;
    private String city;
    private String state;
    private String country;
    private String landmark;

    @Column(name = "location_description", columnDefinition = "text")
    private String locationDescription;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "emergency_at")
    private Instant emergencyAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "helper_count")
    private int helperCount;

    @Column(name = "view_count")
    private int viewCount;

    @Column(name = "contact_click_count")
    private int contactClickCount;

    @Column(name = "resolved_by_external")
    private boolean resolvedByExternal;

    @Column(name = "external_helper_note", columnDefinition = "text")
    private String externalHelperNote;

    @Column(name = "contact_prefs_json", columnDefinition = "text")
    private String contactPrefsJson;

    protected EmergencyCase() {
    }

    public Long getId() {
        return id;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
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

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getEmergencyAt() {
        return emergencyAt;
    }

    public void setEmergencyAt(Instant emergencyAt) {
        this.emergencyAt = emergencyAt;
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

    public int getHelperCount() {
        return helperCount;
    }

    public void setHelperCount(int helperCount) {
        this.helperCount = helperCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getContactClickCount() {
        return contactClickCount;
    }

    public void setContactClickCount(int contactClickCount) {
        this.contactClickCount = contactClickCount;
    }

    public boolean isResolvedByExternal() {
        return resolvedByExternal;
    }

    public void setResolvedByExternal(boolean resolvedByExternal) {
        this.resolvedByExternal = resolvedByExternal;
    }

    public String getExternalHelperNote() {
        return externalHelperNote;
    }

    public void setExternalHelperNote(String externalHelperNote) {
        this.externalHelperNote = externalHelperNote;
    }

    public String getContactPrefsJson() {
        return contactPrefsJson;
    }

    public void setContactPrefsJson(String contactPrefsJson) {
        this.contactPrefsJson = contactPrefsJson;
    }
}
