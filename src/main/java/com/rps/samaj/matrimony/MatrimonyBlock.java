package com.rps.samaj.matrimony;

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
@Table(name = "samaj_matrimony_blocks", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_user_id", "blocked_user_id"}))
public class MatrimonyBlock {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id")
    private User blocked;

    protected MatrimonyBlock() {
    }

    public MatrimonyBlock(UUID id, User owner, User blocked) {
        this.id = id;
        this.owner = owner;
        this.blocked = blocked;
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public User getBlocked() {
        return blocked;
    }
}
