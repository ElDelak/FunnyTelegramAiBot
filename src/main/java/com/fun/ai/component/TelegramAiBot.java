package com.fun.ai.component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;


import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

@Component
public class TelegramAiBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${openai.api-key}")
    private String openaiApiKey;
    
    @Value("${telegram.id.scammer}")
    private String idScammer;

    private final OpenAiService openAiService;
    
    public TelegramAiBot(@Value("${openai.api-key}") String key) {
        this.openAiService = new OpenAiService(key);
    }
	
	@Override
	public void onUpdateReceived(Update update) {
		if (update.hasMessage() && update.getMessage().hasText()) {
			
			Long userId = update.getMessage().getFrom().getId();
			
			if (!userId.equals(idScammer)) return; 
			
            String userMessage = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            int delay = ThreadLocalRandom.current().nextInt(3000, 8000); // 2-4 seconds
            try { 
            	SendChatAction typing = new SendChatAction();
            	typing.setChatId(chatId);
            	typing.setAction(ActionType.TYPING);

            	execute(typing);
            	Thread.sleep(delay); 

            } 
            catch (InterruptedException | TelegramApiException e) { e.printStackTrace(); }
            
            String reply = askOpenAi(userMessage);
            logConversation(userId, userMessage, reply);
            SendMessage message = new SendMessage(chatId, reply);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
		
	}
	
	private String askOpenAi(String prompt) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(List.of(new ChatMessage("user", prompt)))
            .build();
        return openAiService.createChatCompletion(request)
            .getChoices().get(0).getMessage().getContent();
    }

	@Override
	public String getBotUsername() {
		 return botUsername;
	}
	
	@Override
    public String getBotToken() {
        return botToken;
    }
	
	private void logConversation(Long userId, String input, String output) {
	    String log = String.format("[%s] User %d: %s\nBot: %s\n\n",
	            LocalDateTime.now(), userId, input, output);
	    try {
	        Files.write(Paths.get("chat-log.txt"),
	                    log.getBytes(StandardCharsets.UTF_8),
	                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}
