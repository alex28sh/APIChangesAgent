# API Changes Agent

## Configuration

### Environment Variables
For the test running tool:
```
GRAGLE_HOME=/path/to/gragle
```

### Parameters
```
--inputPath, -i -> Input file containing API changes (always required) { String }
--outputPath, -o -> Output file to save results (always required) { String }
--models, -m -> Models for evaluation (always required) { Value should be one of [gpt4_1, gpt4o, gpt4omini, o1mini, o3mini, o1, o3] }
--openaiApiKey, -a [] -> API key for OpenAI { String }
--antropicApiKey, -A [] -> API key for Anthropic { String }
--workers, -w [4] -> Number of workers for parallel processing { Int }
--maxAgentIterations, -it [15] -> Maximum number of iterations for agent { Int }
--help, -h -> Usage info 
```

## Architecture description 

Tools are implemented in `org.example.tools` package: 
* ProjectGenerationToolSet.kt
    * Generates source code and gradle file as initial proccess of setting up a project.
* RepairingToolSet.kt
    * Edits source code and gradle file based on feedback from running tests.
* TestRunnnerToolSet.kt
    * Runs tests and collects results.
  
Main logic is implemented in `APiChangeEntry.kt`. It creates an `AIAgent` instance, that is then runned. 

Variable `strategy` describes actions to be performed by the agent based on the current state. It splits the pipeline into 2 stages (subgraphs):
1. Project Generation
2. Self-Repairing

These stages take different set of tools in their disposal. This separation is empirically shown to be effective. 

## Results

On a small bench of API changes `data/tests_spring_mini_filtered.json` the agent was able to fix 90% of the changes (gpt4.1 model) in 30 iterations.
Results are collected in `data/results_mini_filtered.json` file.

## Benchmark collection

[SpringEvo](https://github.com/alex28sh/SpringEvo)
