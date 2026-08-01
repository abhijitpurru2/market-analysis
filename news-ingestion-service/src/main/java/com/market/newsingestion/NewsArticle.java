package com.market.newsingestion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsArticle {
    private String id;
    private String title;
    private String content;
    private String source;
    private String url;
    private Instant publishedAt;
    private String ticker;
}
