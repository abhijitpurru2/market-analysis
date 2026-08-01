package com.market.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketQuoteRepository extends JpaRepository<MarketQuote, String> {
    List<MarketQuote> findTop10ByTickerOrderByTimestampDesc(String ticker);
}
