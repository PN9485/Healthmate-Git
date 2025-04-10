package com.healthmate.app;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.healthmate.app.util.ApiConfig;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    // Splash screen duration in milliseconds
    private static final int SPLASH_DURATION = 2000;
    
    // UI Elements
    private ImageView logoImageView;
    private TextView appNameTextView, taglineTextView;
    
    // Animations
    private Animation topAnimation, bottomAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide status bar for a more immersive experience
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_main);
        
        // Initialize API configuration
        initializeApiConfig();
        
        // Initialize animations
        topAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        bottomAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        
        // Set animation duration
        topAnimation.setDuration(1500);
        bottomAnimation.setDuration(1500);
        
        // Initialize UI elements
        logoImageView = findViewById(R.id.logoImageView);
        appNameTextView = findViewById(R.id.appNameTextView);
        taglineTextView = findViewById(R.id.taglineTextView);
        
        // Apply animations
        logoImageView.setAnimation(topAnimation);
        appNameTextView.setAnimation(bottomAnimation);
        taglineTextView.setAnimation(bottomAnimation);
        
        // Navigate to LoginActivity after the splash screen duration
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }
    
    /**
     * Initialize API configuration
     */
    private void initializeApiConfig() {
        try {
            // Initialize API keys from environment variables and manifest metadata
            ApiConfig.initialize(getApplicationContext());
            
            // If the API key wasn't found in environment or manifest, use a direct approach
            if (ApiConfig.EXERCISE_DB_API_KEY.isEmpty()) {
                // Try to get the API key from system environment (works in Replit environment)
                String apiKeyEnv = System.getenv("RAPID_API_KEY");
                if (apiKeyEnv != null && !apiKeyEnv.isEmpty()) {
                    ApiConfig.setApiKey(apiKeyEnv);
                    Log.d(TAG, "Set API key from environment variable");
                } else {
                    Log.e(TAG, "Failed to find API key in any location");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing API configuration", e);
        }
    }
}
