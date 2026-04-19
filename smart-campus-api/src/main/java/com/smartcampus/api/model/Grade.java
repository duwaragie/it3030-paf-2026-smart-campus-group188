package com.smartcampus.api.model;

// 4.0 scale; I (Incomplete) and W (Withdrawn) don't count toward GPA.
public enum Grade {
    A_PLUS("A+", 4.0, true),
    A("A", 4.0, true),
    A_MINUS("A-", 3.7, true),
    B_PLUS("B+", 3.3, true),
    B("B", 3.0, true),
    B_MINUS("B-", 2.7, true),
    C_PLUS("C+", 2.3, true),
    C("C", 2.0, true),
    C_MINUS("C-", 1.7, true),
    D_PLUS("D+", 1.3, true),
    D("D", 1.0, true),
    F("F", 0.0, true),
    I("I", null, false),   // Incomplete
    W("W", null, false);   // Withdrawn

    private final String label;
    private final Double gpaPoints;
    private final boolean countsForGpa;

    Grade(String label, Double gpaPoints, boolean countsForGpa) {
        this.label = label;
        this.gpaPoints = gpaPoints;
        this.countsForGpa = countsForGpa;
    }

    public String getLabel() { return label; }
    public Double getGpaPoints() { return gpaPoints; }
    public boolean isCountsForGpa() { return countsForGpa; }

    // Tolerates case + any whitespace; "b -" and " A + " both resolve.
    public static Grade fromLabel(String label) {
        if (label == null) return null;
        String normalized = label.replaceAll("\\s+", "").toUpperCase();
        if (normalized.isEmpty()) return null;
        for (Grade g : values()) {
            if (g.label.equalsIgnoreCase(normalized)) return g;
        }
        return null;
    }
}
