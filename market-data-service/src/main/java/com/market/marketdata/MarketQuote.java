package com.market.marketdata;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "market_quotes",
    indexes = @Index(name = "idx_market_quotes_ticker_timestamp", columnList = "ticker,timestamp")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal changePercent;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private Instant timestamp;
}
