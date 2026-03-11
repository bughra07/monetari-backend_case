package com.bugra.monetari.repository;

import com.bugra.monetari.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    List<PriceRecord> findByCoinIdOrderByFetchedAtDesc(String coinId);
}