# CP-053 ShiftGenie: Constraint-Based Workforce Roster Generator

## Team
Kajal Sah (1AT24CS086)
Kavya V.
,Java track

## How to run
javac ShiftGenie.java
java ShiftGenie

## Features that work
1. Record staff with availability across 21 weekly slots and maximum weekly hours
2. Configure shift staffing headcount requirements per day and shift
3. Generate a constraint-based weekly roster respecting availability, max hours, and minimum rest gaps
4. Distribute night shifts evenly across eligible staff
5. Report all understaffed shifts with exact headcount shortages
6. Stretch: Validate and process one-way and mutual shift swap requests
7. Stretch: Enforce configurable minimum rest gaps between consecutive shifts
8. Stretch: Persist and export staff data, weekly rosters, and workload summaries to CSV files

## Sample run
=======================================================
   SHIFTGENIE: CONSTRAINT-BASED WORKFORCE ROSTER
=======================================================
1.  Add New Staff Member (Core 1)
2.  Update Staff Availability or Max Hours (Core 1)
3.  View All Staff & Availability (Core 1)
4.  Configure Shift Staffing Requirements (Core 2)
5.  Configure Minimum Rest Gap Rule (Stretch 2)
6.  Generate Weekly Roster (Core 3 & 4)
7.  View Full Weekly Roster (Core 3)
8.  View Understaffed Shifts Report (Core 5)
9.  View Staff Workload & Night Distribution (Core 4)
10. Swap Shifts Between Two Staff Members (Stretch 1)
11. Export All Reports & Data to CSV Files (Stretch)
12. Reload Staff Data from CSV File (Stretch)
0.  Exit
Enter your choice (0-12): 6

[SUCCESS] Weekly roster generated (Minimum Rest Gap: 8 hrs).

==================== WEEKLY ROSTER ====================

--- MONDAY ---
  Morning (06:00-14:00)  | Req: 2 | Filled (2): Aarav(S101), Diya(S102)
  Evening (14:00-22:00)  | Req: 2 | Filled (2): Rohan(S103), Meera(S104)
  Night   (22:00-06:00)  | Req: 1 | Filled (1): Kabir(S105)

[CSV SAVED] weekly_roster.csv
[CSV SAVED] staff_summary.csv
