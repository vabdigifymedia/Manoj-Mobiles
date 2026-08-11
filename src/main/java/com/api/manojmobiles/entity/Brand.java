package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "brands")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Column(columnDefinition = "text", length = 10485760)
    private String name;

    @Column(columnDefinition = "text", length = 10485760)
    private String logo_url;

    @Column(columnDefinition = "text", length = 10485760)
    private String description;

    @Column(columnDefinition = "text", length = 10485760, unique = true)
    private String slug;

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
            if (base.isBlank()) base = "brand";
            if (base.length() > 200) base = base.substring(0, 200);
            this.slug = base;
        }
    }
}
