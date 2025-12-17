package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    //it will generate the recommendations
    //it will work with Gemini service
    private final GeminiService geminiService;
    public String generateRecommendation(Activity activity){
        String prompt=createPromptForActivity(activity);
        String aiResponse=geminiService.getAnswer(prompt);
        log.info("RESPONSE FROM AI: {}", aiResponse);
        return aiResponse;
    }

    private String createPromptForActivity(Activity activity) {
      return String.format("""
              Analyze this fitness activity and provide detailed recommendation in the following EXACT JSON format:
              {
                "analysis: {
                "ovrall": "Overall analysis here",
                "pace: "Pace analysis here",
                "heartRate": "Heart rate analysis here",
                "caloriesBurned": "Calaories analysis here"
                },
                "improvements": [
                  {
                    "area": "Area name",
                    "recommenadation": "Detailed recommenadation"
                   }
                  ],
                  "suggestions": [
                  {
                   "workout": "Workout name",
                   "description": "Deatailed workout description"
                   }
                  ],
                  "safety": [
                    "Safety point 1",
                    "Safety point 2"
                   ],
                   "food": [
                     "detailed healthy food description",
                     "detailed diet plan"
                     ]
                  }
                  
                  Analyze this activity:
                  Activity Type: %s
                  Duration: %d minutes
                  Calories Burned: %d
                  Additional Metrics: %s
                  
                  Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety guidelines and dilet plan,
                  Ensure the response follows the EXACT JSON format shown above.
                   """,
                    activity.getType(),
                    activity.getDuration(),
                    activity.getCaloriesBurned(),
                    activity.getAdditionalMetrics()
              );
    }

}
