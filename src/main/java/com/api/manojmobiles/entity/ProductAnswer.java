package com.api.manojmobiles.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "product_answer")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ProductQuestion question;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") private User user;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String answerText;

    @Builder.Default
    private Boolean isSellerAnswer = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
