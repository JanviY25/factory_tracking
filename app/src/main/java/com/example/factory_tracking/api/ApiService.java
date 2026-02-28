package com.example.factory_tracking.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/login")
    Call<ApiModels.LoginResponse> login(@Body ApiModels.LoginRequest body);

    @POST("api/admin/login")
    Call<ApiModels.AdminLoginResponse> adminLogin(@Body ApiModels.AdminLoginRequest body);

    @POST("api/admin/addLine")
    Call<ApiModels.AssignResponse> addLine(@Body Map<String, Object> body);

    @POST("api/admin/removeLine")
    Call<ApiModels.AssignResponse> removeLine(@Body Map<String, Object> body);

    @POST("api/startShift")
    Call<ApiModels.StartShiftResponse> startShift(@Body ApiModels.StartShiftRequest body);

    @POST("api/getStations")
    Call<ApiModels.GetStationsResponse> getStations(@Body ApiModels.GetStationsRequest body);

    @GET("api/lines")
    Call<ApiModels.LinesResponse> getLines();

    @GET("api/admin/lines")
    Call<ApiModels.GetStationsResponse> getAdminLines();

    @POST("api/assign")
    Call<ApiModels.AssignResponse> assign(@Body ApiModels.AssignRequest body);

    @POST("api/endShift")
    Call<ApiModels.EndShiftResponse> endShift(@Body ApiModels.EndShiftRequest body);

    @GET("api/transactions")
    Call<ApiModels.TransactionsResponse> getTransactions();

    @GET("api/admin/supervisors")
    Call<ApiModels.SupervisorsListResponse> getSupervisors();

    @POST("api/admin/supervisors")
    Call<ApiModels.AssignResponse> addSupervisor(@Body ApiModels.AddSupervisorRequest body);

    @DELETE("api/admin/supervisors/{id}")
    Call<ApiModels.AssignResponse> deleteSupervisor(@Path("id") String id);

    @GET("api/export/csv")
    Call<okhttp3.ResponseBody> exportCsvRange(@Query("startDate") String start, @Query("endDate") String end);

    @POST("api/operator/login")
    Call<ApiModels.OperatorLoginResponse> operatorLogin(@Body ApiModels.OperatorLoginRequest body);

    @POST("api/operator/history")
    Call<ApiModels.OperatorHistoryResponse> operatorHistory(@Body ApiModels.OperatorHistoryRequest body);

    @POST("api/process/complete")
    Call<ApiModels.SimpleResponse> logProcessCompletion(@Body ApiModels.ProcessCompletionRequest body);

    @POST("api/validation/fail")
    Call<ApiModels.SimpleResponse> logValidationFailure(@Body ApiModels.ValidationFailureRequest body);

    @GET("api/admin/idleTime")
    Call<ApiModels.IdleTimeResponse> getIdleTimeReport();
}
