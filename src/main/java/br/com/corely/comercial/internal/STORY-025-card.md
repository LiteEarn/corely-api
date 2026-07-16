# STORY-025: Encerramento da Aula (Class Session Lifecycle)

## Description
Control the lifecycle of a ClassSession with status transitions: SCHEDULED → IN_PROGRESS → FINISHED or SCHEDULED → CANCELLED.

## Status Transition Rules
- SCHEDULED → IN_PROGRESS (start)
- IN_PROGRESS → FINISHED (finish)
- SCHEDULED → CANCELLED (cancel)
- Terminal states (FINISHED, CANCELLED) are irreversible

## API Endpoints
- `POST /comercial/class-sessions/{id}/start` - Start a session
- `POST /comercial/class-sessions/{id}/finish` - Finish a session

## Side Effects on FINISHED
- Attendance cannot be created/altered
- Booking cannot be created or cancelled
- Wait List cannot promote students

## Architecture
- Transition rules centralized in `ClassSession` entity (`start()`, `finish()`, `cancel()` methods)
- Services consult entity methods (`isScheduled()`, `isInProgress()`, `isFinished()`, `isCancelled()`, `isTerminal()`)
- No scattering of status validation across services

## Dependencies
- STORY-021 (ClassSession entity)
- STORY-022 (Booking entity)
- STORY-023 (Attendance entity)
- STORY-024 (Wait List entity)

## Status
Completed
