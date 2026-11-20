package sk.kasv.fx.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sk.kasv.fx.service.ChatService;
import java.util.List;
import static sk.kasv.fx.dto.ApiDtos.*;

@RestController
@Validated
@RequestMapping("/api/orders/{id}/messages")
public class ChatController {
    private final ChatService chat;
    public ChatController(ChatService chat) { this.chat = chat; }

    /** after: id of the last message the client already has (0 = from the start). Poll every few seconds. */
    @GetMapping
    public List<MessageResponse> list(@PathVariable @Positive Long id, @RequestParam(defaultValue = "0") @Min(0) long after) {
        return chat.list(id, after);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable @Positive Long id, @Valid @RequestBody MessageRequest request) {
        return chat.send(id, request);
    }
}
