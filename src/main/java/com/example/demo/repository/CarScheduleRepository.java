package com.example.demo.repository;

import com.example.demo.entity.CarSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface CarScheduleRepository extends JpaRepository<CarSchedule, Integer> {
    List<CarSchedule> findByCarCarId(Integer carId);

    @Query("SELECT s FROM CarSchedule s WHERE s.car.carId = :carId " +
           "AND s.status = 'UNAVAILABLE' " +
           "AND s.startDate < :endDate AND s.endDate > :startDate")
    List<CarSchedule> findOverlappingSchedules(
            @Param("carId") Integer carId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
