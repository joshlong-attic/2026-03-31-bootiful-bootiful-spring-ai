package com.example.assistant;

import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;

import java.util.List;

import static org.springaicommunity.mcp.security.client.sync.config.McpClientOAuth2Configurer.mcpClientOAuth2;

@SpringBootApplication
public class AssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssistantApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return http -> http
                .with(mcpClientOAuth2())
                .authorizeHttpRequests(r -> r
                        .requestMatchers("/ask").permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                );
    }

    @Bean
    PromptChatMemoryAdvisor memoryAdvisor(DataSource dataSource) {
        var jdbc = JdbcChatMemoryRepository
                .builder()
                .dataSource(dataSource)
                .build();
        var mwa = MessageWindowChatMemory
                .builder()
                .chatMemoryRepository(jdbc)
                .build();
        return PromptChatMemoryAdvisor
                .builder(mwa)
                .build();
    }

    @Bean
    QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return QuestionAnswerAdvisor
                .builder(vectorStore)
                .build();
    }

}

interface DogRepository extends ListCrudRepository<Dog, Integer> {
}

record Dog(@Id int id, String name, String description) {
}

@Controller
@ResponseBody
class AssistantController {

    private final ChatClient ai;

    AssistantController(
            ToolCallbackProvider scheduler,
            QuestionAnswerAdvisor questionAnswerAdvisor,
            DogRepository repository,
            VectorStore vectorStore,
            PromptChatMemoryAdvisor memoryAdvisor,
            ChatClient.Builder ai) {

        if (false) {
            repository.findAll().forEach(dog -> {
                var dogument = new Document("id: %s, name: %s, description: %s".formatted(
                        dog.id(), dog.name(), dog.description()
                ));
                vectorStore.add(List.of(dogument));
            });
        }

        var skills = SkillsTool
                .builder()
                .addSkillsResource(new ClassPathResource("/META-INF/skills"))
                .build();

        var system = """
                You are an AI powered assistant to help people adopt a dog from the adoptions agency named Pooch Palace 
                with locations in Antwerp, Seoul, Tokyo, Singapore, Paris, Mumbai, New Delhi, Barcelona, San Francisco, 
                and London. Information about the dogs availables will be presented below. If there is no information, 
                then return a polite response suggesting wes don't have any dogs available.
                
                If somebody asks for a time to pick up the dog, don't ask other questions: simply provide a time by consulting the tools you have available.
                """;
        var toolCallAdvisor = ToolCallAdvisor
                .builder()
                .conversationHistoryEnabled(true)
                .build();
        this.ai = ai
                .defaultAdvisors(toolCallAdvisor, questionAnswerAdvisor, memoryAdvisor)
                .defaultToolCallbacks(scheduler)
                .defaultToolCallbacks(skills)
                .defaultSystem(system)
                .build();
    }

    // what are some amusing or very odd differences between the chihuahua and poodle breeds?

    @GetMapping("/ask")
    String ask(@RequestParam(defaultValue = """
            schedule an appointment for Prancer from the Paris Pooch Palace
            location using whatever tools you've got available. 
            """) String question) {
        return this.ai
                .prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getName()
                ))
                .user(question)
                .call()
                .content();
    }

}


record DogAdoptionSuggestion(int id, String name) {
}