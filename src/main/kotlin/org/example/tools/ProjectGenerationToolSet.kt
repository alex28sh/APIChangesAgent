package org.example.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.model.PromptExecutorExt.execute
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.runBlocking
import org.example.ApiChange

@LLMDescription("A set of tools for generating and managing java projects in a software development environment.")
class ProjectGenerationToolSet(val promptExecutor: PromptExecutor, val model: LLModel) : ToolSet {

    @Tool(customName = "generateJavaMethod")
    @LLMDescription("Generate source code for a Java method.")
    fun generateJavaMethod(
        @LLMDescription("The API change details including the function signature and query.")
        apiChange: ApiChange,
    ): String = runBlocking {
        // In a real implementation, this would use the promptExecutor to generate code
        val methodPrompt = getMethodPrompt(apiChange)
        promptExecutor.execute(methodPrompt, model = model).content
    }

    private fun getMethodPrompt(
        apiChange: ApiChange,
    ) : Prompt = prompt("generate-java-method") {
        system(
            """
            You are an expert Java programmer. Write a Java function implementation for the following task:
        
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
            """ .trimIndent()
        )
    }


    @Tool(customName = "generateGradleFile")
    @LLMDescription("Generate gradle file with all needed dependencies for Java method.")
    fun generateGradleFile(
        @LLMDescription("The API change details including the function signature and query.")
        apiChange: ApiChange,
        @LLMDescription("The generated Java code for the method.")
        generatedCode: String,
    ): String = runBlocking {
        // In a real implementation, this would use the promptExecutor to generate code
        val gradlePrompt = getGradlePrompt(apiChange, generatedCode)
        promptExecutor.execute(gradlePrompt, model = model).content
    }

    private fun getGradlePrompt(
        apiChange: ApiChange,
        generatedCode: String,
    ): Prompt = prompt("generate-gradle-file") {
        system(
            """
            You are an expert Java programmer. Write a Gradle build file for the following task:
        
            Task Description:
            ${apiChange.query}
        
            Required Function Signature:
            ```java
            ${apiChange.function_signature}
            ```
        
            Generated Java Code:
            ```java
            $generatedCode
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
            """.trimIndent()
        )
    }
}