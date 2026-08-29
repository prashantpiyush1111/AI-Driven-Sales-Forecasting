package com.salesforecast.repository;

import com.salesforecast.entity.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {

    List<SalesRecord> findByProductName(String productName);

    List<SalesRecord> findByCategory(String category);

    List<SalesRecord> findByRegion(String region);

    List<SalesRecord> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);

    List<SalesRecord> findByProductNameAndSaleDateBetween(
            String productName, 
            LocalDate startDate, 
            LocalDate endDate
    );
}