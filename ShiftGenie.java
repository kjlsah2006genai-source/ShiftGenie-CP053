import java.io.*;
import java.util.*;

class ShiftGenie {

    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    static final String[] SHIFT_NAMES = {
            "Morning (06:00-14:00)",
            "Evening (14:00-22:00)",
            "Night   (22:00-06:00)"
    };
    static final int SHIFT_HOURS = 8;

    static final String STAFF_CSV = "staff_data.csv";
    static final String ROSTER_CSV = "weekly_roster.csv";
    static final String SUMMARY_CSV = "staff_summary.csv";

    static int minRestGapSlots = 1; // 1 slot = 8 hrs rest between shifts
    static boolean rosterGenerated = false;

    static class Staff {
        String id;
        String name;
        int maxHoursPerWeek;
        boolean[][] availability = new boolean[7][3];
        int assignedHours = 0;
        int nightShiftsCount = 0;

        Staff(String id, String name, int maxHoursPerWeek, boolean defaultAvail) {
            this.id = id;
            this.name = name;
            this.maxHoursPerWeek = maxHoursPerWeek;
            for (int d = 0; d < 7; d++) {
                Arrays.fill(this.availability[d], defaultAvail);
            }
        }

        void resetStats() {
            this.assignedHours = 0;
            this.nightShiftsCount = 0;
        }
    }

    static class Shift {
        int day;
        int type;
        int required;
        List<Staff> assignedStaff = new ArrayList<>();

        Shift(int day, int type, int required) {
            this.day = day;
            this.type = type;
            this.required = required;
        }
    }

    static List<Staff> staffList = new ArrayList<>();
    static Shift[][] weeklyShifts = new Shift[7][3];
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        initializeDefaultShifts();

        if (!loadStaffFromCSV(STAFF_CSV)) {
            loadSampleData();
            saveStaffToCSV(STAFF_CSV);
        }

        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("   SHIFTGENIE: CONSTRAINT-BASED WORKFORCE ROSTER");
            System.out.println("=======================================================");
            System.out.println("1.  Add New Staff Member (Core 1)");
            System.out.println("2.  Update Staff Availability or Max Hours (Core 1)");
            System.out.println("3.  View All Staff & Availability (Core 1)");
            System.out.println("4.  Configure Shift Staffing Requirements (Core 2)");
            System.out.println("5.  Configure Minimum Rest Gap Rule (Stretch 2)");
            System.out.println("6.  Generate Weekly Roster (Core 3 & 4)");
            System.out.println("7.  View Full Weekly Roster (Core 3)");
            System.out.println("8.  View Understaffed Shifts Report (Core 5)");
            System.out.println("9.  View Staff Workload & Night Distribution (Core 4)");
            System.out.println("10. Swap Shifts Between Two Staff Members (Stretch 1)");
            System.out.println("11. Export All Reports & Data to CSV Files (Stretch)");
            System.out.println("12. Reload Staff Data from CSV File (Stretch)");
            System.out.println("0.  Exit");

