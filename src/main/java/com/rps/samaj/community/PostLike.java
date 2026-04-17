package com.rps.samaj.community;

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
@Table(name = "samaj_post_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class PostLike {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    protected PostLike() {
    }

    public PostLike(UUID id, CommunityPost post, User user) {
        this.id = id;
        this.post = post;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public CommunityPost getPost() {
        return post;
    }

    public User getUser() {
        return user;
    }
}
