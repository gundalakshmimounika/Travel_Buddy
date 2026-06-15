package com.simats.travelbuddy

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @FormUrlEncoded
    @POST("login.php")
    fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("register.php")
    fun register(
        @Field("full_name") fullName: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("reset_password.php")
    fun resetPassword(
        @Field("email") email: String,
        @Field("password") password: String
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("add_trip.php")
    fun addTrip(
        @Field("email") email: String,
        @Field("destination") destination: String,
        @Field("image_res") imageRes: Int
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("delete_trip.php")
    fun deleteTrip(
        @Field("email") email: String,
        @Field("destination") destination: String
    ): Call<ApiResponse>

    @GET("get_trips.php")
    fun getTrips(
        @Query("email") email: String
    ): Call<TripListResponse>

    @GET("place_details.php")
    fun getPlaceDetails(
        @retrofit2.http.Query("place") place: String
    ): Call<PlaceDetailsResponse>

    @POST("ask_buddy.php")
    fun askBuddy(
        @retrofit2.http.Body request: ChatRequest
    ): Call<ChatResponse>

    @GET("packing.php?action=get")
    fun getPackingItems(
        @retrofit2.http.Query("trip") trip: String
    ): Call<PackingListResponse>

    @GET("packing.php?action=add")
    fun addPackingItem(
        @retrofit2.http.Query("trip") trip: String,
        @retrofit2.http.Query("item") item: String,
        @retrofit2.http.Query("category") category: String
    ): Call<ApiResponse>

    @GET("packing.php?action=toggle")
    fun togglePackingItem(
        @retrofit2.http.Query("trip") trip: String,
        @retrofit2.http.Query("id") id: Int,
        @retrofit2.http.Query("status") status: Int
    ): Call<ApiResponse>

    @GET("packing.php?action=delete")
    fun deletePackingItem(
        @retrofit2.http.Query("trip") trip: String,
        @retrofit2.http.Query("id") id: Int
    ): Call<ApiResponse>

    @GET("budget.php?action=get")
    fun getBudgetInfo(
        @retrofit2.http.Query("trip") trip: String
    ): Call<BudgetResponse>

    @GET("budget.php?action=set_limit")
    fun setBudgetLimit(
        @retrofit2.http.Query("trip") trip: String,
        @retrofit2.http.Query("limit") limit: Double
    ): Call<ApiResponse>

    @GET("budget.php?action=add_expense")
    fun addExpense(
        @retrofit2.http.Query("trip") trip: String,
        @retrofit2.http.Query("amount") amount: Double,
        @retrofit2.http.Query("note") note: String
    ): Call<ApiResponse>

    @GET("budget.php?action=delete_expense")
    fun deleteExpense(
        @retrofit2.http.Query("trip") trip: String,
        @retrofit2.http.Query("id") id: Int
    ): Call<ApiResponse>

    @GET("weather.php")
    fun getWeather(
        @retrofit2.http.Query("place") place: String
    ): Call<WeatherResponse>

    @GET("hotels.php")
    fun getHotels(
        @retrofit2.http.Query("place") place: String
    ): Call<List<HotelItem>>

    @GET("tickets.php")
    fun getTickets(
        @retrofit2.http.Query("from") fromLocation: String,
        @retrofit2.http.Query("to") toLocation: String
    ): Call<TicketResponse>

    @GET("community.php?action=get")
    fun getCommunityPosts(): Call<CommunityResponse>

    @FormUrlEncoded
    @POST("community.php?action=add")
    fun addCommunityPost(
        @Field("user_name") userName: String,
        @Field("destination") destination: String,
        @Field("dates") dates: String,
        @Field("description") description: String
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("community.php?action=join")
    fun joinCommunityPost(
        @Field("id") id: Int,
        @Field("requester_name") requesterName: String,
        @Field("requester_email") requesterEmail: String
    ): Call<ApiResponse>

    @GET("community.php?action=get_requests")
    fun getJoinRequests(
        @Query("owner_name") ownerName: String
    ): Call<JoinRequestsResponse>

    @FormUrlEncoded
    @POST("community.php?action=respond_request")
    fun respondJoinRequest(
        @Field("request_id") requestId: Int,
        @Field("status") status: String
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("community.php?action=edit")
    fun editCommunityPost(
        @Field("id") id: Int,
        @Field("destination") destination: String,
        @Field("dates") dates: String,
        @Field("description") description: String
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("community.php?action=delete")
    fun deleteCommunityPost(
        @Field("id") id: Int
    ): Call<ApiResponse>
}

data class TicketOption(
    val type: String,
    val price: String,
    val duration: String
)

data class TicketResponse(
    val status: String,
    val data: List<TicketOption>?
)

data class HotelItem(
    val name: String,
    val rating: String,
    val price: String,
    val description: String,
    val contact: String
)

data class WeatherResponse(
    val current_temp: String,
    val condition: String,
    val hourly: List<HourlyWeather>,
    val daily: List<DailyWeather>
)

data class HourlyWeather(
    val time: String,
    val temp: String,
    val condition: String
)

data class DailyWeather(
    val day: String,
    val low: String,
    val high: String,
    val condition: String
)

data class ExpenseItem(
    val id: Int,
    val amount: Double,
    val note: String,
    val created_at: String
)

data class BudgetResponse(
    val status: String,
    val total_budget: Double,
    val expenses: List<ExpenseItem>
)

data class PackingItem(
    val id: Int,
    val item_name: String,
    val category: String,
    var is_packed: Boolean
)

data class PackingListResponse(
    val status: String,
    val data: List<PackingItem>
)

data class ChatRequest(val question: String)
data class ChatResponse(val status: String, val answer: String)

data class ApiResponse(
    val status: String,
    val message: String
)

data class TripListResponse(
    val status: String,
    val data: List<TripData>
)

data class PlaceDetailsResponse(
    val status: String,
    val data: PlaceFullInfo
)

data class PlaceFullInfo(
    val title: String,
    val description: String,
    val hotels: List<String>,
    val travel_methods: String,
    val budget_min: String,
    val budget_max: String,
    val latitude: Double,
    val longitude: Double,
    val best_time: String,
    val rating: String,
    val recommended_places: List<String>?
)

data class TripData(
    val destination: String,
    val image_res: Int
)

data class CommunityPost(
    val id: Int,
    val user_name: String,
    val user_rating: String,
    val avatar_id: Int,
    val destination: String,
    val dates: String,
    val description: String,
    val interested_count: Int
)

data class CommunityResponse(
    val status: String,
    val data: List<CommunityPost>
)

data class JoinRequestData(
    val id: Int,
    val post_id: Int,
    val requester_name: String,
    val requester_email: String,
    val destination: String,
    val status: String
)

data class JoinRequestsResponse(
    val status: String,
    val data: List<JoinRequestData>?
)
