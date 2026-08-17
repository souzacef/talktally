# TalkTally frontend

React, TypeScript, and Vite SPA for the TalkTally Spring Boot API. The current UI is a functional foundation; the approved production visual design will be integrated separately.

## Requirements

- Node.js 24 LTS
- The TalkTally backend at `http://localhost:8080`

## Setup

```bash
cp .env.example .env
npm ci
```

Frontend configuration is public browser configuration:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Never put API keys, JWT signing secrets, database credentials, or other secrets in `VITE_*` variables.

For local cross-origin requests, start the backend with:

```dotenv
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Then run:

```bash
npm run dev
```

The frontend runs at `http://localhost:5173`.

## Validation

```bash
npm run lint
npm run typecheck
npm run test:run
npm run build
```