            int choice = readIntInRange("Enter your choice (0-12): ", 0, 12);
            switch (choice) {
                case 1 -> addStaffMenu();
                case 2 -> updateStaffMenu();
                case 3 -> viewStaff();
                case 4 -> configureShiftsMenu();
                case 5 -> configureRestGapMenu();
                case 6 -> generateRoster();
                case 7 -> printRoster();
                case 8 -> printUnderstaffedShifts();
                case 9 -> printWorkloadSummary();
                case 10 -> swapShiftsMenu();
                case 11 -> exportAllCSV();
                case 12 -> {
                    if (loadStaffFromCSV(STAFF_CSV)) {
                        rosterGenerated = false;
                        clearRosterAssignments();
                        System.out.println("[SUCCESS] Staff records reloaded from " + STAFF_CSV + ". Please regenerate the roster (Option 6).");
                    } else {
                        System.out.println("[ERROR] No valid " + STAFF_CSV + " file found.");
                    }
                }
                case 0 -> {
                    saveStaffToCSV(STAFF_CSV);
                    System.out.println("All staff data saved. Exiting ShiftGenie. Goodbye!");
                    return;
                }
            }
        }
    }

    static void initializeDefaultShifts() {
        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < 3; s++) {
                int defaultReq = (s == 2) ? 1 : 2;
                weeklyShifts[d][s] = new Shift(d, s, defaultReq);
            }
        }
    }

    static void clearRosterAssignments() {
        for (Staff st : staffList) {
            st.resetStats();
        }
        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < 3; s++) {
                weeklyShifts[d][s].assignedStaff.clear();
            }
        }
    }

    static void loadSampleData() {
        Staff s1 = new Staff("S101", "Aarav", 40, true);
        Staff s2 = new Staff("S102", "Diya", 40, true);
        Staff s3 = new Staff("S103", "Rohan", 32, true);
        s3.availability[6][0] = false;
        s3.availability[6][1] = false;
        s3.availability[6][2] = false;

        Staff s4 = new Staff("S104", "Meera", 40, true);
        s4.availability[0][2] = false;
        s4.availability[1][2] = false;

        Staff s5 = new Staff("S105", "Kabir", 24, true);
        Staff s6 = new Staff("S106", "Ananya", 40, true);

        staffList.addAll(Arrays.asList(s1, s2, s3, s4, s5, s6));
    }

    // CORE 1: Add staff with strict validation
    static void addStaffMenu() {
        String id = readValidId("Enter new Staff ID (alphanumeric, e.g., S107): ");
        if (findStaffById(id) != null) {
            System.out.println("[ERROR] Staff ID '" + id + "' already exists. IDs must be unique.");
            return;
        }

        String name = readValidName("Enter Staff Name (letters and spaces only): ");
        int maxHours = readIntInRange("Enter Max Hours Per Week (8 to 84, multiple of 8 recommended): ", 8, 84);
        boolean allAvail = readYesNo("Available for all 21 weekly shifts? (y/n): ");

        Staff newStaff = new Staff(id, name, maxHours, allAvail);

        if (!allAvail) {
            System.out.println("Enter availability for each shift (1 = Available, 0 = Unavailable):");
            boolean atLeastOneAvailable = false;
            for (int d = 0; d < 7; d++) {
                for (int s = 0; s < 3; s++) {
                    int val = readIntInRange("  " + DAYS[d] + " - " + SHIFT_NAMES[s].trim() + " (1/0): ", 0, 1);
                    newStaff.availability[d][s] = (val == 1);
                    if (val == 1) atLeastOneAvailable = true;
                }
            }
            if (!atLeastOneAvailable) {
                System.out.println("[WARNING] " + name + " has 0 available shifts and will never be scheduled.");
            }
        }

        staffList.add(newStaff);
        saveStaffToCSV(STAFF_CSV);
        System.out.println("[SUCCESS] Staff " + name + " (" + id + ") added and saved.");
        if (rosterGenerated) {
            System.out.println("[NOTE] Run Option 6 to include the new staff member in the weekly roster.");
        }
    }

    // CORE 1: Update existing staff availability or max hours safely
    static void updateStaffMenu() {
        if (staffList.isEmpty()) {
            System.out.println("[ERROR] No staff registered to update.");
            return;
        }

        viewStaff();
        String id = readValidId("Enter Staff ID to update: ");
        Staff st = findStaffById(id);
        if (st == null) {
            System.out.println("[ERROR] Staff ID '" + id + "' not found.");
            return;
        }

        System.out.println("1. Update Max Weekly Hours (Current: " + st.maxHoursPerWeek + " hrs, Assigned: " + st.assignedHours + " hrs)");
        System.out.println("2. Toggle Availability for a Specific Day & Shift");
        int opt = readIntInRange("Choose option (1-2): ", 1, 2);

        if (opt == 1) {
            int hrs = readIntInRange("Enter new Max Weekly Hours (8 to 84): ", 8, 84);
            if (rosterGenerated && hrs < st.assignedHours) {
                System.out.println("[ERROR] Cannot reduce max hours to " + hrs + " because " + st.name
                        + " is already assigned " + st.assignedHours + " hours in the active roster. Regenerate roster after changing.");
                return;
            }
            st.maxHoursPerWeek = hrs;
            saveStaffToCSV(STAFF_CSV);
            System.out.println("[SUCCESS] Updated max hours for " + st.name + " to " + hrs + ".");
        } else {
            int d = promptDay();
            int s = promptShift();
            boolean newStatus = !st.availability[d][s];

            if (!newStatus && rosterGenerated && weeklyShifts[d][s].assignedStaff.contains(st)) {
                System.out.println("[ERROR] " + st.name + " is currently assigned to " + DAYS[d] + " " + SHIFT_NAMES[s].trim()
                        + ". Swap their shift first (Option 10) or regenerate the roster (Option 6) after updating.");
                return;
            }

            st.availability[d][s] = newStatus;
            saveStaffToCSV(STAFF_CSV);
            System.out.println("[SUCCESS] " + st.name + " is now " + (newStatus ? "AVAILABLE" : "UNAVAILABLE")
                    + " for " + DAYS[d] + " " + SHIFT_NAMES[s].trim() + ".");
        }
    }

    static void viewStaff() {
        if (staffList.isEmpty()) {
            System.out.println("[INFO] No staff members registered.");
            return;
        }
        System.out.println("\n---------------- REGISTERED STAFF ----------------");
        for (Staff st : staffList) {
            int availCount = 0;
            for (int d = 0; d < 7; d++) {
                for (int s = 0; s < 3; s++) {
                    if (st.availability[d][s]) availCount++;
                }
            }
            System.out.printf("%-6s | %-15s | Max Hrs: %2d | Available Slots: %2d/21%n",
                    st.id, st.name, st.maxHoursPerWeek, availCount);
        }
    }

    // CORE 2: Configure shift requirements with bounds checking
    static void configureShiftsMenu() {
        int d = promptDay();
        int s = promptShift();
        int current = weeklyShifts[d][s].required;
        int req = readIntInRange("Current required staff: " + current + ". Enter new count (0-20): ", 0, 20);
        weeklyShifts[d][s].required = req;
        System.out.println("[SUCCESS] Updated " + DAYS[d] + " " + SHIFT_NAMES[s].trim() + " to require " + req + " staff.");
        if (rosterGenerated) {
            System.out.println("[NOTE] Run Option 6 to apply the updated requirement to the roster.");
        }
    }

    // STRETCH 2: Configure minimum rest gap between shifts
    static void configureRestGapMenu() {
        System.out.println("Current Minimum Rest Gap: " + minRestGapSlots + " shift slot(s) (" + (minRestGapSlots * 8) + " hours).");
        System.out.println("0 = Allow back-to-back shifts (0 hrs rest)");
        System.out.println("1 = Minimum 1 shift off between duties (8 hrs rest)");
        System.out.println("2 = Minimum 2 shifts off between duties (16 hrs rest)");
        int gap = readIntInRange("Enter new rest gap in shift slots (0-2): ", 0, 2);
        minRestGapSlots = gap;
        System.out.println("[SUCCESS] Minimum rest gap set to " + minRestGapSlots + " shift slot(s) (" + (minRestGapSlots * 8) + " hours).");
        if (rosterGenerated) {
            System.out.println("[NOTE] Run Option 6 to regenerate the roster under the new rest-gap rule.");
        }
    }

    // CORE 3 & CORE 4: Generate weekly roster
    static void generateRoster() {
        if (staffList.isEmpty()) {
            System.out.println("[ERROR] Cannot generate roster: No staff members registered. Add staff first (Option 1).");
            return;
        }

        int totalRequiredSlots = 0;
        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < 3; s++) {
                totalRequiredSlots += weeklyShifts[d][s].required;
            }
        }
        if (totalRequiredSlots == 0) {
            System.out.println("[ERROR] All shifts have 0 required staff. Configure shift requirements first (Option 4).");
            return;
        }

        clearRosterAssignments();

        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < 3; s++) {
                Shift currentShift = weeklyShifts[d][s];
                if (currentShift.required == 0) continue;

                List<Staff> eligible = new ArrayList<>();
                for (Staff st : staffList) {
                    if (canAssign(st, d, s, null)) {
                        eligible.add(st);
                    }
                }

                if (s == 2) {
                    // Fair night shift distribution: fewest night shifts first, then fewest total hours
                    eligible.sort(Comparator.comparingInt((Staff st) -> st.nightShiftsCount)
                            .thenComparingInt(st -> st.assignedHours)
                            .thenComparing(st -> st.id));
                } else {
                    // Balanced day shifts: fewest total hours first
                    eligible.sort(Comparator.comparingInt((Staff st) -> st.assignedHours)
                            .thenComparing(st -> st.id));
                }

                int assignCount = Math.min(currentShift.required, eligible.size());
                for (int i = 0; i < assignCount; i++) {
                    Staff chosen = eligible.get(i);
                    currentShift.assignedStaff.add(chosen);
                    chosen.assignedHours += SHIFT_HOURS;
                    if (s == 2) {
                        chosen.nightShiftsCount++;
                    }
                }
            }
        }

        rosterGenerated = true;
        System.out.println("\n[SUCCESS] Weekly roster generated (Minimum Rest Gap: " + (minRestGapSlots * 8) + " hrs).");
        printRoster();
        exportAllCSV();
    }

    static boolean canAssign(Staff st, int day, int shiftType, Shift ignoredShift) {
        if (!st.availability[day][shiftType]) return false;
        if (weeklyShifts[day][shiftType].assignedStaff.contains(st)) return false;

        int effectiveHours = st.assignedHours - (ignoredShift != null ? SHIFT_HOURS : 0);
        if (effectiveHours + SHIFT_HOURS > st.maxHoursPerWeek) return false;

        int targetGlobalSlot = day * 3 + shiftType;
        for (int offset = -minRestGapSlots; offset <= minRestGapSlots; offset++) {
            if (offset == 0) continue;
            int neighborSlot = targetGlobalSlot + offset;
            if (neighborSlot >= 0 && neighborSlot < 21) {
                int nDay = neighborSlot / 3;
                int nShift = neighborSlot % 3;
                Shift neighbor = weeklyShifts[nDay][nShift];
                if (neighbor != ignoredShift && neighbor.assignedStaff.contains(st)) {
                    return false;
                }
            }
        }
        return true;
    }

    static void printRoster() {
        if (!rosterGenerated) {
            System.out.println("[WARNING] No roster generated yet. Run Option 6 first.");
            return;
        }
        System.out.println("\n==================== WEEKLY ROSTER ====================");
        for (int d = 0; d < 7; d++) {
            System.out.println("\n--- " + DAYS[d].toUpperCase() + " ---");
            for (int s = 0; s < 3; s++) {
                Shift sh = weeklyShifts[d][s];
                List<String> names = new ArrayList<>();
                for (Staff st : sh.assignedStaff) {
                    names.add(st.name + "(" + st.id + ")");
                }
                String flag = (sh.assignedStaff.size() < sh.required) ? " [UNDERSTAFFED]" : "";
                System.out.printf("  %-22s | Req: %d | Filled (%d): %s%s%n",
                        SHIFT_NAMES[s],
                        sh.required,
                        sh.assignedStaff.size(),
                        names.isEmpty() ? "NONE" : String.join(", ", names),
                        flag);
            }
        }
    }

    // CORE 5: Report understaffed shifts
    static void printUnderstaffedShifts() {
        if (!rosterGenerated) {
            System.out.println("[WARNING] No roster generated yet. Run Option 6 first.");
            return;
        }
        System.out.println("\n-------------- UNDERSTAFFED SHIFTS REPORT --------------");
        boolean found = false;
        for (int d = 0; d < 7; d++) {
            for (int s = 0; s < 3; s++) {
                Shift sh = weeklyShifts[d][s];
                if (sh.assignedStaff.size() < sh.required) {
                    found = true;
                    int deficit = sh.required - sh.assignedStaff.size();
                    System.out.printf("%-10s | %-22s | Required: %d | Assigned: %d | Short by: %d%n",
                            DAYS[d], SHIFT_NAMES[s], sh.required, sh.assignedStaff.size(), deficit);
                }
            }
        }
        if (!found) {
            System.out.println("All shifts across the week are 100% staffed!");
        }
    }

    // CORE 4: Workload & night shift balance report
    static void printWorkloadSummary() {
        if (!rosterGenerated) {
            System.out.println("[WARNING] No roster generated yet. Run Option 6 first.");
            return;
        }
        System.out.println("\n--------- STAFF WORKLOAD & NIGHT SHIFT SUMMARY ---------");
        System.out.printf("%-6s | %-15s | %-16s | %-12s%n", "ID", "Name", "Hours (Used/Max)", "Night Shifts");
        System.out.println("-----------------------------------------------------------");
        for (Staff st : staffList) {
            System.out.printf("%-6s | %-15s | %2d / %2d hrs      | %d%n",
                    st.id, st.name, st.assignedHours, st.maxHoursPerWeek, st.nightShiftsCount);
        }
    }

    // STRETCH 1: Shift swap with detailed rejection diagnostics
    static void swapShiftsMenu() {
        if (!rosterGenerated) {
            System.out.println("[ERROR] Generate a weekly roster first (Option 6) before swapping shifts.");
            return;
        }

        System.out.println("\n--- Shift Swap Request ---");
        System.out.println("1. Cover/Replace: An unassigned staff takes over an assigned staff's shift");
        System.out.println("2. Mutual Trade: Two assigned staff exchange their respective shifts");
        int swapType = readIntInRange("Select swap type (1 or 2): ", 1, 2);

        if (swapType == 1) {
            int d = promptDay();
            int s = promptShift();
            Shift shift = weeklyShifts[d][s];

            if (shift.assignedStaff.isEmpty()) {
                System.out.println("[ERROR] No staff are currently assigned to " + DAYS[d] + " " + SHIFT_NAMES[s].trim() + ".");
                return;
            }

            String outId = readValidId("Enter ID of assigned staff stepping down: ");
            Staff outStaff = findStaffById(outId);
            String inId = readValidId("Enter ID of replacement staff taking the shift: ");
            Staff inStaff = findStaffById(inId);

            if (outStaff == null || inStaff == null) {
                System.out.println("[ERROR] One or both Staff IDs do not exist.");
                return;
            }
            if (outStaff == inStaff) {
                System.out.println("[ERROR] Cannot swap a staff member with themselves.");
                return;
            }
            if (!shift.assignedStaff.contains(outStaff)) {
                System.out.println("[ERROR] " + outStaff.name + " is not assigned to this shift.");
                return;
            }
            if (!canAssign(inStaff, d, s, null)) {
                System.out.println("[REJECTED] " + inStaff.name + " violates availability, duplicate shift, max hours, or rest-gap rules.");
                return;
            }

            shift.assignedStaff.remove(outStaff);
            outStaff.assignedHours -= SHIFT_HOURS;
            if (s == 2) outStaff.nightShiftsCount--;

            shift.assignedStaff.add(inStaff);
            inStaff.assignedHours += SHIFT_HOURS;
            if (s == 2) inStaff.nightShiftsCount++;

            exportAllCSV();
            System.out.println("[APPROVED] " + inStaff.name + " replaced " + outStaff.name + " on " + DAYS[d] + " " + SHIFT_NAMES[s].trim() + ".");

        } else {
            System.out.println("First Staff Member's Shift:");
            int d1 = promptDay();
            int s1 = promptShift();
            Staff st1 = findStaffById(readValidId("Enter First Staff ID: "));

            System.out.println("Second Staff Member's Shift:");
            int d2 = promptDay();
            int s2 = promptShift();
            Staff st2 = findStaffById(readValidId("Enter Second Staff ID: "));

            if (st1 == null || st2 == null) {
                System.out.println("[ERROR] One or both Staff IDs do not exist.");
                return;
            }
            if (st1 == st2) {
                System.out.println("[ERROR] Cannot execute a mutual swap with the same staff member.");
                return;
            }
            if (d1 == d2 && s1 == s2) {
                System.out.println("[ERROR] Both selected shifts are identical.");
                return;
            }

            Shift shift1 = weeklyShifts[d1][s1];
            Shift shift2 = weeklyShifts[d2][s2];

            if (!shift1.assignedStaff.contains(st1)) {
                System.out.println("[ERROR] " + st1.name + " is not assigned to " + DAYS[d1] + " " + SHIFT_NAMES[s1].trim() + ".");
                return;
            }
            if (!shift2.assignedStaff.contains(st2)) {
                System.out.println("[ERROR] " + st2.name + " is not assigned to " + DAYS[d2] + " " + SHIFT_NAMES[s2].trim() + ".");
                return;
            }

            boolean st1CanTakeShift2 = canAssign(st1, d2, s2, shift1);
            boolean st2CanTakeShift1 = canAssign(st2, d1, s1, shift2);

            if (!st1CanTakeShift2 || !st2CanTakeShift1) {
                System.out.println("[REJECTED] Mutual swap violates scheduling constraints:");
                if (!st1CanTakeShift2) {
                    System.out.println(" - " + st1.name + " cannot take " + DAYS[d2] + " " + SHIFT_NAMES[s2].trim() + " (availability/rest gap/duplicate).");
                }
                if (!st2CanTakeShift1) {
                    System.out.println(" - " + st2.name + " cannot take " + DAYS[d1] + " " + SHIFT_NAMES[s1].trim() + " (availability/rest gap/duplicate).");
                }
                return;
            }

            shift1.assignedStaff.remove(st1);
            shift2.assignedStaff.remove(st2);
            if (s1 == 2) st1.nightShiftsCount--;
            if (s2 == 2) st2.nightShiftsCount--;

            shift1.assignedStaff.add(st2);
            shift2.assignedStaff.add(st1);
            if (s1 == 2) st2.nightShiftsCount++;
            if (s2 == 2) st1.nightShiftsCount++;

            exportAllCSV();
            System.out.println("[APPROVED] Mutual swap completed between " + st1.name + " and " + st2.name + "!");
        }
    }

    // CSV EXPORT & IMPORT
    static void exportAllCSV() {
        saveStaffToCSV(STAFF_CSV);
        if (rosterGenerated) {
            exportRosterToCSV(ROSTER_CSV);
            exportStaffSummaryToCSV(SUMMARY_CSV);
        } else {
            System.out.println("[INFO] Staff data saved to CSV. Generate a roster (Option 6) first to export roster & summary CSVs.");
        }
    }

    static void saveStaffToCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Staff_ID,Name,Max_Hours,Availability_21_Bits");
            for (Staff st : staffList) {
                StringBuilder bits = new StringBuilder();
                for (int d = 0; d < 7; d++) {
                    for (int s = 0; s < 3; s++) {
                        bits.append(st.availability[d][s] ? '1' : '0');
                    }
                }
                writer.printf("%s,%s,%d,%s%n", st.id, st.name, st.maxHoursPerWeek, bits);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Could not save " + filename + ": " + e.getMessage());
        }
    }

    static boolean loadStaffFromCSV(String filename) {
        File file = new File(filename);
        if (!file.exists()) return false;

        List<Staff> loaded = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Header
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    System.out.println("[WARNING] Skipping malformed CSV row " + lineNum);
                    continue;
                }
                String id = parts[0].trim().toUpperCase();
                String name = parts[1].trim();
                int maxHrs = Integer.parseInt(parts[2].trim());
                String bits = parts[3].trim();

                if (id.isEmpty() || name.isEmpty() || maxHrs < 8 || maxHrs > 84 || bits.length() != 21 || seenIds.contains(id)) {
                    System.out.println("[WARNING] Skipping invalid CSV record at row " + lineNum);
                    continue;
                }

                Staff st = new Staff(id, name, maxHrs, true);
                int idx = 0;
                for (int d = 0; d < 7; d++) {
                    for (int s = 0; s < 3; s++) {
                        char c = bits.charAt(idx++);
                        st.availability[d][s] = (c == '1');
                    }
                }
                seenIds.add(id);
                loaded.add(st);
            }
            if (!loaded.isEmpty()) {
                staffList = loaded;
                return true;
            }
        } catch (Exception e) {
            System.out.println("[WARNING] Error reading " + filename + " (" + e.getMessage() + "). Falling back to defaults.");
        }
        return false;
    }

    static void exportRosterToCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Day,Shift,Required_Staff,Assigned_Count,Status,Assigned_Staff_List");
            for (int d = 0; d < 7; d++) {
                for (int s = 0; s < 3; s++) {
                    Shift sh = weeklyShifts[d][s];
                    List<String> names = new ArrayList<>();
                    for (Staff st : sh.assignedStaff) {
                        names.add(st.name + " (" + st.id + ")");
                    }
                    String status = (sh.assignedStaff.size() < sh.required) ? "UNDERSTAFFED" : "FULL";
                    String staffJoined = names.isEmpty() ? "NONE" : String.join(" | ", names);
                    writer.printf("%s,%s,%d,%d,%s,\"%s\"%n",
                            DAYS[d],
                            SHIFT_NAMES[s].trim(),
                            sh.required,
                            sh.assignedStaff.size(),
                            status,
                            staffJoined);
                }
            }
            System.out.println("[CSV SAVED] " + new File(filename).getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Writing roster CSV failed: " + e.getMessage());
        }
    }

    static void exportStaffSummaryToCSV(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("Staff_ID,Name,Max_Hours_Per_Week,Assigned_Hours,Night_Shifts_Count");
            for (Staff st : staffList) {
                writer.printf("%s,%s,%d,%d,%d%n",
                        st.id, st.name, st.maxHoursPerWeek, st.assignedHours, st.nightShiftsCount);
            }
            System.out.println("[CSV SAVED] " + new File(filename).getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Writing summary CSV failed: " + e.getMessage());
        }
    }

    // ==================== BULLETPROOF INPUT VALIDATION HELPERS ====================

    static int promptDay() {
        return readIntInRange("Enter Day (0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun): ", 0, 6);
    }

    static int promptShift() {
        return readIntInRange("Enter Shift (0=Morning, 1=Evening, 2=Night): ", 0, 2);
    }

    static Staff findStaffById(String id) {
        for (Staff st : staffList) {
            if (st.id.equalsIgnoreCase(id)) return st;
        }
        return null;
    }

    static int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                if (!sc.hasNextLine()) {
                    System.out.println("\nInput stream closed. Exiting safely.");
                    System.exit(0);
                }
                String raw = sc.nextLine().trim();
                if (raw.isEmpty()) {
                    System.out.println("[INVALID] Input cannot be blank. Enter a number between " + min + " and " + max + ".");
                    continue;
                }
                int val = Integer.parseInt(raw);
                if (val < min || val > max) {
                    System.out.println("[INVALID] Number must be between " + min + " and " + max + ".");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("[INVALID] Please enter a valid integer (digits only).");
            }
        }
    }

    static String readValidId(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!sc.hasNextLine()) System.exit(0);
            String id = sc.nextLine().trim().toUpperCase();
            if (id.matches("[A-Z0-9_-]{2,15}")) {
                return id;
            }
            System.out.println("[INVALID] ID must be 2-15 characters using only letters, numbers, '-' or '_'.");
        }
    }

    static String readValidName(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!sc.hasNextLine()) System.exit(0);
            String name = sc.nextLine().trim().replaceAll("\\s+", " ");
            if (name.matches("[A-Za-z .'-]{2,30}")) {
                return name;
            }
            System.out.println("[INVALID] Name must be 2-30 characters using only letters, spaces, dots, or hyphens.");
        }
    }

    static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!sc.hasNextLine()) System.exit(0);
            String ans = sc.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) return true;
            if (ans.equals("n") || ans.equals("no")) return false;
            System.out.println("[INVALID] Please enter 'y' (yes) or 'n' (no).");
        }
    }
}