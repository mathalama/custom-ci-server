package dev.mathalama.zovik.runner.service.impl;

import dev.mathalama.zovik.runner.api.dto.RunnerJobPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
public class RunnerJobQueueService {

    private final Map<Long, BlockingQueue<RunnerJobPayload>> runnerQueues = new ConcurrentHashMap<>();
    private final Map<Long, DeferredResult<ResponseEntity<RunnerJobPayload>>> activePollers = new ConcurrentHashMap<>();

    /**
     * Enqueues a job for a specific runner.
     * If the runner currently has an active Long-Polling connection, the job is dispatched immediately.
     */
    public void enqueueJob(Long runnerId, RunnerJobPayload job) {
        log.info("Enqueuing job for runner ID: {}, step ID: {}", runnerId, job.stepId());

        DeferredResult<ResponseEntity<RunnerJobPayload>> activePoller = activePollers.remove(runnerId);
        if (activePoller != null && !activePoller.isSetOrExpired()) {
            log.info("Dispatching job immediately to active long-poller for runner ID: {}", runnerId);
            activePoller.setResult(ResponseEntity.ok(job));
            return;
        }

        runnerQueues.computeIfAbsent(runnerId, k -> new LinkedBlockingQueue<>()).offer(job);
        log.debug("Job queued for runner ID: {}. Queue size: {}", runnerId, runnerQueues.get(runnerId).size());
    }

    /**
     * Handles Long-Polling request from a runner.
     * Returns immediately if a job is in queue, or holds connection for up to 15 seconds.
     */
    public DeferredResult<ResponseEntity<RunnerJobPayload>> pollNextJob(Long runnerId) {
        BlockingQueue<RunnerJobPayload> queue = runnerQueues.get(runnerId);
        if (queue != null && !queue.isEmpty()) {
            RunnerJobPayload job = queue.poll();
            if (job != null) {
                log.info("Returning queued job for runner ID: {}, step ID: {}", runnerId, job.stepId());
                DeferredResult<ResponseEntity<RunnerJobPayload>> immediate = new DeferredResult<>();
                immediate.setResult(ResponseEntity.ok(job));
                return immediate;
            }
        }

        // Long-poll for 15 seconds (returns 204 No Content on timeout)
        DeferredResult<ResponseEntity<RunnerJobPayload>> deferredResult = new DeferredResult<>(15000L, ResponseEntity.noContent().build());
        activePollers.put(runnerId, deferredResult);

        deferredResult.onCompletion(() -> activePollers.remove(runnerId, deferredResult));
        deferredResult.onTimeout(() -> activePollers.remove(runnerId, deferredResult));
        deferredResult.onError((ex) -> activePollers.remove(runnerId, deferredResult));

        return deferredResult;
    }
}
