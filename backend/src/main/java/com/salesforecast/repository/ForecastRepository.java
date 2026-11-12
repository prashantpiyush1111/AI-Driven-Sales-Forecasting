package com.salesforecast.repository;

import com.salesforecast.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ForecastRepository extends JpaRepository<Forecast, Long> {

    List<Forecast> findByProductName(String productName);

    List<Forecast> findByProductNameOrderByForecastDateAsc(String productName);

    Optional<Forecast> findTopByProductNameOrderByCreatedAtDesc(String productName);

    List<Forecast> findByForecastDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("""
            select f from Forecast f
            where f.productName = :productName
              and (:fromDate is null or f.forecastDate >= :fromDate)
              and (:toDate is null or f.forecastDate <= :toDate)
            order by f.forecastDate asc, f.createdAt asc
            """)
    List<Forecast> search(@Param("productName") String productName,
                          @Param("fromDate") LocalDate fromDate,
                          @Param("toDate") LocalDate toDate);

    @Query("select sum(f.predictedRevenue) from Forecast f where f.forecastDate >= :fromDate")
    BigDecimal sumPredictedRevenueFrom(@Param("fromDate") LocalDate fromDate);
}
