package com.healthmate.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.healthmate.app.R;
import com.healthmate.app.model.Exercise;

import java.util.List;

/**
 * Adapter for displaying exercise items in a RecyclerView
 */
public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {
    
    private List<Exercise> exerciseList;
    private Context context;
    private OnExerciseItemClickListener listener;
    
    /**
     * Interface for handling exercise item clicks
     */
    public interface OnExerciseItemClickListener {
        void onExerciseItemClick(Exercise exercise);
    }
    
    /**
     * Constructor for ExerciseAdapter
     * @param context Context for inflating views
     * @param exerciseList List of exercise items to display
     * @param listener Click listener for exercise items
     */
    public ExerciseAdapter(Context context, List<Exercise> exerciseList, OnExerciseItemClickListener listener) {
        this.context = context;
        this.exerciseList = exerciseList;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.exercise_item, parent, false);
        return new ExerciseViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = exerciseList.get(position);
        
        holder.exerciseName.setText(exercise.getName());
        holder.exerciseBodyPart.setText(exercise.getBodyPart());
        holder.exerciseDifficulty.setText(exercise.getDifficultyLevel());
        holder.exerciseCalories.setText(String.format("%d cal/30min", exercise.getCaloriesBurned()));
        holder.exerciseDuration.setText(String.format("%d min", exercise.getDurationInMinutes()));
        
        // Show a short preview of the description
        String descriptionPreview = exercise.getDescription();
        if (descriptionPreview.length() > 100) {
            descriptionPreview = descriptionPreview.substring(0, 97) + "...";
        }
        holder.exerciseDescription.setText(descriptionPreview);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExerciseItemClick(exercise);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return exerciseList != null ? exerciseList.size() : 0;
    }
    
    /**
     * Update the adapter's data
     * @param newExerciseList New list of exercise items
     */
    public void updateData(List<Exercise> newExerciseList) {
        this.exerciseList = newExerciseList;
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder for exercise items
     */
    public static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        TextView exerciseBodyPart;
        TextView exerciseDifficulty;
        TextView exerciseCalories;
        TextView exerciseDuration;
        TextView exerciseDescription;
        
        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            
            exerciseName = itemView.findViewById(R.id.tv_exercise_name);
            exerciseBodyPart = itemView.findViewById(R.id.tv_exercise_body_part);
            exerciseDifficulty = itemView.findViewById(R.id.tv_exercise_difficulty);
            exerciseCalories = itemView.findViewById(R.id.tv_exercise_calories);
            exerciseDuration = itemView.findViewById(R.id.tv_exercise_duration);
            exerciseDescription = itemView.findViewById(R.id.tv_exercise_description);
        }
    }
}