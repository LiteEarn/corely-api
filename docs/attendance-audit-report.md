# Attendance Backend Flow Audit Report

## Project: Corely
## Date: 2026-06-21
## Priority: Critical

---

## 1. Current Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ATTENDANCE FLOW                                    │
└─────────────────────────────────────────────────────────────────────────────┘

1. USER SELECTS CLASS GROUP
   │
   ▼
2. LOAD ACTIVE ENROLLED STUDENTS
   │
   │  GET /attendance/class-group/{classGroupId}/date/{attendanceDate}
   │
   │  AttendanceService.findByClassGroupIdAndAttendanceDate()
   │  │
   │  ├─► Query: enrollmentRepository.findByClassGroupIdAndActiveTrueAndStudentActiveTrueAndClassGroupActiveTrue()
   │  │   └─► Returns ONLY active enrollments with active students in active class groups
   │  │
   │  └─► For each enrollment:
   │      ├─► Check if attendance exists for student + date
   │      ├─► If exists: return AttendanceResponse with attendance data
   │      └─► If not exists: return AttendanceResponse with default values (present=false, id=null)
   │
   ▼
3. ATTENDANCE SCREEN DISPLAYS
   │
   │  Shows list of students with:
   │  - Student name and ID
   │  - Attendance ID (if record exists) or null (if no record)
   │  - Present status (from record or default false)
   │  - Notes (from record or null)
   │
   ▼
4. USER MARKS ATTENDANCE
   │
   │  Frontend builds AttendanceBulkRequest with:
   │  - studioId, classGroupId, attendanceDate
   │  - List of AttendanceItemRequest (studentId, present, notes)
   │
   ▼
5. SAVE ATTENDANCE
   │
   │  POST /attendance/bulk
   │
   │  AttendanceService.createBulk()
   │  │
   │  └─► For each AttendanceItemRequest:
   │      ├─► Validate student is active
   │      ├─► Validate enrollment is active
   │      ├─► Check if attendance exists for student + date
   │      ├─► If exists: UPDATE existing record
   │      └─► If not exists: CREATE new record
   │
   ▼
6. RELOAD ATTENDANCE
   │
   │  GET /attendance/class-group/{classGroupId}/date/{attendanceDate}
   │
   │  Returns previously saved attendance records
   │  (Same as step 2, but now attendance records exist)
   │
   ▼
7. ATTENDANCE SCREEN REFRESHES
   │
   │  Shows updated attendance data with:
   │  - Attendance ID (now populated)
   │  - Present status (from saved record)
   │  - Notes (from saved record)
```

---

## 2. Identified Issues

### Issue #1: Bulk Save Didn't Support Updates (FIXED ✓)

**Severity:** Critical

**Description:** The `createBulk` method was throwing an exception when attendance already existed for a student on a given date. This prevented teachers from re-saving attendance for the same date (e.g., to correct mistakes or update notes).

**Before:**
```java
attendanceRepository.findByStudentIdAndClassGroupIdAndAttendanceDate(...)
    .ifPresent(attendance -> {
        throw new IllegalArgumentException("Attendance already exists for student...");
    });
```

**After:**
```java
Attendance attendance = attendanceRepository.findByStudentIdAndClassGroupIdAndAttendanceDate(...)
    .orElseGet(Attendance::new);

if (attendance.getId() == null) {
    // New attendance - set all fields
    attendance.setStudio(studio);
    attendance.setStudent(student);
    attendance.setClassGroup(classGroup);
    attendance.setAttendanceDate(request.getAttendanceDate());
}

