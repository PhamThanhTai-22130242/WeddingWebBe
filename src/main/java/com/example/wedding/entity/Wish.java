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
@Table(name = "wishes")
public class Wish {
    @Id
    @Column(name = "wish_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "wedding_id", nullable = false)
    private WeddingCard wedding;

    @Size(max = 255)
    @Column(name = "guest_name")
    private String guestName;

    @Lob
    @Column(name = "message")
    private String message;

    @ColumnDefault("current_timestamp()")
    @Column(name = "`created-at`")
    private LocalDateTime createdAt;

    @ColumnDefault("0")
    @Column(name = "is_approved")
    private Boolean isApproved;

}