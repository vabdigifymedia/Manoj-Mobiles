package com.api.manojmobiles.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "categories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent")
    @JsonManagedReference
    private Set<Category> children = new HashSet<>();

    @Column(unique = true)
    private String slug;

    private String imageUrl;

    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;

    @PrePersist
    @PreUpdate
    public void autoGenerateSlug() {
        if (this.name != null && (this.slug == null || this.slug.isBlank())) {
            this.slug = this.name.toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
        }
    }
}
