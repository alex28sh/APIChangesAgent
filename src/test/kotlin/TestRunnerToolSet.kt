package org.example

import org.example.tools.TestRunnerToolSet
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class TestRunnerToolSetTest {

    private lateinit var testRunnerToolSet: TestRunnerToolSet

    @BeforeEach
    fun setUp() {
        testRunnerToolSet = TestRunnerToolSet()
    }

    val standardGradle = """
        plugins {
            id 'java'
        }

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation 'org.springframework:spring-context:6.1.20'
            testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.1'
            testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.1'
        }

        test {
            useJUnitPlatform()
        }
    """.trimIndent()

    @Test
    fun testRunTestsForAdd() {
        // Create an ApiChange object for the Spring Framework ConcurrentMapCache.retrieve method
        val apiChange = ApiChange(
            library = "spring-framework",
            name = "org.springframework.cache.concurrent.ConcurrentMapCache#retrieve(key,valueLoader)",
            from_version = "v7.0.0-M4",
            to_version = "v6.1.20",
            type = "method",
            signature = "public CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader)",
            documentation = null,
            changetype = "deprecated",
            source_code = "\tpublic <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {\n\t\treturn CompletableFuture.supplyAsync(() ->\n\t\t\t\t(T) fromStoreValue(this.store.computeIfAbsent(key, k -> toStoreValue(valueLoader.get().join()))));\n\t}",
            query = "Design an asynchronous caching method that ensures non-blocking data retrieval and enhances thread safety, thereby improving overall application performance and developer experience.",
            function_signature = "public CompletableFuture<T> fetchAsync(Object key, Supplier<CompletableFuture<T>> valueProvider);",
            test_program = """
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;

                public class ExampleSpringServiceTest {

                    @Test
                    public void testAdd() {
                        ExampleSpringService exampleSpringService = new ExampleSpringService();
                        int result = exampleSpringService.add(2, 3);
                        assertEquals(5, result);
                    }
                }
            """.trimIndent()
        )

        val code = """
            import org.springframework.cache.concurrent.ConcurrentMapCache;
            import java.util.concurrent.CompletableFuture;
            import java.util.function.Supplier;

            public class ExampleSpringService {

                public int add(int a, int b) {
                    return a + b;
                }
            }
        """.trimIndent()

        val result = testRunnerToolSet.runTests(apiChange, code, standardGradle)

        assertTrue { result.success }
        assertEquals(code, result.generatedCode)
        assertEquals(standardGradle, result.gradleBuild)
    }


    @Test
    fun testRunTestsForAddWrong() {
        // Create an ApiChange object for the Spring Framework ConcurrentMapCache.retrieve method
        val apiChange = ApiChange(
            library = "spring-framework",
            name = "org.springframework.cache.concurrent.ConcurrentMapCache#retrieve(key,valueLoader)",
            from_version = "v7.0.0-M4",
            to_version = "v6.1.20",
            type = "method",
            signature = "public CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader)",
            documentation = null,
            changetype = "deprecated",
            source_code = "\tpublic <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {\n\t\treturn CompletableFuture.supplyAsync(() ->\n\t\t\t\t(T) fromStoreValue(this.store.computeIfAbsent(key, k -> toStoreValue(valueLoader.get().join()))));\n\t}",
            query = "Design an asynchronous caching method that ensures non-blocking data retrieval and enhances thread safety, thereby improving overall application performance and developer experience.",
            function_signature = "public CompletableFuture<T> fetchAsync(Object key, Supplier<CompletableFuture<T>> valueProvider);",
            test_program = """
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertEquals;

                public class ExampleSpringServiceTest {

                    @Test
                    public void testAdd() {
                        ExampleSpringService exampleSpringService = new ExampleSpringService();
                        int result = exampleSpringService.add(2, 3);
                        assertEquals(5, result);
                    }
                }
            """.trimIndent()
        )

        val code = """
            import org.springframework.cache.concurrent.ConcurrentMapCache;
            import java.util.concurrent.CompletableFuture;
            import java.util.function.Supplier;

            public class ExampleSpringService {

                public int add(int a, int b) {
                    return a - b;
                }
            }
        """.trimIndent()

        val result = testRunnerToolSet.runTests(apiChange, code, standardGradle)

        assertFalse { result.success }
        assertEquals(code, result.generatedCode)
        assertEquals(standardGradle, result.gradleBuild)
    }

    @Test
    fun testRunTestsForSpringConcurrentMapCache() {
        // Create an ApiChange object for the Spring Framework ConcurrentMapCache.retrieve method
        val apiChange = ApiChange(
            library = "spring-framework",
            name = "org.springframework.cache.concurrent.ConcurrentMapCache#retrieve(key,valueLoader)",
            from_version = "v7.0.0-M4",
            to_version = "v6.1.20",
            type = "method",
            signature = "public CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader)",
            documentation = null,
            changetype = "deprecated",
            source_code = "\tpublic <T> CompletableFuture<T> retrieve(Object key, Supplier<CompletableFuture<T>> valueLoader) {\n\t\treturn CompletableFuture.supplyAsync(() ->\n\t\t\t\t(T) fromStoreValue(this.store.computeIfAbsent(key, k -> toStoreValue(valueLoader.get().join()))));\n\t}",
            query = "Design an asynchronous caching method that ensures non-blocking data retrieval and enhances thread safety, thereby improving overall application performance and developer experience.",
            function_signature = "public CompletableFuture<T> fetchAsync(Object key, Supplier<CompletableFuture<T>> valueProvider);",
            test_program = """
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                import org.springframework.cache.concurrent.ConcurrentMapCache;

                import java.util.concurrent.CompletableFuture;
                import java.util.concurrent.ExecutionException;
                import java.util.concurrent.atomic.AtomicInteger;
                import java.util.function.Supplier;

                import static org.junit.jupiter.api.Assertions.*;

                public class ExampleSpringServiceTest {

                    private ExampleSpringService exampleSpringService;
                    private ConcurrentMapCache cache;

                    @BeforeEach
                    public void setUp() {
                        exampleSpringService = new ExampleSpringService();
                        cache = new ConcurrentMapCache("exampleCache");
                    }

                    @Test
                    public void testFetchAsync_CacheMiss() throws ExecutionException, InterruptedException {
                        Object key = "testKey";
                        CompletableFuture<String> futureValue = CompletableFuture.completedFuture("TestValue");
                        AtomicInteger invocationCount = new AtomicInteger(0);

                        Supplier<CompletableFuture<String>> valueProvider = () -> {
                            invocationCount.incrementAndGet();
                            return futureValue;
                        };

                        CompletableFuture<String> resultFuture = exampleSpringService.fetchAsync(key, valueProvider);
                        String result = resultFuture.get();

                        assertEquals("TestValue", result);
                        assertEquals(1, invocationCount.get());

                        // Verify that the value is cached
                        CompletableFuture<String> cachedFuture = exampleSpringService.fetchAsync(key, valueProvider);
                        String cachedResult = cachedFuture.get();
                        assertEquals("TestValue", cachedResult);
                        // valueProvider should not be called again
                        assertEquals(1, invocationCount.get());
                    }

                    @Test
                    public void testFetchAsync_CacheHit() throws ExecutionException, InterruptedException {
                        Object key = "testKey";
                        CompletableFuture<String> futureValue = CompletableFuture.completedFuture("CachedValue");
                        AtomicInteger invocationCount = new AtomicInteger(0);

                        Supplier<CompletableFuture<String>> valueProvider = () -> {
                            invocationCount.incrementAndGet();
                            return futureValue;
                        };

                        // First call to populate cache
                        CompletableFuture<String> firstCall = exampleSpringService.fetchAsync(key, valueProvider);
                        String firstResult = firstCall.get();
                        assertEquals("CachedValue", firstResult);
                        assertEquals(1, invocationCount.get());

                        // Second call should retrieve from cache
                        CompletableFuture<String> secondCall = exampleSpringService.fetchAsync(key, valueProvider);
                        String secondResult = secondCall.get();
                        assertEquals("CachedValue", secondResult);
                        // valueProvider should not be called again
                        assertEquals(1, invocationCount.get());
                    }

                    @Test
                    public void testFetchAsync_ConcurrentAccess() throws InterruptedException, ExecutionException {
                        Object key = "concurrentKey";
                        CompletableFuture<String> futureValue = new CompletableFuture<>();
                        AtomicInteger invocationCount = new AtomicInteger(0);

                        Supplier<CompletableFuture<String>> valueProvider = () -> {
                            invocationCount.incrementAndGet();
                            return futureValue;
                        };

                        // Start multiple threads to call fetchAsync concurrently
                        int threadCount = 10;
                        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];
                        for (int i = 0; i < threadCount; i++) {
                            futures[i] = exampleSpringService.fetchAsync(key, valueProvider);
                        }

                        // Complete the futureValue
                        futureValue.complete("ConcurrentValue");

                        // Verify all futures complete with the same value
                        for (int i = 0; i < threadCount; i++) {
                            assertEquals("ConcurrentValue", futures[i].get());
                        }

                        // valueProvider should be called only once
                        assertEquals(1, invocationCount.get());
                    }

                    @Test
                    public void testFetchAsync_ValueProviderException() {
                        Object key = "exceptionKey";
                        CompletableFuture<String> failedFuture = new CompletableFuture<>();
                        failedFuture.completeExceptionally(new RuntimeException("ValueProvider failed"));
                        AtomicInteger invocationCount = new AtomicInteger(0);

                        Supplier<CompletableFuture<String>> valueProvider = () -> {
                            invocationCount.incrementAndGet();
                            return failedFuture;
                        };

                        CompletableFuture<String> resultFuture = exampleSpringService.fetchAsync(key, valueProvider);

                        ExecutionException exception = assertThrows(ExecutionException.class, resultFuture::get);
                        assertTrue(exception.getCause() instanceof RuntimeException);
                        assertEquals("ValueProvider failed", exception.getCause().getMessage());

                        assertEquals(1, invocationCount.get());
                    }
                }
            """.trimIndent()
        )

        val code = """
            import org.springframework.cache.concurrent.ConcurrentMapCache;
            import java.util.concurrent.CompletableFuture;
            import java.util.function.Supplier;

            public class ExampleSpringService {

                private final ConcurrentMapCache cache;

                public ExampleSpringService() {
                    this.cache = new ConcurrentMapCache("exampleCache");
                }

                public <T> CompletableFuture<T> fetchAsync(Object key, Supplier<CompletableFuture<T>> valueProvider) {
                    return cache.retrieve(key, valueProvider);
                }
            }
        """.trimIndent()

        val result = testRunnerToolSet.runTests(apiChange, code, standardGradle)

        assertTrue { result.success }
        assertEquals(code, result.generatedCode)
        assertEquals(standardGradle, result.gradleBuild)
    }
}
