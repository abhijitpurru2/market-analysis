import asyncio
import json
import logging
import os
import re
import threading
from contextlib import asynccontextmanager

from fastapi import FastAPI
from kafka import KafkaConsumer, KafkaProducer
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
NEWS_TOPIC = "market.news.raw"
SOCIAL_TOPIC = "market.social.posts"
SENTIMENT_TOPIC = "market.sentiment.scores"

# Simple lexicon-based sentiment (no heavy model download needed in demo)
BULLISH_WORDS = {
    "buy", "bull", "bullish", "strong", "beat", "record", "growth", "surge",
    "rally", "moon", "gain", "positive", "up", "higher", "long", "boost",
}
BEARISH_WORDS = {
    "sell", "bear", "bearish", "weak", "miss", "decline", "drop", "fall",
    "crash", "short", "loss", "negative", "down", "lower", "risk", "worry",
}


def analyze_sentiment(text: str) -> dict:
    words = set(re.findall(r"\b\w+\b", text.lower()))
    bull_score = len(words & BULLISH_WORDS)
    bear_score = len(words & BEARISH_WORDS)
    total = bull_score + bear_score or 1
    score = (bull_score - bear_score) / total
    label = "POSITIVE" if score > 0 else "NEGATIVE" if score < 0 else "NEUTRAL"
    return {"label": label, "score": round(score, 4)}


producer = None


def get_producer():
    global producer
    if producer is None:
        producer = KafkaProducer(
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8") if k else None,
        )
    return producer


def process_topic(topic: str):
    consumer = KafkaConsumer(
        topic,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        group_id=f"sentiment-analyzer-{topic}",
        auto_offset_reset="latest",
    )
    logger.info("Listening on topic: %s", topic)
    for message in consumer:
        try:
            data = message.value
            text = data.get("content") or data.get("title", "")
            ticker = data.get("ticker", "UNKNOWN")
            sentiment = analyze_sentiment(text)
            result = {
                "source_topic": topic,
                "ticker": ticker,
                "text_preview": text[:120],
                "sentiment": sentiment["label"],
                "score": sentiment["score"],
            }
            get_producer().send(SENTIMENT_TOPIC, key=ticker, value=result)
            logger.info("Sentiment for %s: %s (%.4f)", ticker, sentiment["label"], sentiment["score"])
        except Exception as exc:
            logger.error("Error processing message: %s", exc)


@asynccontextmanager
async def lifespan(app: FastAPI):
    for topic in [NEWS_TOPIC, SOCIAL_TOPIC]:
        t = threading.Thread(target=process_topic, args=(topic,), daemon=True)
        t.start()
    yield


app = FastAPI(title="Sentiment Analysis Service", lifespan=lifespan)


class AnalyzeRequest(BaseModel):
    text: str
    ticker: str = "UNKNOWN"


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    result = analyze_sentiment(req.text)
    return {"ticker": req.ticker, **result}


@app.get("/health")
def health():
    return {"status": "UP", "service": "sentiment-analysis"}
