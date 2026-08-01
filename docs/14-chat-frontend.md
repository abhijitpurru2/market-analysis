# Chat Frontend

## Overview

The chat frontend is a single-page React application that provides a conversational interface for market analysis queries.

## Technology Stack

| Library | Version | Purpose |
|---------|---------|---------|
| React | 18 | UI framework |
| axios | 1.7 | HTTP client |
| uuid | 10 | Session ID generation |

## Component Structure

```
src/
├── App.js          # Main chat component
├── App.css         # Dark-theme styles
├── index.js        # React entry point
└── index.css       # Global reset
```

## Features

- Dark-theme UI optimised for financial data
- Persistent session via UUID stored in component state
- Quick-prompt buttons for common queries
- Typing indicator while awaiting LLM response
- Auto-scroll to latest message
- Proxied API calls to `/api/chat` via nginx

## Build

```bash
npm install
npm run build
```

## Development

```bash
npm start   # Starts dev server at http://localhost:3000
```

Set `REACT_APP_API_BASE` to override the API base URL.
