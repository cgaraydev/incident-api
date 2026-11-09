package com.cgaraydev.incidentapi.incident;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.cgaraydev.incidentapi.PostgresTestConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class IncidentConcurrencyTest {

    @Autowired
    private IncidentService service;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void waitsForTheLockAndUsesTheCommittedStatus() throws Exception {
        Incident incident = service.create(
                "Prueba de concurrencia",
                "Dos transacciones cambian la misma incidencia.");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Integer> waitingPid = new CompletableFuture<>();
        AtomicReference<Future<Incident>> pendingChange = new AtomicReference<>();

        try {
            TransactionTemplate firstTransaction =
                    new TransactionTemplate(transactionManager);

            firstTransaction.executeWithoutResult(transaction -> {
                service.changeStatus(
                        incident.getId(),
                        IncidentStatus.IN_PROGRESS);

                int blockingPid = jdbc.sql("SELECT pg_backend_pid()")
                        .query(Integer.class)
                        .single();

                pendingChange.set(executor.submit(() -> {
                    TransactionTemplate secondTransaction =
                            new TransactionTemplate(transactionManager);

                    return secondTransaction.execute(second -> {
                        jdbc.sql("SET LOCAL lock_timeout = '10s'").update();

                        int pid = jdbc.sql("SELECT pg_backend_pid()")
                                .query(Integer.class)
                                .single();

                        waitingPid.complete(pid);

                        return service.changeStatus(
                                incident.getId(),
                                IncidentStatus.RESOLVED);
                    });
                }));

                try {
                    int secondPid = waitingPid.get(5, TimeUnit.SECONDS);

                    awaitDatabaseLock(secondPid, blockingPid);
                    assertFalse(pendingChange.get().isDone());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(
                            "Interrupted while checking the lock",
                            exception);
                } catch (Exception exception) {
                    throw new AssertionError(
                            "Could not verify the concurrent transaction",
                            exception);
                }
            });

            Incident resolved = pendingChange.get()
                    .get(10, TimeUnit.SECONDS);

            assertNotNull(resolved);
            assertEquals(IncidentStatus.RESOLVED, resolved.getStatus());

            Incident stored = service.findById(incident.getId())
                    .orElseThrow();

            assertEquals(IncidentStatus.RESOLVED, stored.getStatus());
            assertEquals(incident.getCreatedAt(), stored.getCreatedAt());
        } finally {
            executor.shutdown();

            boolean stopped = executor.awaitTermination(15, TimeUnit.SECONDS);

            if (!stopped) {
                executor.shutdownNow();
            }

            assertTrue(stopped, "The concurrent task must finish");

            jdbc.sql("DELETE FROM incidents WHERE id = :id")
                    .param("id", incident.getId())
                    .update();
        }
    }

    private void awaitDatabaseLock(int waitingPid, int blockingPid)
            throws InterruptedException {

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            boolean blocked = jdbc.sql("""
                    SELECT :blockingPid = ANY(pg_blocking_pids(:waitingPid))
                    """)
                    .param("blockingPid", blockingPid)
                    .param("waitingPid", waitingPid)
                    .query(Boolean.class)
                    .single();

            if (blocked) {
                return;
            }

            Thread.sleep(25);
        }

        fail("The second transaction did not wait for the first transaction");
    }
}