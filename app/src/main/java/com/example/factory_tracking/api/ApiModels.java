package com.example.factory_tracking.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiModels {

    public static class LoginRequest {
        public String userId;
        public String password;

        public LoginRequest(String userId, String password) {
            this.userId = userId;
            this.password = password;
        }
    }

    public static class LoginResponse {
        public String status;
        @SerializedName("supervisor_id")
        public String supervisorId;
        public String name;
        public String line;
        @SerializedName("active_session")
        public ActiveSession activeSession;
    }

    public static class ActiveSession {
        @SerializedName("session_id")
        public int sessionId;
        @SerializedName("line_id")
        public String lineId;
        public String shift;
        @SerializedName("end_time")
        public String endTime;
    }

    public static class AdminLoginRequest {
        public String userId;
        public String password;
    }

    public static class AdminLoginResponse {
        public String status;
        @SerializedName("admin_id")
        public String adminId;
    }

    public static class StartShiftRequest {
        @SerializedName("supervisor_id")
        public String supervisorId;
        @SerializedName("line_id")
        public String lineId;
        public String shift;
        @SerializedName("end_time")
        public String endTime;
    }

    public static class StartShiftResponse {
        public String status;
        public String message;
        @SerializedName("session_id")
        public int sessionId;
    }

    public static class GetStationsRequest {
        public String line;

        public GetStationsRequest(String line) {
            this.line = line;
        }
    }

    public static class StationItem {
        @SerializedName("station_id")
        public String stationId;
        public String line;
        @SerializedName("operator_id")
        public String operatorId;
        @SerializedName("operator_name")
        public String operatorName;
        public String status;
        @SerializedName("in_charge")
        public String inCharge;
    }

    public static class GetStationsResponse {
        public String status;
        public List<StationItem> stations;
    }

    public static class AssignRequest {
        @SerializedName("station_id")
        public String stationId;
        @SerializedName("operator_id")
        public String operatorId;
        @SerializedName("operator_name")
        public String operatorName;
        @SerializedName("supervisor_id")
        public String supervisorId;
        public String shift;
        @SerializedName("session_id")
        public Integer sessionId;
    }

    public static class AssignResponse {
        public String status;
        public String message;
    }

    public static class EndShiftRequest {
        @SerializedName("session_id")
        public int sessionId;
        @SerializedName("line_id")
        public String lineId;
    }

    public static class EndShiftResponse {
        public String status;
    }

    public static class SupervisorItem {
        @SerializedName("supervisor_id")
        public String supervisorId;
        public String name;
        public String line;
    }

    public static class SupervisorsListResponse {
        public String status;
        public List<SupervisorItem> list;
    }

    public static class AddSupervisorRequest {
        @SerializedName("supervisor_id")
        public String supervisorId;
        public String name;
        public String password;
        public String line;
    }

    public static class TransactionItem {
        @SerializedName("txn_id")
        public int txnId;
        @SerializedName("operator_id")
        public String operatorId;
        @SerializedName("station_id")
        public String stationId;
        @SerializedName("line_id")
        public String lineId;
        @SerializedName("supervisor_id")
        public String supervisorId;
        public String shift;
        @SerializedName("start_time")
        public String startTime;
        @SerializedName("end_time")
        public String endTime;
    }

    public static class TransactionsResponse {
        public String status;
        public List<TransactionItem> data;
    }

    public static class OperatorLoginRequest {
        public String name;
        public String password;
    }

    public static class OperatorLoginResponse {
        public String status;
        @SerializedName("operator_id") public String operatorId;
        public String name;
        public String message;
    }

    public static class OperatorHistoryRequest {
        @SerializedName("operator_id") public String operatorId;
    }

    public static class OperatorHistoryRecord {
        @SerializedName("station_id") public String stationId;
        public String date;
        public String shift;
        public String status;
    }

    public static class OperatorHistoryResponse {
        public String status;
        public String message;
        public List<OperatorHistoryRecord> history;
    }

    public static class LinesResponse {
        public String status;
        public List<String> lines;
    }

    public static class ProcessCompletionRequest {
        @SerializedName("operator_id") public String operatorId;
        @SerializedName("station_id") public String stationId;
        @SerializedName("step_name") public String stepName;
    }

    public static class SimpleResponse {
        public String status;
        public String message;
    }

    public static class ValidationFailureRequest {
        @SerializedName("supervisor_id") public String supervisorId;
        @SerializedName("station_id") public String stationId;
        @SerializedName("operator_id") public String operatorId;
        public String reason;
    }

    public static class IdleTimeItem {
        @SerializedName("session_id") public int sessionId;
        @SerializedName("line_id") public String lineId;
        @SerializedName("supervisor_id") public String supervisorId;
        @SerializedName("session_start") public String sessionStart;
        @SerializedName("session_end") public String sessionEnd;
        @SerializedName("total_shift_minutes") public int totalShiftMinutes;
        @SerializedName("total_working_minutes") public int totalWorkingMinutes;
    }

    public static class IdleTimeResponse {
        public String status;
        public List<IdleTimeItem> data;
    }
}
