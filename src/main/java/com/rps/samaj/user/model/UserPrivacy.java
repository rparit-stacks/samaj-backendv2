package com.rps.samaj.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "samaj_user_privacy")
public class UserPrivacy {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "show_email")
    private boolean showEmail;

    @Column(name = "show_blood_group")
    private boolean showBloodGroup;

    @Column(name = "show_phone")
    private boolean showPhone;

    @Column(name = "show_family_members")
    private boolean showFamilyMembers;

    @Column(name = "profile_visibility", nullable = false, length = 32)
    private String profileVisibility;

    @Column(name = "service_privacy_json", columnDefinition = "text")
    private String servicePrivacyJson;

    protected UserPrivacy() {
    }

    public UserPrivacy(User user) {
        this.user = user;
        this.profileVisibility = "MEMBERS_ONLY";
        this.showEmail = false;
        this.showBloodGroup = false;
        this.showPhone = false;
        this.showFamilyMembers = false;
        this.servicePrivacyJson = "{}";
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public boolean isShowEmail() {
        return showEmail;
    }

    public void setShowEmail(boolean showEmail) {
        this.showEmail = showEmail;
    }

    public boolean isShowBloodGroup() {
        return showBloodGroup;
    }

    public void setShowBloodGroup(boolean showBloodGroup) {
        this.showBloodGroup = showBloodGroup;
    }

    public boolean isShowPhone() {
        return showPhone;
    }

    public void setShowPhone(boolean showPhone) {
        this.showPhone = showPhone;
    }

    public boolean isShowFamilyMembers() {
        return showFamilyMembers;
    }

    public void setShowFamilyMembers(boolean showFamilyMembers) {
        this.showFamilyMembers = showFamilyMembers;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(String profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    public String getServicePrivacyJson() {
        return servicePrivacyJson;
    }

    public void setServicePrivacyJson(String servicePrivacyJson) {
        this.servicePrivacyJson = servicePrivacyJson;
    }
}
