#!/bin/bash
# Create Kafka topics for the market-analysis platform
set -e

KAFKA_HOST="${KAFKA_HOST:-localhost:9092}"

TOPICS=(
  "market.news.raw:3:1"
  "market.social.posts:3:1"
  "market.data.quotes:6:1"
  "market.sentiment.scores:3:1"
)

for TOPIC_DEF in "${TOPICS[@]}"; do
  IFS=':' read -r TOPIC PARTITIONS REPLICATION <<< "$TOPIC_DEF"
  echo "Creating topic: $TOPIC (partitions=$PARTITIONS, replication=$REPLICATION)"
  kafka-topics.sh \
    --bootstrap-server "$KAFKA_HOST" \
    --create \
    --if-not-exists \
    --topic "$TOPIC" \
    --partitions "$PARTITIONS" \
    --replication-factor "$REPLICATION"
done

echo "All Kafka topics created."
