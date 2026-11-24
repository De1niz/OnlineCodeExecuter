package com.firstspringproject.onlinecodeexecuter.controller;

import com.firstspringproject.onlinecodeexecuter.entity.CodeExecutionRequest;
import com.firstspringproject.onlinecodeexecuter.repository.CodeExecutionRequestRepository;
import com.firstspringproject.onlinecodeexecuter.service.CodeExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code")
@CrossOrigin(origins = "*")
public class CodeExecutionController {

    @Autowired
    private CodeExecutionRequestRepository repository;

    @Autowired
    private CodeExecutionService codeExecutionService;

    @PostMapping("/execute")
    public CodeExecutionRequest executeCode(@RequestBody CodeRequest codeRequest) {
        // Save request to database
        CodeExecutionRequest request = new CodeExecutionRequest(codeRequest.getCode());
        request = repository.save(request);

        // Execute code asynchronously (Kafka yerine direkt async method)
        codeExecutionService.executeCodeAsync(request.getId());

        return request;
    }

    @GetMapping("/status/{id}")
    public CodeExecutionRequest getStatus(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    @PostMapping("/input")
    public void provideInput(@RequestBody InputRequest inputRequest) {
        codeExecutionService.provideInput(inputRequest.getRequestId(), inputRequest.getInput());
    }

    @PostMapping("/stop")
    public void stopExecution(@RequestBody StopRequest stopRequest) {
        codeExecutionService.stopExecution(stopRequest.getRequestId());
    }

    public static class InputRequest {
        private Long requestId;
        private String input;

        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
    }

    public static class StopRequest {
        private Long requestId;

        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }
    }

    public static class CodeRequest {
        private String code;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
