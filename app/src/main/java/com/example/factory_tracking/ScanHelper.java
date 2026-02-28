package com.example.factory_tracking;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust extractor for QR data.
 * Handles Operator (ID, Name, Contractor) and Station (ID, Line).
 */
public final class ScanHelper {

    // Matches "STATION ID: :LINE1-ST05" -> Extracts "LINE1-ST05"
    private static final Pattern STATION_PATTERN = Pattern.compile(
            "STATION\\s*ID\\s*:\\s*:?([A-Za-z0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Matches "ASSEMBLY LINE: :LINE 1" -> Extracts "LINE 1"
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "ASSEMBLY\\s*LINE\\s*:\\s*:?([A-Za-z0-9\\s\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Matches "OPEATOR ID: :OP-001" -> Extracts "OP-001"
    private static final Pattern OPERATOR_PATTERN = Pattern.compile(
            "OPE?RATOR\\s*ID\\s*:\\s*:?([A-Za-z0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Matches "NAME: DUMMY WORKER" -> Extracts "DUMMY WORKER"
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "NAME\\s*:\\s*([A-Za-z0-9\\s\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Matches "CONTRACTOR: ABC CONTRACTS" -> Extracts "ABC CONTRACTS"
    private static final Pattern CONTRACTOR_PATTERN = Pattern.compile(
            "CONTRACTOR\\s*:\\s*([A-Za-z0-9\\s\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    public static String extractStationId(String rawText) {
        return findMatch(STATION_PATTERN, rawText);
    }

    public static String extractLine(String rawText) {
        return findMatch(LINE_PATTERN, rawText);
    }

    public static String extractOperatorId(String rawText) {
        return findMatch(OPERATOR_PATTERN, rawText);
    }

    public static String extractOperatorName(String rawText) {
        return findMatch(NAME_PATTERN, rawText);
    }

    public static String extractContractor(String rawText) {
        return findMatch(CONTRACTOR_PATTERN, rawText);
    }

    private static String findMatch(Pattern pattern, String text) {
        if (text == null) return null;
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }
}
