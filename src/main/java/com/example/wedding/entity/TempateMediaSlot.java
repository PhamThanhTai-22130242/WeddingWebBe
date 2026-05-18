package com.example.wedding.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tempate_media_slots")
public class TempateMediaSlot {
    @Id
    @Column(name = "template_media_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "slot_key", nullable = false, length = 100)
    private String slotKey;

    @Size(max = 255)
    @Column(name = "slot_lable")
    private String slotLable;

    @Size(max = 50)
    @ColumnDefault("'image'")
    @Column(name = "media_type", length = 50)
    private String mediaType;

    @ColumnDefault("1")
    @Column(name = "max_item")
    private Integer maxItem;

    @ColumnDefault("current_timestamp()")
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ColumnDefault("current_timestamp()")
    @Column(name = "update_at")
    private LocalDateTime updateAt;

}