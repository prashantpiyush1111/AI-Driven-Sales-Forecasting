package com.salesforecast.repository;

import com.salesforecast.entity.SalesRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select s from SalesRecord s
            where (:productName is null or s.productName = :productName)
              and (:category is null or s.category = :category)
              and (:region is null or s.region = :region)
              and (:fromDate is null or s.saleDate >= :fromDate)
              and (:toDate is null or s.saleDate <= :toDate)
            """)
    Page<SalesRecord> search(@Param("productName") String productName,
                             @Param("category") String category,
                             @Param("region") String region,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate,
                             Pageable pageable);

    @Query("""
            select sum(s.quantity) as totalQuantity,
                   sum(s.revenue) as totalRevenue,
                   count(s) as recordCount
            from SalesRecord s
            where (:productName is null or s.productName = :productName)
              and (:category is null or s.category = :category)
              and (:region is null or s.region = :region)
              and (:fromDate is null or s.saleDate >= :fromDate)
              and (:toDate is null or s.saleDate <= :toDate)
            """)
    SalesSummaryView summarize(@Param("productName") String productName,
                               @Param("category") String category,
                               @Param("region") String region,
                               @Param("fromDate") LocalDate fromDate,
                               @Param("toDate") LocalDate toDate);
}