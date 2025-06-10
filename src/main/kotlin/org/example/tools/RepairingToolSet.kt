package org.example.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.PromptExecutorExt.execute
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import org.example.ApiChange
import org.example.TestResult

@LLMDescription("A set of tools for repairing java projects in a software development environment by the given feedback from test execution.")
class RepairingToolSet(val promptExecutor: PromptExecutor, val model: LLModel) : ToolSet {

    @Tool
    @LLMDescription("Repair source code for a Java method with the given error feedback.")
    fun repairJavaMethod(
        @LLMDescription("The API change details including the function signature and query.")
        apiChange: ApiChange,
        @LLMDescription("Previously generated source code")
        previousSourceCode: String,
        @LLMDescription("Previously generated Gradle build file")
        previousGradleBuild: String,
        @LLMDescription("Execution results of the previous code generation and testing")
        testResults: TestResult
    ): String = runBlocking {
        // In a real implementation, this would use the promptExecutor to generate code
        val methodPrompt = getMethodPrompt(apiChange, previousSourceCode, previousGradleBuild, testResults)
        promptExecutor.execute(methodPrompt, model = model).content
    }

    private fun getMethodPrompt(
        apiChange: ApiChange,
        previousSourceCode: String,
        previousGradleBuild: String,
        testResults: TestResult,
    ) : Prompt = Prompt.build("repair-java-method") {
        """
        Previously you generated a Java method and a Gradle build file for the following task:
    
        Task Description:
        ${apiChange.query}
    
        Required Function Signature:
        ```java
        ${apiChange.function_signature}
        ```
        
        Requirements:
        1. Implement ONLY the function with the given signature, no additional functions.
        2. Your implementation MUST use the specified API: ${apiChange.name}
        3. Make sure your code is compatible with Spring version ${apiChange.to_version}
        3. Do not include tests, main function, or any code outside the required function.
        4. Do not include additional comments or explanations.
    
        Respond with ONLY the Java function implementation, nothing else.
    
        ### Code Format ###
        ```java
        [All needed imports]
    
        public class ExampleSpringService {
        [Your code here]
        }
        ```
        
        Your source code from the previous attempt:
        $previousSourceCode
        
        Your Gradle build file from the previous attempt:
        $previousGradleBuild
        
        The test results from the previous attempt:
        ${testResults.output}
        
        Please generate a new Java method implementation that addresses errors occurred when testing your previously generated code.
        """ .trimIndent()
    }


    @Tool
    @LLMDescription("Repair gradle file with the given error feedback.")
    fun repairGradleFile(
        @LLMDescription("The API change details including the function signature and query.")
        apiChange: ApiChange,
        @LLMDescription("The repaired Java code for the method.")
        generatedCode: String,
        @LLMDescription("Previously generated source code")
        previousSourceCode: String,
        @LLMDescription("Previously generated Gradle build file")
        previousGradleBuild: String,
        @LLMDescription("Execution results of the previous code generation and testing")
        testResults: TestResult
    ): String = runBlocking {
        // In a real implementation, this would use the promptExecutor to generate code
        val gradlePrompt = getGradlePrompt(
            apiChange, generatedCode, previousSourceCode, previousGradleBuild, testResults
        )
        promptExecutor.execute(gradlePrompt, model = model).content
    }

    private fun getGradlePrompt(
        apiChange: ApiChange,
        generatedCode: String,
        previousSourceCode: String,
        previousGradleBuild: String,
        testResults: TestResult,
    ): Prompt = Prompt.build("repair-gradle-file") {
        """
        Previously you generated a Java method and a Gradle build file for the following task:
    
        Task Description:
        ${apiChange.query}
    
        Required Function Signature:
        ```java
        ${apiChange.function_signature}
        ```
    
        Requirements:
        1. Include all necessary dependencies for the function to compile and run.
        2. Ensure compatibility with Spring version ${apiChange.to_version}.
    
        Respond with ONLY the Gradle build file, nothing else.
    
        ### Gradle Build File Format ###
        ```groovy
        plugins {
            id 'java'
        }
        
        repositories {
            mavenCentral()
        }
        
        dependencies {
            // Add your dependencies here
            implementation 'org.springframework:spring-context:${apiChange.to_version}'
            testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
            // Other dependencies as needed
        }
        
        test {
            useJUnitPlatform()
        }
        ```
        
        Your source code from the previous attempt:
        $previousSourceCode
        
        Your Gradle build file from the previous attempt:
        $previousGradleBuild
        
        The test results from the previous attempt:
        ${testResults.output}
        
        Now, you fixed the Java method implementation, so you need to generate a new Gradle build file that includes all necessary dependencies for the function to compile and run.
        Your fixed Java code:
        $generatedCode
        """.trimIndent()
    }
}