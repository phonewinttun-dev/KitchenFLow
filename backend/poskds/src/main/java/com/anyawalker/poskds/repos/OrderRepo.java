package com.anyawalker.poskds.repos;

import com.anyawalker.poskds.models.entities.OrderEntity;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<@NonNull OrderEntity,@NonNull Long> {
    Optional<OrderEntity> findByIdAndUserEntity_Id(@NonNull Long orderId,@NonNull Long userId);
}
