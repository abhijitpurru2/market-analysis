package com.market.socialmedia;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialPost {
    private String id;
    private String platform;
    private String author;
    private String content;
    private String ticker;
    private int likes;
    private int reposts;
    private Instant createdAt;
}
