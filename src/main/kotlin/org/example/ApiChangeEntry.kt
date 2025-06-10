package org.example

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Represents an API change in a library
 */
@Serializable
data class ApiChange(
    @property:LLMDescription("The library where the API change occurs")
    val library: String,
    @property:LLMDescription("The name of the API change, e.g., method name")
    val name: String,
    @property:LLMDescription("The version of the library where the API change occurs")
    val from_version: String,
    @property:LLMDescription("The version of the library where the API change is introduced")
    val to_version: String,
    @property:LLMDescription("The type of the API change, e.g., deprecated, signature, added")
    val type: String,
    @property:LLMDescription("The signature of the API change, e.g., method signature")
    val signature: String,
    @property:LLMDescription("A description of the API change")
    val documentation: String?,
    @property:LLMDescription("The type of change, e.g., method, class, field")
    val changetype: String,
    @property:LLMDescription("The source code of the API change, if available")
    val source_code: String,
    @property:LLMDescription("The query or task description for the API change")
    val query: String,
    @property:LLMDescription("The function signature for the API change, if applicable")
    val function_signature: String,
    @property:LLMDescription("The test program to be used for testing the API change")
    val test_program: String
) : Tool.Args

/**
 * Represents the result of code generation and testing
 */
@Serializable
data class TestResult(
    val apiChange: ApiChange? = null,
    val generatedCode: String? = null,
    val gradleBuild: String? = null,
    val model: String? = null,
    val success: Boolean,
    val output: String? = null,
    val errorOutput: String? = null,
    val testResults: List<TestExecutionResult> = emptyList()
) : Tool.Args, ToolResult {
    override fun toStringDefault(): String =
        Json.encodeToString(serializer(), this)
}

@Serializable
data class TestExecutionResult(
    val testName: String,
    val className: String,
    val success: Boolean,
    val duration: Long,
    val failure: String? = null
)
