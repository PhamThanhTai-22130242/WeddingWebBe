package com.example.wedding.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "card_people")
public class CardPerson {
    @Id
    @Column(name = "card_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "wedding_id", nullable = false)
    private WeddingCard wedding;

    @Size(max = 50)
    @NotNull
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Size(max = 255)
    @Column(name = "full_name")
    private String fullName;

    @Size(max = 100)
    @Column(name = "short_name", length = 100)
    private String shortName;

    @Size(max = 500)
    @Column(name = "avatar", length = 500)
    private String avatar;

    @Size(max = 255)
    @Column(name = "father_name")
    private String fatherName;

    @Size(max = 255)
    @Column(name = "mother_name")
    private String motherName;

    @ColumnDefault("current_timestamp()")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnDefault("current_timestamp()")
    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @Size(max = 100)
    @Column(name = "family_lable", length = 100)
    private String familyLable;

}