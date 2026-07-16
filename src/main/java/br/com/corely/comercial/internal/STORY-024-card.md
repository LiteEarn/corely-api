# STORY-024: Lista de Espera (Wait List)

## Description
Allow students to be placed on a wait list when a ClassSession reaches full capacity. When a slot is freed, the next student is automatically promoted to a CONFIRMED Booking.

## Requirements
- Add to wait list when bookedCount >= capacity
- Sequential position (1, 2, 3, ...)
- No duplicate entries per session+student
- No inclusion if student already has a CONFIRMED Booking for the session
- Automatic promotion on Booking cancellation
- Logical deletion
- Multi-tenant isolation

## API Endpoints
- `POST /comercial/wait-list` - Add to wait list
- `GET /comercial/wait-list` - List wait list entries
- `GET /comercial/wait-list/{id}` - Get by ID
- `DELETE /comercial/wait-list/{id}` - Remove from wait list

## Dependencies
- STORY-021 (ClassSession entity)
- STORY-022 (Booking entity)

## Architecture
- BookingService.createBooking() extracted as reusable internal method
- WaitListService promotes on Booking cancellation via BookingService.createBooking()
- Pessimistic locking (PESSIMISTIC_WRITE) for concurrent promotion

## Status
In Development
