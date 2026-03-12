package com.example.demo.repository;

import com.example.demo.entity.Car;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CarRepository extends JpaRepository<Car, Integer> {

    List<Car> findByOwner(User owner);

    List<Car> findByLocationCityAndStatus(String locationCity, String status);

    @Query("SELECT c FROM Car c WHERE c.status = 'AVAILABLE' " +
           "AND (:city IS NULL OR c.locationCity = :city) " +
           "AND (:brandId IS NULL OR c.brand.brandId = :brandId) " +
           "AND (:categoryId IS NULL OR c.category.categoryId = :categoryId) " +
           "AND c.carId NOT IN (" +
           "  SELECT b.car.carId FROM Booking b " +
           "  WHERE b.status NOT IN ('CANCELLED') " +
           "  AND b.startDate < :endDate AND b.endDate > :startDate" +
           ")")
    List<Car> searchAvailableCars(
            @Param("city") String city,
            @Param("brandId") Integer brandId,
            @Param("categoryId") Integer categoryId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
