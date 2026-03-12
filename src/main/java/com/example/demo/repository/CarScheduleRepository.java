package com.example.demo.repository;

import com.example.demo.entity.CarSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarScheduleRepository extends JpaRepository<CarSchedule, Integer> {
    List<CarSchedule> findByCarCarId(Integer carId);
}
