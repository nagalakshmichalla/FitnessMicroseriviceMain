package com.fitness.activityservice.service;

import com.fitness.activityservice.ActivityRepository;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    public final ActivityRepository activityRepository;
    public final UserValidationService userValidationService;
    private final RabbitTemplate rabbitTemplate;

    //we are getting thus value from application.yml
    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    //we are getting this property from yml configuration
    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public ActivityResponse trackActivity(ActivityRequest request){

        boolean isValidUser= userValidationService.validateUser(request.getUserId());
        if(!isValidUser){
            throw new RuntimeException("Inavalid user: "+request.getUserId());
        }

        //need to save it into the database//
        //1.we have two microservices as of now Activitivy Microservice and User microservice
        //somewhere these two services would communicate each other
        //the userId has to make validate from the user microservice.
        //the end user does not know what's happening inside, they thougt of running single application
        //it will run as a product of application
        Activity activity=Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurned(request.getCaloriesBurned())
                .startTime(request.getStartTime())
                .additionalMetrics(request.getAdditionalMetrics())
                .build();
        Activity savedActivity=activityRepository.save(activity);

        //publish to RabbitMQ for AI Processing

        try {
            rabbitTemplate.convertAndSend(exchange,routingKey,savedActivity);
        } catch(Exception e) {
            log.error("Failed to publish activity to RabbitMQ: ",e);
        }

        return mapToResponse(savedActivity);
    }
    private ActivityResponse mapToResponse(Activity activity){
        ActivityResponse response=new ActivityResponse();
        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setDuration(activity.getDuration());
        response.setCaloriesBurned(activity.getCaloriesBurned());
        response.setStartTime(activity.getStartTime());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setUpdatedAt(activity.getUpdatedAt());
        return response;
    }


    public List<ActivityResponse> getUserActivities(String userId) {
        List<Activity> activities=activityRepository.findByUserId(userId);
        return  activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());//taking the activity and convert it into activity response

    }

    public ActivityResponse getUserActivityById(String activityId) {
        return activityRepository.findById(activityId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Activity not found:" +activityId));
    }
}
