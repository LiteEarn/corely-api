# STORY-023: Attendance on Comercial Module

## Description
Implement attendance tracking for the comercial module. Teachers can mark students as present or absent for class sessions.

## Requirements
- Register attendance for a booking in a class session
- Bulk attendance registration
- List attendance by session, booking, or student
- Validate session is in progress before allowing attendance
- Validate booking belongs to the session and is active

## API Endpoints
- `POST /comercial/attendances/sessions/{sessionId}` - Register single attendance
- `POST /comercial/attendances/bulk` - Bulk attendance registration
- `GET /comercial/attendances/sessions/{sessionId}` - List by session
- `GET /comercial/attendances/bookings/{bookingId}` - List by booking
- `GET /comercial/attendances/students/{studentId}` - List by student

## Dependencies
- STORY-022 (Booking entity)
- STORY-021 (ClassSession entity)
- STORY-020 (ScheduleSlot entity)

## Status
Completed
