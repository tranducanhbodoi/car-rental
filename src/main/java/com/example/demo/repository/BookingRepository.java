package com.example.demo.repository;

import com.example.demo.entity.Booking;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByCustomer(User customer);

    List<Booking> findByCarCarId(Integer carId);

    @Query("SELECT b FROM Booking b WHERE b.car.carId = :carId " +
           "AND b.status NOT IN ('CANCELLED') " +
           "AND b.startDate < :endDate AND b.endDate > :startDate")
    List<Booking> findOverlappingBookings(
            @Param("carId") Integer carId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.createdAt < :cutoff")
    List<Booking> findPendingBookingsOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT b FROM Booking b WHERE b.car.owner.id = :ownerId")
    List<Booking> findByCarOwnerId(@Param("ownerId") Integer ownerId);
}
