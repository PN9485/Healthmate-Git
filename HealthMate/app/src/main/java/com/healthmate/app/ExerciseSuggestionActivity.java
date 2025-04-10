package com.healthmate.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.healthmate.app.adapter.ExerciseAdapter;
import com.healthmate.app.api.ApiClient;
import com.healthmate.app.api.ExerciseDbService;
import com.healthmate.app.database.DatabaseHelper;
import com.healthmate.app.model.Exercise;
import com.healthmate.app.model.api.ApiExerciseList;
import com.healthmate.app.util.ApiConfig;
import com.healthmate.app.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExerciseSuggestionActivity extends AppCompatActivity implements ExerciseAdapter.OnExerciseItemClickListener {

    private static final String TAG = "ExerciseSuggestionActivity";

    // UI elements
    private TextView welcomeTextView, bmiValueTextView;
    private Spinner bodyPartSpinner;
    private RecyclerView exerciseRecyclerView;
    private ProgressBar loadingProgressBar;
    private ImageButton foodNavigationButton;
    
    // Database helper for local exercise data
    private DatabaseHelper databaseHelper;
    
    // API service
    private ExerciseDbService exerciseDbService;
    
    // Adapter for exercise list
    private ExerciseAdapter exerciseAdapter;
    
    // List of exercises
    private List<Exercise> exerciseList = new ArrayList<>();
    
    // Available body parts
    private String[] bodyParts = {
            "Select body part", "Arms", "Chest", "Back", "Abs", "Legs", "Full Body"
    };
    
    // Mapping of UI body parts to API body parts
    private Map<String, String> bodyPartMapping = new HashMap<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_suggestion);
        
        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);
        
        // Initialize API service
        exerciseDbService = ApiClient.getExerciseDbService();
        
        // Initialize body part mapping
        initializeBodyPartMapping();
        
        // Initialize UI elements
        initializeViews();
        
        // Load user information
        loadUserInfo();
        
        // Set up spinner
        setupBodyPartSpinner();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set click listeners
        setClickListeners();
    }
    
    /**
     * Initialize mapping between UI body parts and API body parts
     */
    private void initializeBodyPartMapping() {
        bodyPartMapping.put("Arms", "upper arms,lower arms");
        bodyPartMapping.put("Chest", "chest");
        bodyPartMapping.put("Back", "back");
        bodyPartMapping.put("Abs", "waist");
        bodyPartMapping.put("Legs", "upper legs,lower legs");
        bodyPartMapping.put("Full Body", "cardio");
    }
    
    /**
     * Initialize all UI elements
     */
    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        bmiValueTextView = findViewById(R.id.bmiValueTextView);
        bodyPartSpinner = findViewById(R.id.bodyPartSpinner);
        exerciseRecyclerView = findViewById(R.id.exerciseRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        foodNavigationButton = findViewById(R.id.foodNavigationButton);
        
        // Initially hide loading progress
        loadingProgressBar.setVisibility(View.GONE);
    }
    
    /**
     * Load user information from shared preferences
     */
    private void loadUserInfo() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String userName = preferences.getString(Constants.KEY_NAME, "User");
        float userBmi = preferences.getFloat(Constants.KEY_BMI, 0);
        
        welcomeTextView.setText("Hello, " + userName);
        bmiValueTextView.setText(String.format("Your BMI: %.1f", userBmi));
    }
    
    /**
     * Set up the body part spinner with available options
     */
    private void setupBodyPartSpinner() {
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, bodyParts);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bodyPartSpinner.setAdapter(spinnerAdapter);
        
        bodyPartSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String selectedBodyPart = bodyParts[position];
                    loadExercises(selectedBodyPart);
                } else {
                    exerciseList.clear();
                    exerciseAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    /**
     * Set up RecyclerView for exercise list
     */
    private void setupRecyclerView() {
        exerciseAdapter = new ExerciseAdapter(this, exerciseList, this);
        exerciseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        exerciseRecyclerView.setAdapter(exerciseAdapter);
    }
    
    /**
     * Set click listeners for buttons
     */
    private void setClickListeners() {
        foodNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToFoodSuggestion();
            }
        });
    }
    
    /**
     * Load exercises for the selected body part
     * @param bodyPart Selected body part
     */
    private void loadExercises(String bodyPart) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        exerciseList.clear();
        exerciseAdapter.notifyDataSetChanged();
        
        // First try to load from local database
        List<Exercise> localExercises = databaseHelper.getExercisesByBodyPart(bodyPart);
        
        if (!localExercises.isEmpty()) {
            // If found locally, display immediately
            exerciseList.addAll(localExercises);
            exerciseAdapter.notifyDataSetChanged();
            loadingProgressBar.setVisibility(View.GONE);
        } else {
            // If not found locally or if we need more exercises, fetch from API
            fetchExercisesFromApi(bodyPart);
        }
    }
    
    /**
     * Fetch exercises from the API
     * @param bodyPart Selected body part
     */
    private void fetchExercisesFromApi(String bodyPart) {
        // Get the API mapping for this body part
        String apiBodyPart = bodyPartMapping.get(bodyPart);
        
        if (apiBodyPart == null) {
            loadingProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Could not find mapping for body part", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // If body part contains multiple options (e.g., "upper arms,lower arms"), use the first one
        String firstBodyPart = apiBodyPart.split(",")[0];
        
        Call<ApiExerciseList> call = exerciseDbService.getExercisesByBodyPart(
                firstBodyPart, 
                ApiConfig.EXERCISE_DB_API_KEY, 
                Constants.EXERCISE_DB_HOST_VALUE);
        
        call.enqueue(new Callback<ApiExerciseList>() {
            @Override
            public void onResponse(Call<ApiExerciseList> call, Response<ApiExerciseList> response) {
                loadingProgressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiExerciseList apiResponse = response.body();
                    List<Exercise> apiExercises = apiResponse.toExerciseList();
                    
                    if (!apiExercises.isEmpty()) {
                        exerciseList.clear();
                        exerciseList.addAll(apiExercises);
                        exerciseAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(ExerciseSuggestionActivity.this, 
                                "No exercises found for this body part", 
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Check if we need to use a fallback (e.g., API key issue)
                    handleApiError(bodyPart, response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ApiExerciseList> call, Throwable t) {
                loadingProgressBar.setVisibility(View.GONE);
                Log.e(TAG, "API call failed: " + t.getMessage());
                
                // Use fallback when API call fails
                useFallbackExercises(bodyPart);
            }
        });
    }
    
    /**
     * Handle API errors with appropriate messaging and fallbacks
     * @param bodyPart The selected body part
     * @param errorCode The HTTP error code
     */
    private void handleApiError(String bodyPart, int errorCode) {
        if (errorCode == 401 || errorCode == 403) {
            // Authentication issue - likely invalid API key
            Toast.makeText(this,
                    "API key error. Please update your API key in ApiConfig class.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this,
                    "Error loading exercises from API. Error code: " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }
        
        // Use fallback data
        useFallbackExercises(bodyPart);
    }
    
    /**
     * Use fallback data from local database when API fails
     * @param bodyPart The selected body part
     */
    private void useFallbackExercises(String bodyPart) {
        // Use any default exercises we have for this body part
        List<Exercise> fallbackExercises = databaseHelper.getExercisesByBodyPart(bodyPart);
        
        // If we have some exercises, display them
        if (!fallbackExercises.isEmpty()) {
            exerciseList.clear();
            exerciseList.addAll(fallbackExercises);
            exerciseAdapter.notifyDataSetChanged();
        } else {
            // Create at least one generic exercise per body part if database is empty
            Exercise fallbackExercise = createFallbackExercise(bodyPart);
            exerciseList.add(fallbackExercise);
            exerciseAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Create a generic exercise for a body part when no data is available
     * @param bodyPart The body part
     * @return A generic Exercise object
     */
    private Exercise createFallbackExercise(String bodyPart) {
        Exercise exercise = new Exercise();
        exercise.setId("fallback-" + bodyPart.toLowerCase().replace(" ", "-"));
        
        // Set name based on body part
        if (bodyPart.equals(Constants.BODY_PART_ARMS)) {
            exercise.setName("Basic Push-ups");
            exercise.setDescription("A compound exercise that works the chest, shoulders, triceps, and core.");
            exercise.setInstructions("1. Start in a plank position with hands shoulder-width apart\n" +
                    "2. Lower your body until your chest nearly touches the floor\n" +
                    "3. Push back up to the starting position\n" +
                    "4. Repeat for the desired number of repetitions");
            exercise.setCaloriesBurned(150);
        } else if (bodyPart.equals(Constants.BODY_PART_CHEST)) {
            exercise.setName("Chest Dips");
            exercise.setDescription("An effective exercise for building chest, triceps, and shoulder strength.");
            exercise.setInstructions("1. Position yourself on parallel bars with arms fully extended\n" +
                    "2. Lower your body by bending your arms\n" +
                    "3. Keep your elbows pointed outward to target the chest\n" +
                    "4. Push back up to the starting position");
            exercise.setCaloriesBurned(200);
        } else if (bodyPart.equals(Constants.BODY_PART_BACK)) {
            exercise.setName("Pull-ups");
            exercise.setDescription("A fundamental exercise for building back and bicep strength.");
            exercise.setInstructions("1. Hang from a pull-up bar with palms facing away from you\n" +
                    "2. Pull your body up until your chin is above the bar\n" +
                    "3. Lower back down with control\n" +
                    "4. Repeat for the desired number of repetitions");
            exercise.setCaloriesBurned(220);
        } else if (bodyPart.equals(Constants.BODY_PART_ABS)) {
            exercise.setName("Plank");
            exercise.setDescription("A core stabilizing exercise that builds abdominal strength and endurance.");
            exercise.setInstructions("1. Start in a forearm plank position\n" +
                    "2. Keep your body in a straight line from head to heels\n" +
                    "3. Engage your core and hold the position\n" +
                    "4. Aim for 30-60 seconds per set");
            exercise.setCaloriesBurned(180);
        } else if (bodyPart.equals(Constants.BODY_PART_LEGS)) {
            exercise.setName("Bodyweight Squats");
            exercise.setDescription("A fundamental lower body exercise targeting quadriceps, hamstrings, and glutes.");
            exercise.setInstructions("1. Stand with feet shoulder-width apart\n" +
                    "2. Lower your body by bending knees and hips\n" +
                    "3. Keep your chest up and back straight\n" +
                    "4. Return to standing position");
            exercise.setCaloriesBurned(250);
        } else {
            exercise.setName("Jumping Jacks");
            exercise.setDescription("A full-body cardiovascular exercise.");
            exercise.setInstructions("1. Start standing with feet together and arms at sides\n" +
                    "2. Jump to a position with legs spread and arms overhead\n" +
                    "3. Jump back to the starting position\n" +
                    "4. Repeat at a quick pace");
            exercise.setCaloriesBurned(300);
        }
        
        exercise.setBodyPart(bodyPart);
        exercise.setDurationInMinutes(15);
        exercise.setDifficultyLevel(Constants.DIFFICULTY_MEDIUM);
        
        return exercise;
    }
    
    /**
     * Handle click on an exercise item
     * @param exercise The selected exercise
     */
    @Override
    public void onExerciseItemClick(Exercise exercise) {
        // TODO: Show exercise details in a dialog or new activity
        Toast.makeText(this, "Selected: " + exercise.getName(), Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Navigate to the Food Suggestion screen
     */
    private void navigateToFoodSuggestion() {
        Intent intent = new Intent(this, FoodSuggestionActivity.class);
        startActivity(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload user info in case it has changed
        loadUserInfo();
    }
}
