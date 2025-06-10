package org.example

import kotlinx.cli.*
import kotlin.io.path.Path

fun createConfig(args: Array<String>, parser: ArgParser) : Config {
    val inputPath by parser.option(ArgType.String, shortName = "i", description = "Input file containing API changes")
        .required()
    val outputPath by parser.option(ArgType.String, shortName = "o", description = "Output file to save results")
        .required()

    val models by parser.option(ArgType.Choice<Model>(), shortName = "m", description = "Models for evaluation")
        .multiple().required()
    val openaiApiKey by parser.option(ArgType.String, shortName = "a", description = "API key for OpenAI")
        .default(System.getenv("OPENAI_API_KEY") ?: "")
    val antropicApiKey by parser.option(ArgType.String, shortName = "A", description = "API key for Anthropic")
        .default(System.getenv("ANTHROPIC_API_KEY") ?: "")

    val workers by parser.option(ArgType.Int, shortName = "w", description = "Number of workers for parallel processing")
        .default(4)
    val maxAgentIterations by parser.option(ArgType.Int, shortName = "it", description = "Maximum number of iterations for agent")
        .default(15)

    parser.parse(args)
    // Validate the configuration
    if (openaiApiKey.isEmpty() && antropicApiKey.isEmpty()) {
        throw IllegalArgumentException("Please provide at least one API key using -a or -A options.")
    }

    if (models.isEmpty()) {
        throw IllegalArgumentException("Please provide at least one model using -m option.")
    }

    val inputFile = Path(inputPath)
    val outputFile = Path(outputPath)
    if (!inputFile.toFile().exists()) {
        throw IllegalArgumentException("Input file does not exist: $inputPath")
    }

    return Config(
        inputFile = inputFile,
        outputFile = outputFile,
        models = models,
        openaiApiKey = openaiApiKey,
        antropicApiKey = antropicApiKey,
        workers = workers,
        maxAgentIterations = maxAgentIterations
    )
}

fun main(args: Array<String>) {

    val parser = ArgParser("APIChangesAgent")

    val config = createConfig(args, parser)

    Runner(config).run()
}
