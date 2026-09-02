# TalkTally frontend

Production React 19, TypeScript, and Vite SPA for the TalkTally Spring Boot API. The frontend provides the responsive, bilingual product experience used by the hosted application.

## Product surface

- JWT-backed registration, sign-in, protected routes, and session handling.
- Responsive dashboard with financial summaries, category breakdowns, monthly cash flow, and recent activity.
- Transaction creation, editing, filtering, pagination, installment schedules, and detail views.
- Reimbursement tracking through the Owed to Me experience.
- Text and voice assistant flows in English and Brazilian Portuguese.
- Public `/backend-status` page that polls the backend while a cold Render service wakes.
- Native voice-reply playback through a reusable `HTMLAudioElement`, with one autoplay attempt per generated reply and visible native controls as fallback.
- Persistent theme and locale preferences in the browser.

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

## Voice playback

Recording primes a reusable native audio element during the user gesture. When a synthesized reply arrives, the browser attempts to play that generated URL at most once. Visible native controls remain available for manual replay or when autoplay is unavailable.

This native playback path replaced Web Audio reply playback after mobile Firefox exposed duplicate/echoed TTS playback.

## Validation

```bash
npm run lint
npm run typecheck
npm run test:run
npm run build
```

The current default frontend suite contains **241 tests across 34 files**.
