package com.springbootprojects.securacode

import com.intellij.openapi.project.Project
import javax.swing.*
import java.awt.Font
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class TerminalPanel(private val project: Project) : JPanel() {
    private val textArea: JTextArea
    private val apiKey = "AIzaSyCcqEpu4SV0_HBdg8PPZKFz-xjyHOqXnWg"

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        textArea = JTextArea(20, 60).apply {
            isEditable = false
            font = Font("Monospaced", Font.PLAIN, 12)
            lineWrap = true
            wrapStyleWord = true
        }
        add(JScrollPane(textArea))
    }

    // Start the analysis of all Java files in the project
    fun startCodeAnalysis() {
        val sourceDir = File(project.basePath ?: "")
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            appendText("Source directory is invalid or not found.\n")
            return
        }

        sourceDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "java") {
                val code = Files.readString(Paths.get(file.toURI()))
                analyzeCodeWithGemini(code, file.name)
            }
        }
    }

    // Send the code snippet to the Gemini API for analysis
    private fun analyzeCodeWithGemini(code: String, fileName: String) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
        val requestBody = """
            {
              "contents": [ {
                "parts": [ { "text": "Analyze the following Java code for security vulnerabilities:\n\n```java\n$code\n```\nFilename: $fileName" } ]
              }]
            }
        """.trimIndent()

        val client = OkHttpClient()
        val body = requestBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                appendText("Error analyzing $fileName: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    if (response.isSuccessful) {
                        val vulnerabilities = parseGeminiResponse(responseBody)
                        appendText("Vulnerabilities in $fileName:\n$vulnerabilities")
                    } else {
                        appendText("Failed to analyze $fileName: ${response.message}")
                    }
                }
            }
        })
    }

    // Parse the response from Gemini API to extract vulnerabilities
    private fun parseGeminiResponse(responseBody: String): String {
        val jsonResponse = JSONObject(responseBody)
        val contentArray = jsonResponse.optJSONArray("contents")
        if (contentArray != null && contentArray.length() > 0) {
            val content = contentArray.getJSONObject(0)
            val partsArray = content.optJSONArray("parts")
            if (partsArray != null && partsArray.length() > 0) {
                return partsArray.getJSONObject(0).getString("text")
            }
        }
        return "No vulnerabilities found or unable to parse the response."
    }

    // Helper function to append text to the text area on the UI thread
    private fun appendText(message: String) {
        SwingUtilities.invokeLater {
            textArea.append(message + "\n")
            textArea.caretPosition = textArea.document.length
        }
    }
}
