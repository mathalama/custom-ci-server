package dev.mathalama.rabotyagaci.pipeline.service.impl;

import dev.mathalama.rabotyagaci.common.exception.ResourceNotFoundException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GitCloneServiceTest {

    private final GitCloneService gitCloneService = new GitCloneService();

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
}
