package com.rps.samaj.community;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "samaj_post_tags", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"}))
public class PostTagLink {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id")
    private CommunityTag tag;

    protected PostTagLink() {
    }

    public PostTagLink(UUID id, CommunityPost post, CommunityTag tag) {
        this.id = id;
        this.post = post;
        this.tag = tag;
    }

    public UUID getId() {
        return id;
    }

    public CommunityPost getPost() {
        return post;
    }

    public CommunityTag getTag() {
        return tag;
    }
}
