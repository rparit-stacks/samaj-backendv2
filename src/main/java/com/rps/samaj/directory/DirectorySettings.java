package com.rps.samaj.directory;

import com.rps.samaj.user.model.User;
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
@Table(name = "samaj_directory_settings")
public class DirectorySettings {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private boolean visible;

    @Column(name = "actions_json", columnDefinition = "text")
    private String actionsJson;

    protected DirectorySettings() {
    }

    public DirectorySettings(User user) {
        this.user = user;
        this.id = user.getId();
        this.visible = true;
        this.actionsJson = "[]";
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getActionsJson() {
        return actionsJson;
    }

    public void setActionsJson(String actionsJson) {
        this.actionsJson = actionsJson;
    }
}
