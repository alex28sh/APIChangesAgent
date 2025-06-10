package org.example

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class LoggerTest {

    @Test
    fun testLoggers() {
        // Clear log files first
        File("logs/agent.log").writeText("")
        File("logs/test_res.log").writeText("")

        val logger = KotlinLogging.logger("AgentLogger")
        val testLogger = KotlinLogging.logger("TestLogger")

        val regularMessage = "This should go to agent.log"
        val testMessage = "This should go to test_res.log"

        logger.info { regularMessage }
        testLogger.info { testMessage }

        // Give some time for logs to be written
        Thread.sleep(1000)

        // Read log files
        val agentLog = File("logs/agent.log").readText()
        val testLog = File("logs/test_res.log").readText()

        // Print log contents for debugging
        println("Agent log: $agentLog")
        println("Test log: $testLog")

        // Verify logs
        assertTrue(agentLog.contains(regularMessage), "agent.log should contain regular message")
        assertTrue(testLog.contains(testMessage), "test_res.log should contain test message")
        assertFalse(agentLog.contains(testMessage), "agent.log should NOT contain test message")
    }
}
