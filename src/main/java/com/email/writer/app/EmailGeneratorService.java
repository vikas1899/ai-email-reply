package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {


    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }


    public String generateEmail(@RequestBody EmailRequest emailRequest){
        // Build the prompt
        String prompt = buildPrompt(emailRequest);

        // Craft the request
        Map<String,Object>requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts",new Object[] {
                                Map.of("text",prompt)
                        })
                }
        );

        // Do request and get response

        String response = webClient.post()
                .uri(geminiApiUrl+"?key="+apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve().bodyToMono(String.class)
                .block();

        // return the response

        return extractResponseContent(response);

    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode  rootNode = objectMapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

        }catch(Exception e){
            return "Error processing response :"+ e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional reply for the following email content. Please don't generate a subject line. ");

        if(emailRequest.getTone() != null &&  !emailRequest.getTone().isEmpty()){
            prompt.append("Use a").append(emailRequest.getTone()).append(" tone.");
        }

        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
