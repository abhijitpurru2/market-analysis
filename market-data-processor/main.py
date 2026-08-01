import json
import logging
import os
import threading
from contextlib import asynccontextmanager
from datetime import datetime

import psycopg2
import psycopg2.extras
from fastapi import FastAPI
from kafka import KafkaConsumer
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "marketdb")
DB_USER = os.getenv("DB_USER", "market")
DB_PASS = os.getenv("DB_PASS", "market")

QUOTES_TOPIC = "market.data.quotes"
SENTIMENT_TOPIC = "market.sentiment.scores"

CREATE_PROCESSED_QUOTES = """
CREATE TABLE IF NOT EXISTS processed_quotes (
    id          SERIAL PRIMARY KEY,
    ticker      VARCHAR(10) NOT NULL,
    price       NUMERIC(18,4),
    change_pct  NUMERIC(18,4),
    volume      BIGINT,
    ts          TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);
"""

CREATE_SENTIMENT_SCORES = """
CREATE TABLE IF NOT EXISTS sentiment_scores (
    id         SERIAL PRIMARY KEY,
    ticker     VARCHAR(10) NOT NULL,
    sentiment  VARCHAR(20),
    score      NUMERIC(6,4),
    source     VARCHAR(50),
    ts         TIMESTAMPTZ DEFAULT NOW()
);
"""


def get_conn():
    return psycopg2.connect(
        host=DB_HOST, port=DB_PORT, dbname=DB_NAME,
        **{"password": DB_PASS, "user": DB_USER}
    )


def init_db():
    conn = get_conn()
    with conn.cursor() as cur:
        cur.execute(CREATE_PROCESSED_QUOTES)
        cur.execute(CREATE_SENTIMENT_SCORES)
    conn.commit()
    conn.close()
    logger.info("Database tables initialised")


def consume_quotes():
    conn = get_conn()
    consumer = KafkaConsumer(
        QUOTES_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        group_id="market-data-processor-quotes",
        auto_offset_reset="latest",
    )
    logger.info("Consuming market quotes from %s", QUOTES_TOPIC)
    for message in consumer:
        try:
            q = message.value
            with conn.cursor() as cur:
                cur.execute(
                    "INSERT INTO processed_quotes (ticker, price, change_pct, volume, ts) "
                    "VALUES (%s, %s, %s, %s, %s)",
                    (q["ticker"], q["price"], q["changePercent"], q["volume"],
                     datetime.utcnow()),
                )
            conn.commit()
        except Exception as exc:
            logger.error("Error persisting quote: %s", exc)
            conn.rollback()


def consume_sentiment():
    conn = get_conn()
    consumer = KafkaConsumer(
        SENTIMENT_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
        group_id="market-data-processor-sentiment",
        auto_offset_reset="latest",
    )
    logger.info("Consuming sentiment scores from %s", SENTIMENT_TOPIC)
    for message in consumer:
        try:
            s = message.value
            with conn.cursor() as cur:
                cur.execute(
                    "INSERT INTO sentiment_scores (ticker, sentiment, score, source) "
                    "VALUES (%s, %s, %s, %s)",
                    (s["ticker"], s["sentiment"], s["score"], s.get("source_topic", "")),
                )
            conn.commit()
        except Exception as exc:
            logger.error("Error persisting sentiment: %s", exc)
            conn.rollback()


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    for fn in [consume_quotes, consume_sentiment]:
        threading.Thread(target=fn, daemon=True).start()
    yield


app = FastAPI(title="Market Data Processor", lifespan=lifespan)


class StatsResponse(BaseModel):
    ticker: str
    avg_price: float
    avg_sentiment: float
    record_count: int


@app.get("/stats/{ticker}", response_model=StatsResponse)
def get_stats(ticker: str):
    conn = get_conn()
    try:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(
                "SELECT AVG(price)::float AS avg_price, COUNT(*) AS record_count "
                "FROM processed_quotes WHERE ticker = %s",
                (ticker.upper(),),
            )
            quote_row = cur.fetchone()
            cur.execute(
                "SELECT AVG(score)::float AS avg_sentiment "
                "FROM sentiment_scores WHERE ticker = %s",
                (ticker.upper(),),
            )
            sent_row = cur.fetchone()
        return StatsResponse(
            ticker=ticker.upper(),
            avg_price=quote_row["avg_price"] or 0.0,
            avg_sentiment=sent_row["avg_sentiment"] or 0.0,
            record_count=quote_row["record_count"] or 0,
        )
    finally:
        conn.close()


@app.get("/health")
def health():
    return {"status": "UP", "service": "market-data-processor"}
