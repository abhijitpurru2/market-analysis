package com.market.marketdata;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/quotes/{ticker}")
    public ResponseEntity<List<MarketQuote>> getQuotes(@PathVariable String ticker) {
        return ResponseEntity.ok(marketDataService.getRecentQuotes(ticker.toUpperCase()));
    }

    @PostMapping("/quotes/refresh")
    public ResponseEntity<Void> refresh() {
        marketDataService.publishQuotes();
        return ResponseEntity.accepted().build();
    }
}
