package dev.mathalama.zovik.runner.service.impl;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import dev.mathalama.zovik.runner.domain.Runner;
import dev.mathalama.zovik.runner.domain.RunnerStatus;
import dev.mathalama.zovik.runner.repository.RunnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerProvisionerService {

    private final RunnerRepository runnerRepository;

    @Value("${zovik.frontend.url:http://localhost:3001}")
    private String masterUrl;

    @Async
    @Transactional
    public void provisionRunnerAsync(Long runnerId) {
        log.info("Starting remote runner provisioning for runner ID: {}", runnerId);
        
        Runner runner = runnerRepository.findById(runnerId).orElse(null);
        if (runner == null) {
            log.error("Runner not found for provisioning: {}", runnerId);
            return;
        }

        StringBuilder installLog = new StringBuilder();
        appendToLog(installLog, runner, "--- Starting Runner Provisioning ---\n");

        Session session = null;
        try {
            JSch jsch = new JSch();
            
            // Setup identity if private key is supplied
            if (runner.getSshKey() != null && !runner.getSshKey().isBlank()) {
                appendToLog(installLog, runner, "Setting up SSH Key authentication...\n");
                jsch.addIdentity("agent-key", runner.getSshKey().getBytes(), null, null);
            }

            session = jsch.getSession(runner.getUsername(), runner.getHost(), runner.getPort());
            
            if (runner.getPassword() != null && !runner.getPassword().isBlank()) {
                appendToLog(installLog, runner, "Using password authentication...\n");
                session.setPassword(runner.getPassword());
            }

            session.setConfig("StrictHostKeyChecking", "no");
            
            appendToLog(installLog, runner, "Connecting SSH session to " + runner.getHost() + ":" + runner.getPort() + "...\n");
            session.connect(15000);
            appendToLog(installLog, runner, "SSH Connection established successfully.\n");

            // 1. Check and Install Docker
            appendToLog(installLog, runner, "Checking if Docker is installed...\n");
            int hasDocker = executeRemoteCommand(session, installLog, runner, "docker --version");
            if (hasDocker != 0) {
                appendToLog(installLog, runner, "Docker is missing. Starting Docker installation...\n");
                executeRemoteCommand(session, installLog, runner, "sudo apt-get update -y && sudo apt-get install -y docker.io");
                executeRemoteCommand(session, installLog, runner, "sudo systemctl start docker && sudo systemctl enable docker");
            } else {
                appendToLog(installLog, runner, "Docker is already installed.\n");
            }

            // 2. Check and Install Java 21
            appendToLog(installLog, runner, "Checking if Java 21 is installed...\n");
            int hasJava = executeRemoteCommand(session, installLog, runner, "java -version");
            if (hasJava != 0) {
                appendToLog(installLog, runner, "Java is missing. Starting OpenJDK 21 installation...\n");
                executeRemoteCommand(session, installLog, runner, "sudo apt-get update -y && sudo apt-get install -y openjdk-21-jre-headless");
            } else {
                appendToLog(installLog, runner, "Java is already installed.\n");
            }

            // 3. Create folder
            appendToLog(installLog, runner, "Creating workspace directory at /opt/rabotyaga-agent...\n");
            executeRemoteCommand(session, installLog, runner, "sudo mkdir -p /opt/rabotyaga-agent && sudo chmod 777 /opt/rabotyaga-agent");

            // 4. Copy agent.jar via SFTP
            appendToLog(installLog, runner, "Copying agent.jar via SFTP...\n");
            ClassPathResource agentJarResource = new ClassPathResource("static/agent.jar");
            if (!agentJarResource.exists()) {
                throw new Exception("Embedded agent.jar resource not found. Run ./gradlew.bat buildAgent first.");
            }

            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(10000);
            try (InputStream jarStream = agentJarResource.getInputStream()) {
                sftp.put(jarStream, "/opt/rabotyaga-agent/agent.jar");
            }
            sftp.disconnect();
            appendToLog(installLog, runner, "agent.jar copied successfully.\n");

            // 5. Setup Systemd Service
            appendToLog(installLog, runner, "Creating systemd service configuration...\n");
            String serviceTemplate = """
            [Unit]
            Description=Zovik Runner Agent
            After=network.target docker.service
            Requires=docker.service
            
            [Service]
            Type=simple
            User=root
            WorkingDirectory=/opt/rabotyaga-agent
            ExecStart=/usr/bin/java -jar agent.jar --master=%s --token=%s
            Restart=always
            RestartSec=10
            
            [Install]
            WantedBy=multi-user.target
            """.formatted(masterUrl, runner.getToken());

            // Escape quotes for bash cat writing
            String escapedService = serviceTemplate.replace("$", "\\$").replace("`", "\\`");
            String writeServiceCmd = "sudo cat << 'EOF' > /tmp/rabotyaga-agent.service\n" + escapedService + "\nEOF";
            executeRemoteCommand(session, installLog, runner, writeServiceCmd);
            executeRemoteCommand(session, installLog, runner, "sudo mv /tmp/rabotyaga-agent.service /etc/systemd/system/rabotyaga-agent.service");

            // 6. Start the Service
            appendToLog(installLog, runner, "Reloading systemd daemon and starting service...\n");
            executeRemoteCommand(session, installLog, runner, "sudo systemctl daemon-reload");
            executeRemoteCommand(session, installLog, runner, "sudo systemctl enable rabotyaga-agent");
            executeRemoteCommand(session, installLog, runner, "sudo systemctl restart rabotyaga-agent");

            appendToLog(installLog, runner, "\n--- Provisioning Completed Successfully! ---\n");
            runner.setStatus(RunnerStatus.OFFLINE); // Set to OFFLINE, it will flip to ONLINE when WebSocket connects
            runnerRepository.save(runner);

        } catch (Exception e) {
            log.error("Failed to provision runner agent ID: {}", runnerId, e);
            appendToLog(installLog, runner, "\nFATAL PROVISIONING ERROR: " + e.getMessage() + "\n");
            runner.setStatus(RunnerStatus.FAILED);
            runnerRepository.save(runner);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private int executeRemoteCommand(Session session, StringBuilder installLog, Runner runner, String command) throws Exception {
        appendToLog(installLog, runner, "$ " + command + "\n");
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        
        InputStream in = channel.getInputStream();
        InputStream err = channel.getErrStream();
        
        channel.connect(20000);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
        
        String line;
        while ((line = reader.readLine()) != null) {
            appendToLog(installLog, runner, line + "\n");
        }
        while ((line = errReader.readLine()) != null) {
            appendToLog(installLog, runner, "ERROR: " + line + "\n");
        }

        while (!channel.isClosed()) {
            Thread.sleep(100);
        }
        
        int exitStatus = channel.getExitStatus();
        channel.disconnect();
        return exitStatus;
    }

    private void appendToLog(StringBuilder installLog, Runner runner, String message) {
        installLog.append(message);
        runner.setInstallLog(installLog.toString());
        runnerRepository.save(runner);
    }
}
