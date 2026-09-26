package com.manatandas.backend.bathroom;

import com.manatandas.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SavedBathroomRepository extends JpaRepository<SavedBathroom, Long> {

    List<SavedBathroom> findByUser(User user);

    boolean existsByUserAndBathroom(User user, Bathroom bathroom);

    @Transactional
    void deleteByUserAndBathroom(User user, Bathroom bathroom);

    @Transactional
    void deleteAllByBathroom(Bathroom bathroom);
}
