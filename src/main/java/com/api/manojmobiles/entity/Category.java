package com.api.manojmobiles.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Column(columnDefinition = "text", length = 10485760)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent")
    @JsonManagedReference
    private Set<Category> children = new HashSet<>();

    @Column(columnDefinition = "text", length = 10485760, unique = true)
    private String slug;

    @Column(columnDefinition = "text", length = 10485760)
    private String imageUrl;

    @Column(columnDefinition = "text", length = 10485760)
    private String description;

    @Column(columnDefinition = "text", length = 10485760)
    private String metaTitle;

    @Column(columnDefinition = "text", length = 10485760)
    private String metaDescription;

    @Column(columnDefinition = "text", length = 10485760)
    private String metaKeywords;

    @PrePersist
    @PreUpdate
    public void autoGenerateSlug() {
        if (this.name != null && (this.slug == null || this.slug.isBlank())) {
            String base = this.name.toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            if (base.isBlank()) base = "category";
            if (base.length() > 200) base = base.substring(0, 200);
            this.slug = base;
        }
    }
}
