package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {
    //it will generate the recommendations
    //it will work with Gemini service
    private final GeminiService geminiService;
    public Recommendation generateRecommendation(Activity activity){
        String prompt=createPromptForActivity(activity);
        String aiResponse=geminiService.getAnswer(prompt);
        log.info("RESPONSE FROM AI: {}", aiResponse);
        return porcessAiResponse(activity,aiResponse);
    }

    private Recommendation porcessAiResponse(Activity activity, String aiResponse){
        try {
            ObjectMapper mapper=new ObjectMapper();
            JsonNode rootNode=mapper.readTree(aiResponse);

            JsonNode textNode=rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent=textNode.asText()
                    .replaceAll("```json\\n","")
                    .replaceAll("\\n```","")
                    .trim();

         //   log.info("PARSED RESPONSE FROM AI: {}", jsonContent);
            JsonNode analysisJson=mapper.readTree(jsonContent);
            JsonNode analysisNode=analysisJson.path("analysis");
            StringBuilder fullAnalysis=new StringBuilder();
            addAnalysisSection(fullAnalysis,analysisNode,"overall","Overall:");
            addAnalysisSection(fullAnalysis,analysisNode,"pace","Pace:");
            addAnalysisSection(fullAnalysis,analysisNode,"heartRate","Heart Rate:");
            addAnalysisSection(fullAnalysis,analysisNode,"overall","Overall:");
            addAnalysisSection(fullAnalysis,analysisNode,"caloriesBurned","Calories:");


            List<String> improvements=extractImprovements(analysisJson.path("improvements"));

            List<String> suggestions=extractSuggestions(analysisJson.path("suggestions"));
            List<String> safety=extractSafetyGuidelines(analysisJson.path("safety"));
            List<String> food=extractFoodGuidelines(analysisJson.path("food"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getType())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestion(suggestions)
                    .safety(safety)
                    .food(food)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }

    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestion(Collections.singletonList("Consider consulting a fitness professional"))
                .safety(Arrays.asList(
                        "Always warm up before excercise",
                        "Stay hydrated",
                        "Listen to your body"
                ))
                .food(Arrays.asList(
                        "prepare your diet according to your preference",
                        "eat nutritious food",
                        "try to avoid junk food if possible"
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<String> extractFoodGuidelines(JsonNode foodNode) {
        List<String> food=new ArrayList<>();
        if(foodNode.isArray()){
            foodNode.forEach(details-> food.add(details.asText()));
        }
        return food.isEmpty() ?
                Collections.singletonList("Prefer nutritious food instead of junk food") :
                food;
    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safety=new ArrayList<>();
        if(safetyNode.isArray()){
            safetyNode.forEach(item-> safety.add(item.asText()));
        }
        return safety.isEmpty() ?
                Collections.singletonList("Follow general safety guidelines") :
                safety;
    }

    private List<String> extractSuggestions(JsonNode suggestionNode) {
        List<String> suggestions=new ArrayList<>();
        if(suggestionNode.isArray()){
            suggestionNode.forEach(suggestion->{
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));
                    });
        }
        return suggestions.isEmpty() ?
                Collections.singletonList("follow general suggestions given by doctor") :
                suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if (improvementsNode.isArray()) {
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String detail = improvement.path("recommenadation").asText();
                improvements.add(String.format("%s: %s", area, detail));
            });
        }
        return improvements.isEmpty() ?
                Collections.singletonList("No specific improvements provided") :
                improvements;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if(!analysisNode.path(key).isMissingNode()){
          fullAnalysis.append(prefix)
                  .append(analysisNode.path(key).asText())
                  .append("\n\n");
        }
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