// Update attendance fields (whether new or existing)
attendance.setPresent(itemRequest.getPresent());
attendance.setNotes(itemRequest.getNotes());
```

**Impact:** Teachers can now update attendance by re-submitting the bulk request for the same date.

---

### Issue #2: `findByClassGroupId` Uses Incorrect Query (DOCUMENTED)

**Severity:** Medium

**Description:** The `findByClassGroupId` method only returns existing attendance records, not all active students. This endpoint is not aligned with the expected behavior of the attendance screen.

**Current Implementation:**
```java
public List<AttendanceResponse> findByClassGroupId(UUID classGroupId) {
    return attendanceRepository.findByClassGroupIdAndStudentActiveTrue(classGroupId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
}
```

**Problem:** This returns ONLY attendance records that exist, not the list of active students who should have attendance.

**Recommendation:** This endpoint should be deprecated or removed in favor of `findByClassGroupIdAndAttendanceDate` which correctly returns all active enrolled students.

---

### Issue #3: Missing Database Constraint for Duplicate Prevention (DOCUMENTED)

**Severity:** Low

**Description:** There's no unique constraint on `(student_id, class_group_id, attendance_date)` to prevent duplicates at the database level.

**Current State:** Duplicate prevention is handled at the application level only.

**Recommendation:** Add a unique constraint to the database:
```sql
ALTER TABLE attendances ADD CONSTRAINT uk_attendance_student_class_date 
    UNIQUE (student_id, class_group_id, attendance_date);
```

---

### Issue #4: Date Parsing in `findByClassGroupIdAndAttendanceDate` (DOCUMENTED)

**Severity:** Low

**Description:** The method accepts a `String` date and parses it using `LocalDate.parse()`, which expects ISO-8601 format (yyyy-MM-dd). Invalid formats will throw a `DateTimeParseException`.

**Current Implementation:**
```java
public List<AttendanceResponse> findByClassGroupIdAndAttendanceDate(UUID classGroupId, String attendanceDate) {
    java.time.LocalDate date = java.time.LocalDate.parse(attendanceDate);
    // ...
}
```

**Recommendation:** This is acceptable behavior as the frontend should always send ISO-8601 format. However, consider adding explicit error handling for better error messages.

---

## 3. Files Changed

| File | Changes |
|------|---------|
| `src/main/java/br/com/corely/attendance/AttendanceService.java` | Modified `createBulk()` to support upsert (create or update) behavior |
| `src/test/java/br/com/corely/attendance/AttendanceServiceTest.java` | Added tests for bulk update behavior |

---

## 4. Fixes Implemented

### Fix #1: Bulk Save Now Supports Updates

The `createBulk` method now:
1. Checks if attendance exists for the student + date combination
2. If exists: updates the existing record (preserves ID, updates present/notes)
3. If not exists: creates a new record

This allows teachers to:
- Correct mistakes in attendance
- Update notes for students
- Re-submit attendance for the same date

### Test Coverage Added

```java
@Test
void createBulk_updatesExistingAttendance() { ... }

@Test
void createBulk_createsNewAndUpdatesExisting() { ... }
```

---

## 5. Test Scenarios Executed

| Test | Status |
|------|--------|
| `create_attendanceSavedWhenAllActive` | ✅ PASS |
| `create_throwsExceptionWhenStudentInactive` | ✅ PASS |
| `create_throwsExceptionWhenEnrollmentInactive` | ✅ PASS |
| `create_throwsExceptionWhenClassGroupInactive` | ✅ PASS |
| `historicalAttendanceRemainsUnchanged` | ✅ PASS |
| `findByClassGroupIdAndAttendanceDate_returnsStudentsWhenNoAttendanceRecords` | ✅ PASS |
| `findByClassGroupIdAndAttendanceDate_returnsExistingAttendanceWhenRecorded` | ✅ PASS |
| `findByClassGroupIdAndAttendanceDate_excludesInactiveStudents` | ✅ PASS |
| `findByClassGroupIdAndAttendanceDate_excludesInactiveEnrollments` | ✅ PASS |
| `findByClassGroupIdAndAttendanceDate_excludesInactiveClassGroups` | ✅ PASS |
| `findByClassGroupIdAndAttendanceDate_returnsEmptyListWhenNoActiveEnrollments` | ✅ PASS |
| `findByClassGroupIdAndAttendanceDate_returnsMultipleStudentsWithMixedAttendance` | ✅ PASS |
| `createBulk_updatesExistingAttendance` | ✅ PASS (NEW) |
| `createBulk_createsNewAndUpdatesExisting` | ✅ PASS (NEW) |

---

## 6. Validation Summary

### Active/Inactive Lifecycle Rules ✅

| Entity | Validation | Status |
|--------|------------|--------|
| Student.active | Validated in `create()`, `createBulk()`, `update()` | ✅ Working |
| Enrollment.active | Validated in `create()`, `createBulk()`, `update()` | ✅ Working |
| ClassGroup.active | Validated in `create()`, `createBulk()`, `update()` | ✅ Working |

### Attendance Behavior ✅

| Behavior | Status |
|----------|--------|
| Duplicate attendance prevention | ✅ Working (application level) |
| Attendance update behavior | ✅ Fixed (bulk now supports updates) |
| Bulk save behavior | ✅ Fixed (supports create and update) |
| Attendance reload behavior | ✅ Working (returns saved records) |
| Screen works with empty attendances table | ✅ Working (returns students with default values) |

---

## 7. Recommendations

1. **Add Database Constraint:** Consider adding a unique constraint on `(student_id, class_group_id, attendance_date)` for data integrity.

2. **Deprecate `findByClassGroupId`:** This endpoint doesn't align with the attendance screen workflow. Consider deprecating it in favor of `findByClassGroupIdAndAttendanceDate`.

3. **Add Error Handling for Date Parsing:** Consider adding explicit error handling for `DateTimeParseException` in `findByClassGroupIdAndAttendanceDate`.

4. **Consider Soft Delete:** Instead of hard deleting attendance records, consider adding an `active` flag or `deletedAt` timestamp for audit purposes.

---

## 8. Conclusion

The attendance backend flow is now consistent and follows the expected business process:

1. ✅ User selects a Class Group
2. ✅ System loads ACTIVE enrolled students
3. ✅ Existing attendance for the selected date is loaded if present
4. ✅ User marks attendance
5. ✅ Attendance is saved using bulk endpoint (now supports updates)
6. ✅ Reloading the same date shows previously saved attendance

The critical issue with bulk save not supporting updates has been fixed. All tests pass successfully.
