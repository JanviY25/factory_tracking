package com.example.factory_tracking;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Session storage using SharedPreferences.
 * Persists supervisor/admin session until logout or app data cleared.
 */
public class SessionManager {

    private static final String PREFS_NAME = "factory_tracking_session";
    private static final String KEY_ROLE = "role";           // "supervisor" | "admin"
    private static final String KEY_SUPERVISOR_ID = "supervisor_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_LINE = "line";           // Can be multiple lines comma-separated
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_SHIFT = "shift";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_ADMIN_ID = "admin_id";
    private static final String KEY_SHIFT_SUPERVISOR_ID = "shift_supervisor_id";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSupervisorSession(String supervisorId, String name, String lines) {
        prefs.edit()
                .putString(KEY_ROLE, "supervisor")
                .putString(KEY_SUPERVISOR_ID, supervisorId)
                .putString(KEY_NAME, name)
                .putString(KEY_LINE, lines)
                .remove(KEY_ADMIN_ID)
                .apply();
    }

    public void saveShiftSession(int sessionId, String shift, String endTime, String selectedLines) {
        prefs.edit()
                .putInt(KEY_SESSION_ID, sessionId)
                .putString(KEY_SHIFT, shift)
                .putString(KEY_END_TIME, endTime)
                .putString(KEY_LINE, selectedLines) // Update lines to what was selected for the shift
                .putString(KEY_SHIFT_SUPERVISOR_ID, getSupervisorId())
                .apply();
    }

    public void saveAdminSession(String adminId) {
        prefs.edit()
                .putString(KEY_ROLE, "admin")
                .putString(KEY_ADMIN_ID, adminId)
                .remove(KEY_SUPERVISOR_ID)
                .remove(KEY_NAME)
                .remove(KEY_LINE)
                .remove(KEY_SESSION_ID)
                .remove(KEY_SHIFT)
                .remove(KEY_END_TIME)
                .remove(KEY_SHIFT_SUPERVISOR_ID)
                .apply();
    }

    public boolean isLoggedIn() {
        String role = prefs.getString(KEY_ROLE, null);
        if ("supervisor".equals(role)) {
            return prefs.getString(KEY_SUPERVISOR_ID, null) != null;
        }
        if ("admin".equals(role)) {
            return prefs.getString(KEY_ADMIN_ID, null) != null;
        }
        return false;
    }

    public boolean isSupervisor() {
        return "supervisor".equals(prefs.getString(KEY_ROLE, null));
    }

    public boolean isAdmin() {
        return "admin".equals(prefs.getString(KEY_ROLE, null));
    }

    public String getSupervisorId() { return prefs.getString(KEY_SUPERVISOR_ID, ""); }
    public String getName() { return prefs.getString(KEY_NAME, ""); }
    public String getLine() { return prefs.getString(KEY_LINE, ""); }
    public int getSessionId() { return prefs.getInt(KEY_SESSION_ID, -1); }
    public String getShift() { return prefs.getString(KEY_SHIFT, ""); }
    public String getEndTime() { return prefs.getString(KEY_END_TIME, ""); }
    public String getAdminId() { return prefs.getString(KEY_ADMIN_ID, ""); }
    public String getShiftSupervisorId() { return prefs.getString(KEY_SHIFT_SUPERVISOR_ID, ""); }

    public void setDarkMode(boolean isEnabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isEnabled).apply();
        applyTheme();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void applyTheme() {
        if (isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    /**
     * Clears only shift specific data but keeps the supervisor logged in.
     */
    public void clearShiftSession() {
        prefs.edit()
                .remove(KEY_SESSION_ID)
                .remove(KEY_SHIFT)
                .remove(KEY_END_TIME)
                .remove(KEY_SHIFT_SUPERVISOR_ID)
                .apply();
    }

    /**
     * Clears login credentials but preserves shift data if active.
     */
    public void logout() {
        prefs.edit()
                .remove(KEY_ROLE)
                .remove(KEY_SUPERVISOR_ID)
                .remove(KEY_NAME)
                .remove(KEY_ADMIN_ID)
                .apply();
    }

    /**
     * Full clear everything.
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
