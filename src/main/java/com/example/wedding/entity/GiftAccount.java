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

@Getter
@Setter
@Entity
@Table(name = "gift_accounts")
public class GiftAccount {
    @Id
    @Column(name = "gift_account_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "wedding_id", nullable = false)
    private WeddingCard wedding;

    @Size(max = 50)
    @NotNull
    @Column(name = "target_person", nullable = false, length = 50)
    private String targetPerson;

    @Size(max = 255)
    @Column(name = "bank_name")
    private String bankName;

    @Size(max = 100)
    @Column(name = "bank_code", length = 100)
    private String bankCode;

    @Size(max = 500)
    @Column(name = "qr_img", length = 500)
    private String qrImg;

    @Size(max = 255)
    @Column(name = "account_name")
    private String accountName;

    @Size(max = 100)
    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @ColumnDefault("current_timestamp()")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("current_timestamp()")
    @Column(name = "update_at")
    private Instant updateAt;

}