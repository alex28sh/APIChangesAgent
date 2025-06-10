package org.example.tools;

import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.annotations.LLMDescription;
import ai.koog.agents.core.tools.annotations.Tool
import org.example.ApiChange
import org.example.TestExecutionResult
import org.example.TestResult
//import org.gradle.tooling.*
//import org.gradle.tooling.events.*
//import org.gradle.tooling.events.task.*
//import org.gradle.tooling.events.test.*
//import org.gradle.tooling.events.test.JvmTestOperationDescriptor
//import org.gradle.tooling.events.test.TestSuccessResult
//import org.gradle.tooling.events.test.TestFailureResult
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import java.util.EnumSet
import java.util.concurrent.CompletableFuture
import kotlin.io.path.isRegularFile

@LLMDescription(
    """
        This toolset provides a tool for running tests (obtained from the dataset) for the generated code and gradle files.
        It's necessary to ensure that the generated code works correctly with the provided API change.
        You can deliver the result only if the tests pass successfully.
        """)
class TestRunnerToolSet : ToolSet {


    /* Tried to use Gradle Tooling API to extract better test result info
    */
//
//    private fun runWithGradleTooling(apiChange : ApiChange, projectDir : Path, sourceCode : String) : String {
//        val connector = GradleConnector.newConnector()
//            .forProjectDirectory(projectDir.toFile())
//
//        val future = CompletableFuture<TestResult>()
//
//        connector.connect().use { connection ->
//            val outputCollector = StringBuilder()
//            val errorCollector = StringBuilder()
//            val testResults = mutableListOf<TestExecutionResult>()
//
//            val buildLauncher = connection.newBuild()
//                .forTasks("test")
//                .setStandardOutput(object : java.io.OutputStream() {
//                    override fun write(b: Int) {
//                        outputCollector.append(b.toChar())
//                    }
//                })
//                .setStandardError(object : java.io.OutputStream() {
//                    override fun write(b: Int) {
//                        errorCollector.append(b.toChar())
//                    }
//                })
//                .addProgressListener({ event ->
//                    when (event) {
//                        is TestProgressEvent -> {
//                            handleTestEvent(event, testResults)
//                        }
//                    }
//                }, EnumSet.of(OperationType.TEST))
//
//            try {
//                buildLauncher.run()
//                val success = testResults.all { it.success }
//                val result = TestResult(
//                    apiChange = apiChange,
//                    generatedCode = sourceCode,
//                    model = "model",
//                    success = success,
//                    output = outputCollector.toString(),
//                    errorOutput = errorCollector.toString(),
//                    testResults = testResults
//                )
//                future.complete(result)
//            } catch (e: Exception) {
//                val result = TestResult(
//                    apiChange = apiChange,
//                    generatedCode = sourceCode,
//                    model = "model",
//                    success = false,
//                    output = outputCollector.toString(),
//                    errorOutput = e.message ?: "Unknown error",
//                    testResults = testResults
//                )
//                future.complete(result)
//            }
//        }
//
//        return try {
//            val result = future.get()
//            if (result.success) {
//                "Tests passed successfully"
//            } else {
//                "Tests failed:\n${result.output}\n${result.errorOutput}\nTest Results:\n${result.testResults.joinToString("\n") { "${it.className}.${it.testName}: ${if (it.success) "PASSED" else "FAILED"} (${it.duration} ms) ${it.failure ?: "\nFailure: ${it.failure}"}" }}"
//            }
//        } catch (e: Exception) {
//            "Error running tests: ${e.message}"
//        }
//    }
//
//    private fun handleTestEvent(event: TestProgressEvent, testResults: MutableList<TestExecutionResult>) {
//        val descriptor = event.descriptor
//        if (descriptor is JvmTestOperationDescriptor) {
//            if (event is TestFinishEvent) {
//                val result = TestExecutionResult(
//                    testName = descriptor.name,
//                    className = descriptor.className,
//                    success = event.result is TestSuccessResult,
//                    duration = event.result.endTime - event.result.startTime,
//                    failure = if (event.result is TestFailureResult) {
//                        (event.result as TestFailureResult).failures.joinToString("\n") { it.message ?: "Unknown error" }
//                    } else null
//                )
//                testResults.add(result)
//            }
//        }
//    }

    private fun runWithCLI(
        projectDir : Path,
    ) : TestResult {
        val gradleHome = System.getenv("GRADLE_HOME") ?: throw IllegalStateException("GRADLE_HOME environment variable is not set")
        val command = listOf(gradleHome, "test")
        val processBuilder = ProcessBuilder(command)
            .directory(projectDir.toFile())

        return try {
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                TestResult(
                    success = true
                )
            } else {
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                TestResult(
                    success = false,
                    output = output,
                    errorOutput = error
                )
            }
        } catch (e: Exception) {
            println(e)
            TestResult(
                success = false,
                errorOutput = e.message ?: "Unknown error"
            )
        }
    }

    @Tool(customName = "run-tests")
    @LLMDescription("Run tests for the generated code and gradle files.")
    fun runTests(
        @LLMDescription("The API change details including the function signature and query.")
        apiChange: ApiChange,
        @LLMDescription("The generated code.")
        sourceCode: String,
        @LLMDescription("The generated Gradle build configuration.")
        gradleBuild: String,
    ): TestResult {
        val projectDir = Files.createTempDirectory("api-change-temp-dir")

        Files.createDirectories(projectDir.resolve("src/main/java"))
        Files.createDirectories(projectDir.resolve("src/test/java"))

        val sourcePath = projectDir.resolve("src/main/java/ExampleSpringService.java")
        val testPath = projectDir.resolve("src/test/java/ExampleSpringServiceTest.java")
        val buildFilePath = projectDir.resolve("build.gradle")

        sourcePath.writeText(sourceCode)
        testPath.writeText(apiChange.test_program)
        buildFilePath.writeText(gradleBuild)

        val result = runWithCLI(projectDir)

        projectDir.toFile().deleteRecursively()

        return result.copy(apiChange = apiChange, generatedCode = sourceCode, gradleBuild = gradleBuild)
    }
}
