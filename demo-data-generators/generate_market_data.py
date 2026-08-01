"""
Demo data generator for the market-analysis platform.
Publishes synthetic market quotes, news articles and social posts to Kafka.
"""

import json
import logging
import random
import time
import uuid
from datetime import datetime, timezone

from faker import Faker
from kafka import KafkaProducer

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
fake = Faker()

TICKERS = ["AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "SPY", "QQQ", "META", "NFLX"]
BASE_PRICES = {"AAPL": 190, "MSFT": 420, "GOOGL": 175, "AMZN": 185,
               "TSLA": 250, "NVDA": 900, "SPY": 540, "QQQ": 460, "META": 490, "NFLX": 650}

BULLISH_TEMPLATES = [
    "{ticker} surges on strong earnings beat",
    "{ticker} reaches new all-time high as demand accelerates",
    "Analysts raise {ticker} price target after record quarter",
    "{ticker} buyback programme boosts shareholder confidence",
]
BEARISH_TEMPLATES = [
    "{ticker} falls on disappointing guidance",
    "{ticker} faces regulatory headwinds",
    "Analysts cut {ticker} target amid macro concerns",
    "{ticker} misses revenue estimates in latest quarter",
]


def create_producer() -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
        key_serializer=lambda k: k.encode("utf-8") if k else None,
    )


def generate_quote(ticker: str) -> dict:
    base = BASE_PRICES[ticker]
    price = round(base + random.gauss(0, base * 0.005), 2)
    change_pct = round(random.gauss(0, 0.8), 2)
    volume = random.randint(500_000, 10_000_000)
    return {
        "id": str(uuid.uuid4()),
        "ticker": ticker,
        "price": price,
        "changePercent": change_pct,
        "volume": volume,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


def generate_news_article(ticker: str) -> dict:
    templates = BULLISH_TEMPLATES if random.random() > 0.4 else BEARISH_TEMPLATES
    title = random.choice(templates).format(ticker=ticker)
    return {
        "id": str(uuid.uuid4()),
        "title": title,
        "content": fake.paragraph(nb_sentences=4),
        "source": random.choice(["Reuters", "Bloomberg", "Financial Times", "WSJ", "CNBC"]),
        "url": f"https://example.com/news/{uuid.uuid4()}",
        "publishedAt": datetime.now(timezone.utc).isoformat(),
        "ticker": ticker,
    }


def generate_social_post(ticker: str) -> dict:
    sentiments = [
        f"${ticker} looking strong today! Bought the dip 🚀",
        f"Not sure about ${ticker}, fundamentals seem stretched",
        f"${ticker} just broke resistance — going long",
        f"Trimming my ${ticker} position, too much uncertainty",
        f"${ticker} earnings coming up, positioning cautiously",
    ]
    return {
        "id": str(uuid.uuid4()),
        "platform": random.choice(["twitter", "reddit", "stocktwits"]),
        "author": fake.user_name(),
        "content": random.choice(sentiments),
        "ticker": ticker,
        "likes": random.randint(0, 5000),
        "reposts": random.randint(0, 1000),
        "createdAt": datetime.now(timezone.utc).isoformat(),
    }


def run():
    producer = create_producer()
    logger.info("Demo data generator started. Publishing to %s", KAFKA_BOOTSTRAP_SERVERS)

    iteration = 0
    while True:
        ticker = random.choice(TICKERS)

        quote = generate_quote(ticker)
        producer.send("market.data.quotes", key=ticker, value=quote)
        logger.info("[QUOTE] %s @ $%.2f (%.2f%%)", ticker, quote["price"], quote["changePercent"])

        if iteration % 5 == 0:
            article = generate_news_article(ticker)
            producer.send("market.news.raw", key=ticker, value=article)
            logger.info("[NEWS] %s", article["title"])

        if iteration % 3 == 0:
            post = generate_social_post(ticker)
            producer.send("market.social.posts", key=ticker, value=post)
            logger.info("[SOCIAL] %s: %s", post["platform"], post["content"][:60])

        producer.flush()
        iteration += 1
        time.sleep(2)


if __name__ == "__main__":
    run()
