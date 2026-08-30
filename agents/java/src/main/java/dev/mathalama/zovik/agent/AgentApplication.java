package dev.mathalama.zovik.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class AgentApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentApplication.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private static String masterUrl;
    private static String token;
    private static DockerClient dockerClient;
    private static Path workspacesDir;
    private static AgentClient webSocketClient;

    public static void main(String[] args) {
        log.info("Starting Zovik Agent...");

        // Parse CLI arguments
        for (String arg : args) {
            if (arg.startsWith("--master=")) {
                masterUrl = arg.substring("--master=".length());
            } else if (arg.startsWith("--token=")) {
                token = arg.substring("--token=".length());
            }
        }

        if (masterUrl == null || token == null) {
            log.error("Missing required arguments. Usage: java -jar agent.jar --master=http://<ip>:<port> --token=<token>");
            System.exit(1);
        }

        // Initialize Docker Client
        try {
            DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();
            dockerClient = DockerClientImpl.getInstance(config, httpClient);
            dockerClient.pingCmd().exec();
            log.info("Successfully connected to Docker Daemon at {}", config.getDockerHost());
        } catch (Exception e) {
            log.error("Failed to connect to local Docker Daemon. Make sure Docker is running.", e);
            System.exit(1);
        }

        // Setup local workspaces dir
        workspacesDir = Paths.get(System.getProperty("user.home"), ".zovik-agent", "workspaces");
        try {
            Files.createDirectories(workspacesDir);
        } catch (IOException e) {
            log.error("Failed to create workspaces directory", e);
            System.exit(1);
        }

        // Start WebSocket Connection Client
        connectWebSocket();
    }

    private static void connectWebSocket() {
        String wsUrl = masterUrl.replace("http://", "ws://").replace("https://", "wss://") + "/api/v1/runners/ws?token=" + token;
        log.info("Connecting to Master Server WebSocket at {} ...", wsUrl);

        try {
            webSocketClient = new AgentClient(URI.create(wsUrl));
            webSocketClient.connect();
        } catch (Exception e) {
            log.error("Failed to connect to master", e);
            scheduleReconnect();
        }
    }

    private static void scheduleReconnect() {
        log.info("Scheduling reconnect in 5 seconds...");
        executor.submit(() -> {
            try {
                Thread.sleep(5000);
                connectWebSocket();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    // WebSocket Client class
    private static class AgentClient extends WebSocketClient {

        public AgentClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            log.info("Connected to Zovik Master! Handshake status: {}", handshakedata.getHttpStatus());
            // Send dynamic system status to Master
            sendSystemInfo();
        }

        @Override
        public void onMessage(String message) {
            log.info("Received message from Master: {}", message);
            try {
                Map<String, Object> payload = mapper.readValue(message, new TypeReference<Map<String, Object>>() {});
                String action = (String) payload.get("action");

                if ("EXECUTE_STEP".equalsIgnoreCase(action)) {
                    executor.submit(() -> executeBuildStep(payload));
                }
            } catch (Exception e) {
                log.error("Error processing websocket message", e);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            log.warn("Connection to Master closed. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
            scheduleReconnect();
        }

        @Override
        public void onError(Exception ex) {
            log.error("WebSocket connection error", ex);
        }

        private void sendSystemInfo() {
            try {
                Map<String, Object> sysInfo = new HashMap<>();
                sysInfo.put("type", "INIT");
                sysInfo.put("os", System.getProperty("os.name"));
                sysInfo.put("cpuCores", Runtime.getRuntime().availableProcessors());
                sysInfo.put("memoryBytes", Runtime.getRuntime().maxMemory());
                send(mapper.writeValueAsString(sysInfo));
            } catch (Exception e) {
                log.error("Failed to send system info", e);
            }
        }
    }

    // Step Execution Logic
    private static void executeBuildStep(Map<String, Object> payload) {
        Long buildId = Long.valueOf(payload.get("buildId").toString());
        Long stepId = Long.valueOf(payload.get("stepId").toString());
        String stepName = (String) payload.get("stepName");
        String repoUrl = (String) payload.get("repoUrl");
        String branch = (String) payload.get("branch");
        String commitSha = (String) payload.get("commitSha");
        String dockerImage = (String) payload.get("dockerImage");
        List<String> commands = (List<String>) payload.get("commands");
        Map<String, String> envVars = (Map<String, String>) payload.get("envVars");
        boolean privileged = Boolean.TRUE.equals(payload.get("privileged"));
        boolean dockerSocket = Boolean.TRUE.equals(payload.get("dockerSocket"));
        Map<String, String> secretFiles = (Map<String, String>) payload.get("secretFiles");
        List<String> artifacts = (List<String>) payload.get("artifacts");
        String gitToken = (String) payload.get("gitToken");

        Path workspacePath = workspacesDir.resolve("build-" + buildId);

        try {
            log.info("Starting task: {} for build ID: {}", stepName, buildId);

            // 1. Setup local workspace & Clone/Checkout Repo code locally
            cloneOrPullRepository(repoUrl, gitToken, branch, commitSha, workspacePath);

            // 2. Inject Secret Files into workspace
            if (secretFiles != null && !secretFiles.isEmpty()) {
                for (Map.Entry<String, String> entry : secretFiles.entrySet()) {
                    Path filePath = workspacePath.resolve(entry.getKey()).normalize();
                    if (!filePath.startsWith(workspacePath)) {
                        throw new Exception("Invalid secret path: " + entry.getKey());
                    }
                    Files.createDirectories(filePath.getParent());
                    Files.writeString(filePath, entry.getValue());
                    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                        Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rw-------"));
                    }
                }
            }

            // 3. Pull Docker image
            log.info("Pulling Docker Image: {}", dockerImage);
            dockerClient.pullImageCmd(dockerImage)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();

            // 4. Configure local bind mounts for step
            List<Bind> binds = new ArrayList<>();
            // Mount the local agent directory workspace to container workspace /app
            binds.add(new Bind(workspacePath.toAbsolutePath().toString(), new Volume("/app")));
            if (dockerSocket || privileged) {
                binds.add(new Bind("/var/run/docker.sock", new Volume("/var/run/docker.sock")));
            }

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withBinds(binds)
                    .withPrivileged(privileged);

            List<String> envs = new ArrayList<>();
            if (envVars != null) {
                envVars.forEach((k, v) -> envs.add(k + "=" + v));
            }

            // Commands shell wrapping
            String cmdScript = "set -e\ncd /app\n" + String.join("\n", commands);

            // Create Container
            CreateContainerResponse container = dockerClient.createContainerCmd(dockerImage)
                    .withHostConfig(hostConfig)
                    .withEnv(envs)
                    .withWorkingDir("/app")
                    .withEntrypoint("sh", "-c", cmdScript)
                    .exec();

            String containerId = container.getId();
            log.info("Created docker container {} for step {}", containerId, stepName);

            // Start container
            dockerClient.startContainerCmd(containerId).exec();

            // Stream logs in real-time back to Master via WebSocket
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame item) {
                            try {
                                Map<String, Object> logMsg = new HashMap<>();
                                logMsg.put("type", "LOG");
                                logMsg.put("buildId", buildId);
                                logMsg.put("stepId", stepId);
                                logMsg.put("content", new String(item.getPayload()));
                                webSocketClient.send(mapper.writeValueAsString(logMsg));
                            } catch (Exception e) {
                                log.error("Failed to send log line", e);
                            }
                        }
                    }).awaitCompletion();

            // Wait for completion and get exit code
            int exitCode = dockerClient.waitContainerCmd(containerId)
                    .start()
                    .awaitStatusCode();

            log.info("Container exited for step {} with code: {}", stepName, exitCode);

            // Cleanup container
            try {
                dockerClient.removeContainerCmd(containerId).exec();
            } catch (Exception e) {
                log.warn("Failed to remove container {}", containerId, e);
            }

            // 5. Gather and upload artifacts back to Master if success
            if (exitCode == 0) {
                List<String> stepArtifacts = artifacts;
                if (stepArtifacts == null || stepArtifacts.isEmpty()) {
                    stepArtifacts = findArtifactsFromYaml(workspacePath, stepName);
                }
                if (stepArtifacts != null && !stepArtifacts.isEmpty()) {
                    collectAndUploadArtifacts(workspacePath, buildId, stepName, stepArtifacts);
                }
            }

            // 6. Notify Master of completion
            Map<String, Object> doneMsg = new HashMap<>();
            doneMsg.put("type", "COMPLETED");
            doneMsg.put("buildId", buildId);
            doneMsg.put("stepId", stepId);
            doneMsg.put("exitCode", exitCode);
            webSocketClient.send(mapper.writeValueAsString(doneMsg));

        } catch (Exception e) {
            log.error("Fatal error executing build step ID: {}", stepId, e);
            try {
                Map<String, Object> errMsg = new HashMap<>();
                errMsg.put("type", "ERROR");
                errMsg.put("buildId", buildId);
                errMsg.put("stepId", stepId);
                errMsg.put("content", e.getMessage());
                webSocketClient.send(mapper.writeValueAsString(errMsg));
            } catch (Exception ex) {
                log.error("Failed to notify master of execution error", ex);
            }
        }
    }

    private static void cloneOrPullRepository(String repoUrl, String token, String branch, String commitSha, Path targetDir) throws Exception {
        UsernamePasswordCredentialsProvider creds = null;
        if (token != null && !token.isBlank()) {
            creds = new UsernamePasswordCredentialsProvider(token, "");
        }

        if (Files.exists(targetDir)) {
            try {
                try (Git git = Git.open(targetDir.toFile())) {
                    log.info("Fetching updates for repository locally: {}", repoUrl);
                    var fetch = git.fetch();
                    if (creds != null) fetch.setCredentialsProvider(creds);
                    fetch.call();
                    
                    String ref = (commitSha != null && !commitSha.isBlank()) ? commitSha : branch;
                    git.checkout().setName(ref).call();
                }
                return;
            } catch (Exception e) {
                log.warn("Failed to open local git repo, deleting and re-cloning", e);
                deleteDirectory(targetDir);
            }
        }

        Files.createDirectories(targetDir);
        log.info("Cloning repository into local runner: {}", repoUrl);
        var clone = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir.toFile())
                .setCloneAllBranches(true);
        if (creds != null) clone.setCredentialsProvider(creds);

        try (Git git = clone.call()) {
            String ref = (commitSha != null && !commitSha.isBlank()) ? commitSha : branch;
            git.checkout().setName(ref).call();
            log.info("Successfully checked out ref: {}", ref);
        }
    }

    private static void collectAndUploadArtifacts(Path workspacePath, Long buildId, String stepName, List<String> patterns) {
        log.info("Collecting artifacts with patterns: {}", patterns);
        for (String pattern : patterns) {
            try {
                List<Path> matchedFiles = findMatchingFiles(workspacePath, pattern);
                for (Path file : matchedFiles) {
                    uploadFileToMaster(buildId, stepName, file);
                }
            } catch (Exception e) {
                log.error("Failed to collect/upload artifacts for pattern: {}", pattern, e);
            }
        }
    }

    private static List<Path> findMatchingFiles(Path baseDir, String pattern) throws IOException {
        List<Path> matches = new ArrayList<>();
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(
                "glob:" + baseDir.toAbsolutePath().toString().replace("\\", "/") + "/" + pattern.replace("\\", "/")
        );
        try (Stream<Path> stream = Files.walk(baseDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> matcher.matches(Paths.get(path.toAbsolutePath().toString().replace("\\", "/"))))
                    .forEach(matches::add);
        }
        return matches;
    }

    private static void uploadFileToMaster(Long buildId, String stepName, Path file) throws Exception {
        log.info("Uploading artifact {} to Master...", file.getFileName());
        String url = masterUrl + "/api/v1/builds/" + buildId + "/artifacts/upload?filename=" 
                + file.getFileName().toString() + "&stepName=" + stepName;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/octet-stream")
                .header("X-Runner-Token", token)
                .POST(HttpRequest.BodyPublishers.ofFile(file))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("Upload failed with status code " + response.statusCode() + ": " + response.body());
        }
        log.info("Successfully uploaded artifact {}!", file.getFileName());
    }

    private static void deleteDirectory(Path path) {
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.error("Failed to delete directory {}", path, e);
        }
    }

    private static List<String> findArtifactsFromYaml(Path workspacePath, String stepName) {
        Path yamlFile = workspacePath.resolve(".rabotyaga.yaml");
        if (!Files.exists(yamlFile)) {
            yamlFile = workspacePath.resolve(".action.yaml");
        }
        if (!Files.exists(yamlFile)) {
            return Collections.emptyList();
        }
        try {
            Map<String, Object> yaml = mapper.readValue(yamlFile.toFile(), new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> steps = (List<Map<String, Object>>) yaml.get("steps");
            if (steps != null) {
                for (Map<String, Object> step : steps) {
                    String name = (String) step.get("name");
                    if (stepName.equalsIgnoreCase(name)) {
                        return (List<String>) step.get("artifacts");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to auto-parse artifacts from YAML: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
