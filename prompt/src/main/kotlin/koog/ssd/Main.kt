package koog.ssd

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun main(args: Array<String>) {
    runBlocking {
        val promptDirPath = args.firstOrNull() ?: "files"
        val promptDir = File(promptDirPath).also { it.mkdirs() }
        val model =
            // "https://ollama.com/library/qwen3"
            LLModel(
                provider = LLMProvider.Ollama,
                id = "qwen3:4b",
                capabilities =
                    listOf(
                        LLMCapability.Document,
                        LLMCapability.Schema.JSON.Basic,
                        LLMCapability.Temperature,
                        LLMCapability.Tools,
                    ),
                contextLength = 256_000,
            )
        val llmClient = OllamaClient()
        llmClient.getModelOrNull(model.id, pullIfMissing = true)

        val promptExecutor = SingleLLMPromptExecutor(llmClient)

        val jobDescriptionFile = File(promptDir, "job-description.md")
        if (!jobDescriptionFile.exists()) {
            println("Generating job-description.md...")
            val jdPrompt =
                prompt(id = Uuid.random().toString()) {
                    user(content = "Generate a professional job description for a Senior Software Developer position.")
                }
            val response = promptExecutor.execute(prompt = jdPrompt, model = model).single()
            jobDescriptionFile.writeText(response.content)
            println("job-description.md generated.")
        }

        val resumeFile = File(promptDir, "resume.md")
        if (!resumeFile.exists()) {
            println("Generating resume.md...")
            val resumePrompt =
                prompt(id = Uuid.random().toString()) {
                    user(content = "Generate a professional resume for a Senior Software Developer.")
                }
            val response = promptExecutor.execute(prompt = resumePrompt, model = model).single()
            resumeFile.writeText(response.content)
            println("resume.md generated.")
        }

        println("Adapting resume to job description...")
        val adaptPrompt =
            prompt(id = Uuid.random().toString()) {
                system(
                    content =
                        "You are an expert career coach. Your task is to adapt the " +
                            "provided resume to better align with the given job description.",
                )
                user {
                    file(
                        ContentPart.File(
                            content = AttachmentContent.PlainText(jobDescriptionFile.readText()),
                            format = "md",
                            mimeType = "text/markdown",
                            fileName = jobDescriptionFile.name,
                        ),
                    )
                    file(
                        ContentPart.File(
                            content = AttachmentContent.PlainText(resumeFile.readText()),
                            format = "md",
                            mimeType = "text/markdown",
                            fileName = resumeFile.name,
                        ),
                    )
                    text(
                        "Please generate a new resume that highlights relevant skills and experiences from my resume that match the job description. Save the result as a new resume.",
                    )
                }
            }

        val response = promptExecutor.execute(prompt = adaptPrompt, model = model).single()
        val newResumeFile = File(promptDir, "new-resume.md")
        newResumeFile.writeText(response.content)
        println("new-resume.md generated.")
    }
}
