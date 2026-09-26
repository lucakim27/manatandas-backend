package com.manatandas.backend.bathroom;

import com.manatandas.backend.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Join entity linking a User to a Bathroom they've bookmarked. Kept in the
// bathroom package (rather than user, or a standalone package) since it's
// conceptually "a bathroom-related fact" — the same reasoning that would
// apply to a future Review entity.
@Entity
@Table(
    name = "saved_bathrooms",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "bathroom_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedBathroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bathroom_id", nullable = false)
    private Bathroom bathroom;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
