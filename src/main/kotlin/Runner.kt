package org.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.io.path.readText
import me.tongfei.progressbar.ProgressBar

class Runner(val config: Config) {

    private fun parseInputFile(): List<ApiChange> {
        val jsonContent = config.inputFile.readText()
        val json = Json {
            ignoreUnknownKeys = true  // 👈 This tells the parser to skip unknown fields
        }
        return json.decodeFromString(jsonContent)
    }

    private fun executeEntry(apiChange: ApiChange, model: Model): TestResult {

        val agentWrapper = AgentWrapper(
            apiChange = apiChange,
            model = model,
            openaiApiKey = config.openaiApiKey,
            antropicApiKey = config.antropicApiKey,
            maxAgentIterations = config.maxAgentIterations
        )

        return agentWrapper.run()
    }

    suspend fun processChanges(apiChanges: List<ApiChange>): List<TestResult> = coroutineScope {
        val progressBar = ProgressBar("Processing", apiChanges.size.toLong())
        val dispatcher = Dispatchers.IO.limitedParallelism(config.workers)

        var total = 0
        var success = 0
        // Launch async tasks in parallel
        val deferredResults = apiChanges.flatMap { apiChange ->
            config.models.map { model ->
                async(dispatcher) {
                    var result = executeEntry(apiChange, model)
                    if (result.apiChange != apiChange) {
                        result = result.copy(
                            success = false,
                            output = "API change mismatch"
                        )
                    }
                    withContext(Dispatchers.Default) {
                        progressBar.step()
                        total++
                        success += if (result.success) 1 else 0
                        System.err.println("Total: $total, Success: $success")
                        System.err.flush()
                    }
                    result
                }
            }
        }

        val results = deferredResults.awaitAll()
        progressBar.close()
        results
    }

    fun run() = runBlocking {
        val apiChanges = parseInputFile()
        val results = processChanges(apiChanges)
        config.outputFile.toFile().writeText(Json.encodeToString(ListSerializer(TestResult.serializer()), results))
    }
}
