package com.ecommerce.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;

/** Small helpers for reading input and printing tables on the terminal. */
public class Console {
    private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    public String line(String prompt) {
        System.out.print(prompt + ": ");
        System.out.flush();
        try {
            String s = in.readLine();
            if (s == null) throw new IllegalStateException("End of input");
            return s.trim();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Required text: asks again until non-empty. */
    public String required(String prompt) {
        while (true) {
            String s = line(prompt);
            if (!s.isEmpty()) return s;
            System.out.println("  This field is required.");
        }
    }

    /** Optional text: Enter keeps the current value. */
    public String withDefault(String prompt, String current) {
        String s = line(prompt + " [" + current + "]");
        return s.isEmpty() ? current : s;
    }

    public int integer(String prompt, int min, int max) {
        while (true) {
            String s = line(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) {
                // fall through
            }
            System.out.println("  Enter a whole number between " + min + " and " + max + ".");
        }
    }

    public int integerWithDefault(String prompt, int current, int min, int max) {
        while (true) {
            String s = line(prompt + " [" + current + "]");
            if (s.isEmpty()) return current;
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) {
                // fall through
            }
            System.out.println("  Enter a whole number between " + min + " and " + max + ".");
        }
    }

    public long id(String prompt) {
        while (true) {
            try {
                return Long.parseLong(line(prompt));
            } catch (NumberFormatException e) {
                System.out.println("  Enter a numeric id.");
            }
        }
    }

    public BigDecimal money(String prompt, BigDecimal current) {
        while (true) {
            String s = line(current == null ? prompt : prompt + " [" + current + "]");
            if (s.isEmpty() && current != null) return current;
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                System.out.println("  Enter an amount like 19.99.");
            }
        }
    }

    /** Blank input returns null. */
    public BigDecimal optionalMoney(String prompt) {
        while (true) {
            String s = line(prompt + " (Enter to skip)");
            if (s.isEmpty()) return null;
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException e) {
                System.out.println("  Enter an amount like 19.99.");
            }
        }
    }

    public boolean confirm(String prompt) {
        return line(prompt + " (y/n)").equalsIgnoreCase("y");
    }

    public void pause() {
        line("Press Enter to continue");
    }

    /** True when the user has typed something (used to leave live views). */
    public boolean hasPendingInput() {
        try {
            return in.ready();
        } catch (IOException e) {
            return false;
        }
    }

    public void drainInput() {
        try {
            while (in.ready()) in.readLine();
        } catch (IOException ignored) {
            // nothing to drain
        }
    }

    // ---------------------------------------------------------------- output

    public static void title(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }

    public static void table(List<String> headers, List<List<String>> rows) {
        if (rows.isEmpty()) {
            System.out.println("  (nothing to show)");
            return;
        }
        int[] w = new int[headers.size()];
        for (int i = 0; i < w.length; i++) w[i] = headers.get(i).length();
        for (List<String> r : rows) for (int i = 0; i < w.length; i++) w[i] = Math.max(w[i], r.get(i).length());
        printRow(headers, w);
        StringBuilder sep = new StringBuilder();
        for (int x : w) sep.append("-".repeat(x)).append("-+-");
        System.out.println(sep.substring(0, sep.length() - 3));
        rows.forEach(r -> printRow(r, w));
    }

    private static void printRow(List<String> cells, int[] w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w.length; i++) {
            sb.append(String.format("%-" + w[i] + "s", cells.get(i)));
            if (i < w.length - 1) sb.append(" | ");
        }
        System.out.println(sb);
    }

    public static String bar(double value, double max, int width) {
        int n = max <= 0 ? 0 : (int) Math.round(value / max * width);
        return "#".repeat(Math.max(0, n));
    }

    public static String clip(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
