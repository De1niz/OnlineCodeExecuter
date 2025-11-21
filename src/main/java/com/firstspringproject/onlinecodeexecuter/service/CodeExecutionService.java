package com.firstspringproject.onlinecodeexecuter.service;

import com.firstspringproject.onlinecodeexecuter.entity.CodeExecutionRequest;
import com.firstspringproject.onlinecodeexecuter.repository.CodeExecutionRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class CodeExecutionService {

    @Autowired
    private CodeExecutionRequestRepository repository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Timeout configuration
    private static final int COMPILATION_TIMEOUT_SECONDS = 10;
    private static final int EXECUTION_TIMEOUT_SECONDS = 30;

    // Input handling mekanizması
    private static final Map<Long, BlockingQueue<String>> inputQueues = new ConcurrentHashMap<>();
    private static final Map<Long, Thread> executionThreads = new ConcurrentHashMap<>();
    private static final Map<Long, Process> executionProcesses = new ConcurrentHashMap<>();

    // Debug ve kontrol için
    private static final Map<Long, Boolean> messageSent = new ConcurrentHashMap<>();

    @Async
    @Transactional
    public void executeCodeAsync(Long requestId) {
        long startTime = System.currentTimeMillis();
        CodeExecutionRequest request = null;
        Thread currentThread = Thread.currentThread();
        executionThreads.put(requestId, currentThread);
        messageSent.put(requestId, false);

        try {
            request = repository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

            // Input queue oluştur
            inputQueues.put(requestId, new LinkedBlockingQueue<>());

            request.markAsProcessing();
            repository.save(request);

            // SADECE BİR KERE MESAJ GÖNDER - duplicate'ı önle
            if (!messageSent.get(requestId)) {
                sendWebSocketUpdate(requestId, "🚀 Launching your code...\n⏳ Please wait while we execute your Java program...", "PROCESSING");
                messageSent.put(requestId, true);
            }

            String output = executeJavaCodeWithInput(request.getCode(), requestId);
            long executionTime = System.currentTimeMillis() - startTime;

            request = repository.findById(requestId).orElseThrow(() -> new RuntimeException("Request not found after execution"));
            request.markAsCompleted(output, executionTime);
            repository.save(request);

            // Sonuç mesajını gönder
            sendWebSocketUpdate(requestId, output, "COMPLETED");

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            handleExecutionError(requestId, e, executionTime, request);
        } finally {
            // Temizlik
            inputQueues.remove(requestId);
            executionThreads.remove(requestId);
            executionProcesses.remove(requestId);
            messageSent.remove(requestId);
        }
    }

    private String executeJavaCodeWithInput(String code, Long requestId) {
        long startTime = System.currentTimeMillis();
        File tempDir = null;
        String className = null;
        File javaFile = null;

        try {
            if (code == null || code.trim().isEmpty()) {
                return "❌ Error: Empty code provided!\n\n💡 Please write some Java code to execute.";
            }

            if (code.length() > 10000) {
                return "❌ Error: Code too long!\n\n💡 Maximum code length is 10,000 characters.";
            }

            className = determineClassName(code);
            tempDir = createTempDirectory();

            // Scanner kullanan kodları tespit et
            String javaCode = adaptCodeForInputHandling(code, className, requestId);

            javaFile = writeJavaFile(tempDir, className, javaCode);
            if (javaFile == null) {
                return "❌ System Error: Could not create temporary file";
            }

            String compileError = compileJavaCode(javaFile);
            if (compileError != null) {
                return formatCompilationError(compileError, System.currentTimeMillis() - startTime);
            }

            ProcessOutput processOutput = executeJavaProgramWithInput(tempDir, className, requestId, startTime);
            if (processOutput.isTimeout()) {
                return formatTimeoutError(System.currentTimeMillis() - startTime);
            }
            if (processOutput.hasError()) {
                return formatRuntimeError(processOutput.getError(), System.currentTimeMillis() - startTime);
            }

            return formatSuccessOutput(processOutput.getOutput(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            return "❌ System Error: " + e.getMessage() +
                    "\n⏱️ Failed after: " + (System.currentTimeMillis() - startTime) + "ms" +
                    "\n\n🔧 Please try again or contact support.";
        } finally {
            cleanupTempFiles(tempDir, className, javaFile);
        }
    }

    // EN GÜVENLİ INPUT HANDLING - HER ZAMAN WRAPPER KULLAN
    private String adaptCodeForInputHandling(String originalCode, String className, Long requestId) {
        // Eğer Scanner içeriyorsa, HER ZAMAN wrapper içine al
        if (originalCode.contains("Scanner") && originalCode.contains("System.in")) {
            return buildUniversalInteractiveClass(originalCode, className, requestId);
        }

        // Normal kod için standart wrapping
        return buildCompleteJavaClass(originalCode, className);
    }

    private String buildUniversalInteractiveClass(String userCode, String className, Long requestId) {
        // HER ZAMAN wrapper içine al - böylece import sorunu olmaz
        String processedCode = userCode.replace("System.in", "new UniversalInputStream(" + requestId + ")");

        return "import java.util.Scanner;\n" +
                "import java.io.*;\n" +
                "\n" +
                "public class " + className + " {\n" +
                "    public static void main(String[] args) {\n" +
                "        " + processedCode + "\n" +
                "    }\n" +
                "}\n\n" +
                "class UniversalInputStream extends InputStream {\n" +
                "    private byte[] buffer = new byte[1024];\n" +
                "    private int pos = 0;\n" +
                "    private int count = 0;\n" +
                "    private final long requestId;\n" +
                "    \n" +
                "    public UniversalInputStream(long requestId) {\n" +
                "        this.requestId = requestId;\n" +
                "    }\n" +
                "    \n" +
                "    public int read() {\n" +
                "        if (pos >= count) {\n" +
                "            // Input isteği gönder\n" +
                "            System.out.println(\"\\n💡 [INPUT_REQUIRED]\");\n" +
                "            System.out.flush();\n" +
                "            \n" +
                "            // Input bekleyen kodu çalıştır\n" +
                "            waitForInput();\n" +
                "            \n" +
                "            if (count == 0) {\n" +
                "                return '\\n'; // Default newline\n" +
                "            }\n" +
                "        }\n" +
                "        return buffer[pos++];\n" +
                "    }\n" +
                "    \n" +
                "    public int available() {\n" +
                "        return count - pos;\n" +
                "    }\n" +
                "    \n" +
                "    private void waitForInput() {\n" +
                "        try {\n" +
                "            // 30 saniye timeout\n" +
                "            for (int i = 0; i < 300; i++) {\n" +
                "                Thread.sleep(100);\n" +
                "                if (count > 0) break;\n" +
                "            }\n" +
                "        } catch (InterruptedException e) {\n" +
                "            Thread.currentThread().interrupt();\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    public void provideInput(String input) {\n" +
                "        byte[] bytes = (input + \"\\n\").getBytes();\n" +
                "        System.arraycopy(bytes, 0, buffer, 0, bytes.length);\n" +
                "        count = bytes.length;\n" +
                "        pos = 0;\n" +
                "    }\n" +
                "}";
    }

    private ProcessOutput executeJavaProgramWithInput(File tempDir, String className, Long requestId, long startTime) throws IOException, InterruptedException {
        ProcessBuilder executeProcess = new ProcessBuilder("java", "-cp", tempDir.getAbsolutePath(), className);
        Process execute = executeProcess.start();
        executionProcesses.put(requestId, execute);

        // Input handling thread
        Thread inputThread = new Thread(() -> {
            try {
                handleProgramInput(execute, requestId);
            } catch (IOException e) {
                System.err.println("Input handling error: " + e.getMessage());
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();

        // Output reading
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(execute.getInputStream());
            } catch (IOException e) {
                return "Error reading output: " + e.getMessage();
            }
        });

        CompletableFuture<String> errorFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return readStream(execute.getErrorStream());
            } catch (IOException e) {
                return "Error reading error stream: " + e.getMessage();
            }
        });

        // Timeout kontrolü
        boolean executionFinished = execute.waitFor(EXECUTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!executionFinished) {
            execute.destroyForcibly();
            executionProcesses.remove(requestId);
            Thread.sleep(100);
            return ProcessOutput.timeout();
        }

        String output, error;
        try {
            output = outputFuture.get(5, TimeUnit.SECONDS);
            error = errorFuture.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException e) {
            output = "Error reading output";
            error = "Error reading error stream";
        }

        executionProcesses.remove(requestId);
        return new ProcessOutput(output, error, false);
    }

    private void handleProgramInput(Process process, Long requestId) throws IOException {
        BlockingQueue<String> inputQueue = inputQueues.get(requestId);
        if (inputQueue == null) return;

        try (OutputStream processInput = process.getOutputStream();
             PrintWriter writer = new PrintWriter(processInput)) {

            while (process.isAlive()) {
                try {
                    String input = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (input != null) {
                        writer.println(input);
                        writer.flush();
                        System.out.println("Input provided to program: " + input);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // Input handler method
    public void provideInput(Long requestId, String input) {
        BlockingQueue<String> queue = inputQueues.get(requestId);
        if (queue != null) {
            boolean added = queue.offer(input);
            if (added) {
                System.out.println("Input added to queue for request " + requestId + ": " + input);
            } else {
                System.err.println("Failed to add input to queue for request: " + requestId);
            }
        } else {
            System.err.println("No input queue found for request: " + requestId);
        }
    }

    // Stop execution method
    public void stopExecution(Long requestId) {
        Thread thread = executionThreads.get(requestId);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

        Process process = executionProcesses.get(requestId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }

        // Cleanup
        inputQueues.remove(requestId);
        executionThreads.remove(requestId);
        executionProcesses.remove(requestId);
        messageSent.remove(requestId);
    }

    // Orijinal metodlar
    private String determineClassName(String code) {
        String publicClassName = extractPublicClassName(code);
        if (publicClassName != null) return publicClassName;

        String className = extractClassName(code);
        if (className != null) return className;

        return "UserCode_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String extractPublicClassName(String code) {
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractClassName(String code) {
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String buildCompleteJavaClass(String userCode, String className) {
        if (hasCompleteStructure(userCode)) return userCode;

        if (hasMainMethod(userCode) && !hasClassDefinition(userCode)) {
            return "public class " + className + " {\n" + userCode + "\n}";
        }

        if (!hasClassDefinition(userCode)) {
            return "public class " + className + " {\n" +
                    "    public static void main(String[] args) {\n" +
                    "        try {\n" +
                    "            " + userCode + "\n" +
                    "        } catch (Exception e) {\n" +
                    "            System.out.println(\"❌ Exception: \" + e.getMessage());\n" +
                    "            e.printStackTrace();\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
        }

        return userCode;
    }

    private boolean hasCompleteStructure(String code) {
        return hasClassDefinition(code) && hasMainMethod(code);
    }

    private boolean hasClassDefinition(String code) {
        return code.contains("class ");
    }

    private boolean hasMainMethod(String code) {
        return code.contains("public static void main") || code.contains("static void main");
    }

    private File createTempDirectory() throws IOException {
        File tempDir = new File("temp-code-execution");
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IOException("Could not create temp directory: " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }

    private File writeJavaFile(File tempDir, String className, String javaCode) throws IOException {
        File javaFile = new File(tempDir, className + ".java");
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(javaCode);
        }
        return javaFile;
    }

    private String compileJavaCode(File javaFile) throws IOException, InterruptedException {
        ProcessBuilder compileProcess = new ProcessBuilder("javac", javaFile.getAbsolutePath());
        Process compile = compileProcess.start();

        boolean compileFinished = compile.waitFor(COMPILATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!compileFinished) {
            compile.destroy();
            return "Compilation timeout: javac took longer than " + COMPILATION_TIMEOUT_SECONDS + " seconds";
        }

        if (compile.exitValue() != 0) {
            return readStream(compile.getErrorStream());
        }
        return null;
    }

    private String formatCompilationError(String error, long executionTime) {
        String cleanError = error.trim();
        return "❌ Compilation Error:\n" + cleanError + "\n⏱️ Failed after: " + executionTime + "ms";
    }

    private String formatRuntimeError(String error, long executionTime) {
        String cleanError = error.trim();
        return "❌ Runtime Error:\n" + cleanError + "\n⏱️ Failed after: " + executionTime + "ms";
    }

    private String formatTimeoutError(long executionTime) {
        return "⏰ Execution Timeout:\nCode execution exceeded " + EXECUTION_TIMEOUT_SECONDS + " second limit\n⏱️ Timed out after: " + executionTime + "ms";
    }

    private String formatSuccessOutput(String output, long executionTime) {
        if (output == null || output.trim().isEmpty()) {
            return "✅ Execution Successful!\n\n📝 Program ran without output\n⏱️ Execution time: " + executionTime + "ms";
        } else {
            return "✅ Execution Successful!\n\n📋 Output:\n" + output.trim() + "\n⏱️ Execution time: " + executionTime + "ms";
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!isNoiseLine(line)) {
                    content.append(line).append("\n");
                }
            }
            return content.toString().trim();
        }
    }

    private boolean isNoiseLine(String line) {
        return line.contains("Picked up JAVA_TOOL_OPTIONS") ||
                line.contains("Picked up _JAVA_OPTIONS") ||
                line.trim().isEmpty();
    }

    private void cleanupTempFiles(File tempDir, String className, File javaFile) {
        try {
            if (javaFile != null && javaFile.exists()) {
                javaFile.delete();
            }
            if (className != null && tempDir != null) {
                File classFile = new File(tempDir, className + ".class");
                if (classFile.exists()) {
                    classFile.delete();
                }
            }
            if (tempDir != null && tempDir.exists()) {
                File[] classFiles = tempDir.listFiles((dir, name) -> name.endsWith(".class"));
                if (classFiles != null) {
                    for (File file : classFiles) {
                        file.delete();
                    }
                }
            }
        } catch (SecurityException e) {
            System.err.println("Cleanup warning: " + e.getMessage());
        }
    }

    private void sendWebSocketUpdate(Long requestId, String output, String status) {
        try {
            messagingTemplate.convertAndSend("/topic/execution-result",
                    new ExecutionResult(requestId, output, status));
        } catch (Exception e) {
            System.err.println("WebSocket error: " + e.getMessage());
        }
    }

    private void handleExecutionError(Long requestId, Exception e, long executionTime, CodeExecutionRequest request) {
        System.err.println("Execution error for request " + requestId + ": " + e.getMessage());

        try {
            CodeExecutionRequest errorRequest = repository.findById(requestId).orElse(null);
            if (errorRequest != null) {
                errorRequest.markAsFailed("System Error: " + e.getMessage(), executionTime);
                repository.save(errorRequest);
            }
        } catch (Exception ex) {
            System.err.println("Failed to update error status: " + ex.getMessage());
        }

        String errorMessage = "❌ System Error: " + e.getMessage() + "\n⏱️ Failed after: " + executionTime + "ms";
        sendWebSocketUpdate(requestId, errorMessage, "ERROR");
    }

    private static class ProcessOutput {
        private final String output;
        private final String error;
        private final boolean timeout;

        public ProcessOutput(String output, String error, boolean timeout) {
            this.output = output;
            this.error = error;
            this.timeout = timeout;
        }

        public static ProcessOutput timeout() {
            return new ProcessOutput(null, null, true);
        }

        public String getOutput() { return output; }
        public String getError() { return error; }
        public boolean isTimeout() { return timeout; }
        public boolean hasError() { return error != null && !error.trim().isEmpty(); }
    }

    public static class ExecutionResult {
        private Long requestId;
        private String output;
        private String status;

        public ExecutionResult() {}
        public ExecutionResult(Long requestId, String output, String status) {
            this.requestId = requestId;
            this.output = output;
            this.status = status;
        }

        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}