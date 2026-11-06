package org.sclerck.ktfm;

import java.util.Scanner;

public class Utils {

    // ─── Scanner ─────────────────────────────────────────────────────────────
    private static final Scanner SC = new Scanner(System.in);

    // =========================================================================
    // UTILITY
    // =========================================================================
    public static void cls() {
        // Clear screen best-effort for terminals
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    public static void waitEnter() {
        System.out.print("  [Press ENTER to continue]");
        SC.nextLine();
    }

    public static String inputLine(String prompt, int maxLen) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim();
            if (maxLen > 0 && s.length() > maxLen) {
                System.out.println("Too long (max " + maxLen + " chars).");
                continue;
            }
            return s;
        }
    }

    public static int inputInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = SC.nextLine().trim();
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { System.out.println("Please enter a number."); }
        }
    }
}
