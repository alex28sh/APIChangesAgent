package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.feature.model.ToolCallEvent
import ai.koog.agents.core.feature.model.ToolCallFailureEvent
import ai.koog.agents.core.feature.model.ToolCallResultEvent
import ai.koog.agents.core.feature.model.ToolValidationErrorEvent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.features.common.writer.FeatureMessageLogWriter
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.tracing.feature.Tracing
import ai.koog.agents.features.tracing.writer.TraceFeatureMessageLogWriter
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.example.tools.ProjectGenerationToolSet
import org.example.tools.RepairingToolSet
import org.example.tools.TestRunnerToolSet
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AgentWrapper(
    val model: Model,
    val apiChange: ApiChange,
    val openaiApiKey: String,
    val antropicApiKey: String,
    val maxAgentIterations: Int,
) {

    var lastTestResult : TestResult? = null
    val runner: AIAgent

    init {
        val agentConfig = AIAgentConfig(
            prompt = prompt("api-changes-prompt") {
                system(
                    """
                You are an expert Java developer specializing in the Spring framework. For each API change (from the old version to the new version) of the Java Spring library, your task is to:

                - Generate the necessary Java code to demonstrate the API change.
                - Write the corresponding Gradle build configuration required to compile and test the code.
                - Use appropriate tools for each step: 
                    - code generation
                    - Gradle configuration
                    - running tests
                - Ensure that the generated code is correct, idiomatic, and follows best practices for the specified Spring version.
                    - test running tool for that
                - Provide clear separation between code, build configuration, and test execution instructions.
                - You are not eligible to modify the API change details, but you can generate code and build files based on the provided API change.

                The API change details will be provided in the form of an `ApiChange` object, which includes:
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
                )
                The ApiChange object will be provided as input to your agent.
                """.trimIndent()
                )
            },
            model = model.model,
            maxAgentIterations = maxAgentIterations
        )

        val llmClients : MutableList<Pair<LLMProvider, LLMClient>> = mutableListOf()
        if (openaiApiKey.isNotEmpty())
            llmClients += LLMProvider.OpenAI to OpenAILLMClient(openaiApiKey)
        if (antropicApiKey.isNotEmpty())
            llmClients += LLMProvider.Anthropic to OpenAILLMClient(antropicApiKey)
        val promptExecutor = MultiLLMPromptExecutor(llmClients.toMap())

        val projectGenerationTools = ProjectGenerationToolSet(promptExecutor, model.model)
        val repairingTools = RepairingToolSet(promptExecutor, model.model)
        val testRunnerToolSet = TestRunnerToolSet()

        val agentStrategy = strategy("api-changes-strategy") {
            val nodeCreate by subgraph(
                tools = projectGenerationTools.asTools(),
                name = "node-create-api-change"
            ) {
                val nodeCallLLM by nodeLLMRequestMultiple()
                val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
                val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
                edge(nodeStart forwardTo nodeCallLLM)

                edge(
                    (nodeCallLLM forwardTo nodeFinish)
                            transformed { it.first() }
                            onAssistantMessage { true }
                )

                edge(
                    (nodeCallLLM forwardTo nodeExecuteToolMultiple)
                            onMultipleToolCalls { true }
                )

                edge(
                    (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                )

                edge(
                    (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                            onMultipleToolCalls { true }
                )

                edge(
                    (nodeSendToolResultMultiple forwardTo nodeFinish)
                            transformed { it.first() }
                            onAssistantMessage { true }
                )
            }

            val repairNode by subgraph(
                tools = repairingTools.asTools() + testRunnerToolSet.asTools(),
                name = "node-repair-api-change"
            ) {
                val nodeCallLLM by nodeLLMRequestMultiple()
                val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
                val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
                edge(nodeStart forwardTo nodeCallLLM)

                edge(
                    (nodeCallLLM forwardTo nodeFinish)
                            transformed { it.first() }
                            onAssistantMessage { msg -> lastTestResult?.success ?: false && lastTestResult?.apiChange == apiChange}
                )

                edge (
                    (nodeCallLLM forwardTo nodeCallLLM)
                            transformed { it.first().content + "\nPlease run tests, before submitting" }
                            onAssistantMessage { msg -> !(lastTestResult?.success ?: false) }
                )

                edge (
                    (nodeCallLLM forwardTo nodeCallLLM)
                            transformed { it.first().content + "\nYou modified the API change details, before running tests. Please, don't do that and run tests again with the original API details" }
                            onAssistantMessage { msg -> lastTestResult?.apiChange != apiChange }
                )

                edge(
                    (nodeCallLLM forwardTo nodeExecuteToolMultiple)
                            onMultipleToolCalls { true }
                )

                edge(
                    (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                )

                edge(
                    (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                            onMultipleToolCalls { true }
                )

                edge (
                    (nodeSendToolResultMultiple forwardTo nodeCallLLM)
                            transformed { it.first().content + "\nPlease run tests, before submitting" }
                            onCondition { !(lastTestResult?.success ?: false) }
                )

                edge (
                    (nodeSendToolResultMultiple forwardTo nodeCallLLM)
                            transformed { it.first().content + "\nYou modified the API change details, before running tests. Please, don't do that and run tests again with the original API details" }
                            onCondition { lastTestResult?.apiChange != apiChange }
                )

                edge(
                    (nodeSendToolResultMultiple forwardTo nodeFinish)
                            transformed { it.first() }
                            onAssistantMessage { msg -> lastTestResult?.success ?: false && lastTestResult?.apiChange == apiChange}
                )
            }

            edge(nodeStart forwardTo nodeCreate)
            edge(nodeCreate forwardTo repairNode)
            edge(repairNode forwardTo nodeFinish
                onCondition { testResult ->
                    lastTestResult?.success ?: false
                }
            )
            edge(repairNode forwardTo repairNode
                onCondition { testResult ->
                    ! (lastTestResult?.success ?: false)
                }
            )
        }

        val customToolRegistry = ToolRegistry {
            tools(
                projectGenerationTools.asTools() +
                repairingTools.asTools() +
                testRunnerToolSet.asTools()
            )
        }

        val logger = KotlinLogging.logger("AgentLogger")
        val writer = TraceFeatureMessageLogWriter(
            targetLogger = logger,
            logLevel = FeatureMessageLogWriter.LogLevel.INFO,
            format = { message ->
                "[TRACE] ${message.messageType.name}: ${message::class.simpleName}"
            }
        )

        val testLogger = KotlinLogging.logger("TestLogger")

        val assistantLogger = KotlinLogging.logger("AssistantLogger")

        runner = AIAgent(
            promptExecutor = promptExecutor,
            strategy = agentStrategy,
            agentConfig = agentConfig,
            toolRegistry = customToolRegistry,
        ) {
            handleEvents {
                onAfterLLMCall = { _, tool, model, response, _ ->
                    assistantLogger.info { "LLM ${model.id} response: $response" }
                }
                onBeforeNode = { node, context, input ->
                    logger.info { "Before node: ${node.name}, context: $context, input: $input" }
                }
                onToolCallResult = { tool, toolArgs, toolResult ->
                    if (tool.name == "run-tests") {
                        try {
                            if (lastTestResult == null || !lastTestResult!!.success) {
                                lastTestResult =
                                    Json.decodeFromString(TestResult.serializer(), toolResult?.toStringDefault() ?: "")
                            }
                        } catch (e: Exception) {
                            testLogger.info { "Error decoding TestResult: ${e.message}" }
                        }

                        val rs = toolResult?.toStringDefault() ?: "No result"
                        testLogger.info { "Tool runTests called with result: $rs" }
                    }
                    logger.info { "Tool called: ${tool.name}, args: $toolArgs, result: ${toolResult?.toStringDefault()}" }
                    logger.info { "success: ${lastTestResult?.success}}" }
                }
            }


            install(Tracing) {
                messageFilter = { message ->
                    message is ToolCallEvent ||
                    message is ToolCallResultEvent ||
                    message is ToolValidationErrorEvent ||
                    message is ToolCallFailureEvent
                }
                addMessageProcessor(writer)
            }
        }
    }

    fun run(): TestResult = runBlocking {
        var msg = "No error output"
        try {
            runner.run(Json.encodeToString(ApiChange.serializer(), apiChange))
        } catch (e : Throwable) {
            msg = e.message ?: "No error output"
            System.err.println(e.message)
        }
        TestResult(
            apiChange = apiChange,
            generatedCode = lastTestResult?.generatedCode,
            gradleBuild = lastTestResult?.gradleBuild,
            model = model.name,
            success = lastTestResult?.success ?: false,
            output = lastTestResult?.output,
            errorOutput = lastTestResult?.errorOutput ?: msg,
            testResults = lastTestResult?.testResults ?: emptyList()
        )
    }
}
