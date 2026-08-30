package dev.mathalama.zovik.pipeline.service.impl;

import dev.mathalama.zovik.common.exception.BusinessException;
import dev.mathalama.zovik.common.exception.ResourceNotFoundException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

import dev.mathalama.zovik.project.repository.ProjectRepository;
import dev.mathalama.zovik.project.service.impl.GitHubIntegrationService;
import dev.mathalama.zovik.project.service.impl.SecretCryptoService;
import org.mockito.Mockito;

class GitCloneServiceTest {

    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final GitHubIntegrationService gitHubIntegrationService = Mockito.mock(GitHubIntegrationService.class);
    private final SecretCryptoService secretCryptoService = Mockito.mock(SecretCryptoService.class);

    private final GitCloneService gitCloneService = new GitCloneService(
            projectRepository,
            gitHubIntegrationService,
            secretCryptoService
    );

    @Test
    void testGitOperationsOffline(@TempDir Path originDir, @TempDir Path cloneDir) throws Exception {
        // 1. Initialize a local git repository in originDir
        RevCommit initialCommit;
        try (Git git = Git.init().setDirectory(originDir.toFile()).call()) {
            Files.writeString(originDir.resolve("test.txt"), "hello world");
            git.add().addFilepattern("test.txt").call();
            initialCommit = git.commit().setMessage("Initial commit").call();
        }

        String repoUrl = originDir.toUri().toString();

        // 2. Clone it using GitCloneService
        gitCloneService.cloneOrPull(repoUrl, cloneDir);

        // Verify clone succeeded and file exists
        assertTrue(Files.exists(cloneDir.resolve("test.txt")));
        String content = gitCloneService.readFile(cloneDir, "test.txt");
        assertEquals("hello world", content);

        // 3. Create a second commit in the origin repo
        RevCommit secondCommit;
        try (Git git = Git.open(originDir.toFile())) {
            Files.writeString(originDir.resolve("test.txt"), "hello updated");
            Files.writeString(originDir.resolve("another.txt"), "new file");
            git.add().addFilepattern(".").call();
            secondCommit = git.commit().setMessage("Second commit").call();
        }

        // 4. Fetch updates using cloneOrPull
        gitCloneService.cloneOrPull(repoUrl, cloneDir);

        // 5. Checkout the second commit
        gitCloneService.checkoutCommit(cloneDir, secondCommit.getName());
        assertEquals("hello updated", gitCloneService.readFile(cloneDir, "test.txt"));
        assertEquals("new file", gitCloneService.readFile(cloneDir, "another.txt"));

        // 6. Checkout the first commit again
        gitCloneService.checkoutCommit(cloneDir, initialCommit.getName());
        assertEquals("hello world", gitCloneService.readFile(cloneDir, "test.txt"));
        assertThrows(ResourceNotFoundException.class, () -> 
                gitCloneService.readFile(cloneDir, "another.txt"));
    }

    @Test
    void testCloneOrPull_InvalidRepo_ThrowsBusinessException(@TempDir Path cloneDir) {
        String invalidUrl = "http://invalid-url.local/repo.git";
        BusinessException exception = assertThrows(BusinessException.class, () ->
                gitCloneService.cloneOrPull(invalidUrl, cloneDir));
        assertTrue(exception.getMessage().contains("Failed to clone repository"));
    }

    @Test
    void testCheckoutCommit_InvalidCommit_ThrowsBusinessException(@TempDir Path originDir, @TempDir Path cloneDir) throws Exception {
        try (Git git = Git.init().setDirectory(originDir.toFile()).call()) {
            Files.writeString(originDir.resolve("test.txt"), "hello");
            git.add().addFilepattern("test.txt").call();
            git.commit().setMessage("Initial commit").call();
        }

        gitCloneService.cloneOrPull(originDir.toUri().toString(), cloneDir);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                gitCloneService.checkoutCommit(cloneDir, "invalid-commit-hash"));
        assertTrue(exception.getMessage().contains("Failed to checkout commit"));
    }

    @Test
    void testReadFile_IsDirectory_ThrowsBusinessException(@TempDir Path originDir, @TempDir Path cloneDir) throws Exception {
        try (Git git = Git.init().setDirectory(originDir.toFile()).call()) {
            Files.createDirectories(originDir.resolve("somedir"));
            Files.writeString(originDir.resolve("somedir/test.txt"), "hello");
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit").call();
        }

        gitCloneService.cloneOrPull(originDir.toUri().toString(), cloneDir);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                gitCloneService.readFile(cloneDir, "somedir"));
        assertTrue(exception.getMessage().contains("Failed to read file from repository"));
    }

    @Test
    void testCloneOrPull_ExistingDirectoryNotGit_DeletesAndReclones(@TempDir Path originDir, @TempDir Path cloneDir) throws Exception {
        try (Git git = Git.init().setDirectory(originDir.toFile()).call()) {
            Files.writeString(originDir.resolve("test.txt"), "hello");
            git.add().addFilepattern("test.txt").call();
            git.commit().setMessage("Initial commit").call();
        }

        Files.createDirectories(cloneDir);
        Files.writeString(cloneDir.resolve("garbage.txt"), "garbage");

        gitCloneService.cloneOrPull(originDir.toUri().toString(), cloneDir);

        assertTrue(Files.exists(cloneDir.resolve("test.txt")));
        assertFalse(Files.exists(cloneDir.resolve("garbage.txt")));
    }
}
