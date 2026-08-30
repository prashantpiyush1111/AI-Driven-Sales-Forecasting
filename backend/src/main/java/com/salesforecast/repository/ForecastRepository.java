package com.salesforecast.repository;

import com.salesforecast.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, Long> {

    List<Forecast> findByProductName(String productName);

    List<Forecast> findByProductNameOrderByForecastDateAsc(String productName);

    Optional<Forecast> findTopByProductNameOrderByCreatedAtDesc(String productName);

    List<Forecast> findByForecastDateBetween(LocalDate startDate, LocalDate endDate);
}
