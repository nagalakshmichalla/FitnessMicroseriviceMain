package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAIService activityAIService;

    @RabbitListener(queues = "activity.queue")
    public void processActivity(Activity activity){

        try {
            log.info("Received activity for processing: {}",activity.getId());
            log.info("Generated Recommenadation: {}",activityAIService.generateRecommendation(activity));
        } catch (WebClientResponseException.Forbidden ex) {
            log.error("Gemini access forbidden: {}", ex.getResponseBodyAsString());
            // decide:
            // - send to DLQ
            // - store for retry
            // - acknowledge and skip
        }

    }
}
