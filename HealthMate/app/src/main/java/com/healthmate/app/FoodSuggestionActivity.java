package com.healthmate.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.healthmate.app.adapter.FoodAdapter;
import com.healthmate.app.api.ApiClient;
import com.healthmate.app.api.OpenFoodFactsService;
import com.healthmate.app.database.DatabaseHelper;
import com.healthmate.app.model.Food;
import com.healthmate.app.model.FoodSearchResponse;
import com.healthmate.app.util.Constants;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodSuggestionActivity extends AppCompatActivity implements FoodAdapter.OnFoodItemClickListener {

    // UI elements
    private TextView welcomeTextView, bmiValueTextView;
    private EditText foodSearchEditText;
    private Button searchButton;
    private ImageButton exerciseNavigationButton;
    private ProgressBar searchProgressBar;
    private CardView resultCardView;
    private LinearLayout resultContainer;
    private RecyclerView foodRecyclerView;
    
    // Food details UI elements
    private TextView foodNameTextView;
    private TextView recommendedAmountTextView;
    private TextView caloriesTextView;
    private TextView proteinsTextView;
    private TextView carbsTextView;
    private TextView fatsTextView;
    private TextView fibersTextView;
    
    // Database helper for local food data
    private DatabaseHelper databaseHelper;
    
    // User's BMI value
    private float userBmi;
    
    // API service
    private OpenFoodFactsService openFoodFactsService;
    
    // Food adapter for the RecyclerView
    private FoodAdapter foodAdapter;
    private List<Food> foodList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_suggestion);
        
        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);
        
        // Initialize API service
        openFoodFactsService = ApiClient.getOpenFoodFactsService();
        
        // Initialize UI elements
        initializeViews();
        
        // Load user information
        loadUserInfo();
        
        // Set click listeners
        setClickListeners();
        
        // Setup RecyclerView
        setupRecyclerView();
    }
    
    /**
     * Initialize all UI elements
     */
    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        bmiValueTextView = findViewById(R.id.bmiValueTextView);
        foodSearchEditText = findViewById(R.id.foodSearchEditText);
        searchButton = findViewById(R.id.searchButton);
        exerciseNavigationButton = findViewById(R.id.exerciseNavigationButton);
        searchProgressBar = findViewById(R.id.searchProgressBar);
        resultCardView = findViewById(R.id.resultCardView);
        resultContainer = findViewById(R.id.resultContainer);
        foodRecyclerView = findViewById(R.id.foodRecyclerView);
        
        foodNameTextView = findViewById(R.id.foodNameTextView);
        recommendedAmountTextView = findViewById(R.id.recommendedAmountTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);
        proteinsTextView = findViewById(R.id.proteinsTextView);
        carbsTextView = findViewById(R.id.carbsTextView);
        fatsTextView = findViewById(R.id.fatsTextView);
        fibersTextView = findViewById(R.id.fibersTextView);
        
        // Initially hide result card and progress bar
        resultCardView.setVisibility(View.GONE);
        searchProgressBar.setVisibility(View.GONE);
    }
    
    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        foodAdapter = new FoodAdapter(this, foodList, this);
        foodRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        foodRecyclerView.setAdapter(foodAdapter);
    }
    
    /**
     * Load user information from shared preferences
     */
    private void loadUserInfo() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String userName = preferences.getString(Constants.KEY_NAME, "User");
        userBmi = preferences.getFloat(Constants.KEY_BMI, 0);
        
        welcomeTextView.setText("Hello, " + userName);
        bmiValueTextView.setText(String.format("Your BMI: %.1f", userBmi));
    }
    
    /**
     * Set click listeners for buttons
     */
    private void setClickListeners() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFood();
            }
        });
        
        exerciseNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToExerciseSuggestion();
            }
        });
    }
    
    /**
     * Search for food in the API and local database
     */
    private void searchFood() {
        String foodName = foodSearchEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(foodName)) {
            foodSearchEditText.setError("Please enter a food item");
            return;
        }
        
        // Show progress bar and hide result card
        searchProgressBar.setVisibility(View.VISIBLE);
        resultCardView.setVisibility(View.GONE);
        
        // First try to find food in the local database
        Food localFood = databaseHelper.getFoodByName(foodName);
        
        if (localFood != null) {
            // If found locally, display it immediately
            searchProgressBar.setVisibility(View.GONE);
            displayFoodDetails(localFood);
            
            // Clear the search results list
            foodList.clear();
            foodAdapter.notifyDataSetChanged();
        } else {
            // If not found locally, search in the API
            searchFoodInAPI(foodName);
        }
    }
    
    /**
     * Search food in the Open Food Facts API
     * @param query Food name to search
     */
    private void searchFoodInAPI(String query) {
        Call<FoodSearchResponse> call = openFoodFactsService.searchFood(
                query, 1, Constants.DEFAULT_PAGE_SIZE);
        
        call.enqueue(new Callback<FoodSearchResponse>() {
            @Override
            public void onResponse(Call<FoodSearchResponse> call, Response<FoodSearchResponse> response) {
                searchProgressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    FoodSearchResponse searchResponse = response.body();
                    List<Food> apiResults = searchResponse.toFoodList();
                    
                    if (!apiResults.isEmpty()) {
                        // Update the RecyclerView with the search results
                        foodList.clear();
                        foodList.addAll(apiResults);
                        foodAdapter.notifyDataSetChanged();
                        foodRecyclerView.setVisibility(View.VISIBLE);
                        
                        // Hide the result card as we're showing a list now
                        resultCardView.setVisibility(View.GONE);
                        
                        Toast.makeText(FoodSuggestionActivity.this, 
                                "Found " + apiResults.size() + " results", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // No results found
                        Toast.makeText(FoodSuggestionActivity.this, 
                                "No foods found. Please try another search term.", 
                                Toast.LENGTH_LONG).show();
                        foodRecyclerView.setVisibility(View.GONE);
                    }
                } else {
                    // API error
                    Toast.makeText(FoodSuggestionActivity.this, 
                            "Error searching for food. Please try again.", 
                            Toast.LENGTH_LONG).show();
                    foodRecyclerView.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onFailure(Call<FoodSearchResponse> call, Throwable t) {
                searchProgressBar.setVisibility(View.GONE);
                
                // Network error
                Toast.makeText(FoodSuggestionActivity.this, 
                        "Network error: " + t.getMessage(), 
                        Toast.LENGTH_LONG).show();
                foodRecyclerView.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Handle click on food item in the RecyclerView
     * @param food The selected food item
     */
    @Override
    public void onFoodItemClick(Food food) {
        displayFoodDetails(food);
        foodRecyclerView.setVisibility(View.GONE);
    }
    
    /**
     * Display food details including recommended amount based on BMI
     * @param food Food object containing food details
     */
    private void displayFoodDetails(Food food) {
        resultCardView.setVisibility(View.VISIBLE);
        
        foodNameTextView.setText(food.getName());
        
        // Calculate recommended amount based on BMI
        String recommendedAmount = calculateRecommendedAmount(food, userBmi);
        recommendedAmountTextView.setText("Recommended: " + recommendedAmount);
        
        // Display nutritional information
        caloriesTextView.setText("Calories: " + food.getCalories() + " kcal");
        proteinsTextView.setText("Proteins: " + food.getProteins() + "g");
        carbsTextView.setText("Carbs: " + food.getCarbs() + "g");
        fatsTextView.setText("Fats: " + food.getFats() + "g");
        fibersTextView.setText("Fibers: " + food.getFiber() + "g");
    }
    
    /**
     * Calculate recommended amount of food based on BMI
     * @param food Food object
     * @param bmi User's BMI
     * @return Recommended amount as a String
     */
    private String calculateRecommendedAmount(Food food, float bmi) {
        String unit = food.getUnit();
        float baseAmount = food.getBaseAmount();
        float adjustedAmount;
        
        // Adjust serving size based on BMI category
        if (bmi < 18.5) {  // Underweight
            adjustedAmount = baseAmount * 1.2f;  // Increase by 20%
        } else if (bmi >= 18.5 && bmi < 25) {  // Normal weight
            adjustedAmount = baseAmount;  // Standard serving
        } else if (bmi >= 25 && bmi < 30) {  // Overweight
            adjustedAmount = baseAmount * 0.8f;  // Reduce by 20%
        } else {  // Obese
            adjustedAmount = baseAmount * 0.7f;  // Reduce by 30%
        }
        
        // Round to one decimal place
        adjustedAmount = Math.round(adjustedAmount * 10) / 10f;
        
        return adjustedAmount + " " + unit;
    }
    
    /**
     * Navigate to the Exercise Suggestion screen
     */
    private void navigateToExerciseSuggestion() {
        Intent intent = new Intent(this, ExerciseSuggestionActivity.class);
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload user info in case it has changed
        loadUserInfo();
    }
}
