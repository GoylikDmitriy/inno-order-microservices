package com.goylik.user_service.repository;

import com.goylik.user_service.model.entity.PaymentCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long> {
    long countByUserId(Long userId);

    @Query("""
            SELECT c
            FROM PaymentCard c
            JOIN FETCH c.user
            WHERE c.user.id = :userId
            """)
    List<PaymentCard> findByUserIdWithUser(Long userId);

    @Query(value = "SELECT c FROM PaymentCard c JOIN FETCH c.user",
            countQuery = "SELECT COUNT(c) FROM PaymentCard c"
    )
    Page<PaymentCard> findAllWithUser(Pageable pageable);

    @Query("""
            SELECT c
            FROM PaymentCard c
            JOIN FETCH c.user
            WHERE c.id = :id
            """)
    Optional<PaymentCard> findByIdWithUser(@Param("id") Long id);

    @Modifying
    @Query("""
       update PaymentCard c
       set c.active = false
       where c.active = true
       and c.deletedAt is null
       and c.expirationDate < :now
       """)
    int deactivateExpiredCards(YearMonth now);
}
