package org.example

import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel
import java.nio.file.Path

enum class Model(val model: LLModel) {
    GPT4_1(OpenAIModels.Chat.GPT4_1),
    GPT4o(OpenAIModels.Chat.GPT4o),
    GPT4oMini(OpenAIModels.Reasoning.GPT4oMini),
    O1Mini(OpenAIModels.Reasoning.O1Mini),
    O3Mini(OpenAIModels.Reasoning.O3Mini),
    O1(OpenAIModels.Reasoning.O1),
    O3(OpenAIModels.Reasoning.O3),
}

data class Config(
    val inputFile: Path,
    val outputFile: Path,
    val models: List<Model>,
    val openaiApiKey: String,
    val antropicApiKey: String,
    val workers: Int = 4,
    val maxAgentIterations: Int = 15
)
