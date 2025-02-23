package com.springbootprojects.securacode


import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

import javax.swing.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.awt.*
import java.io.IOException



class TerminalPanel(private val project: Project) : JPanel() {
    private val textArea: JTextArea
    private val apiKey = "AIzaSyCcqEpu4SV0_HBdg8PPZKFz-xjyHOqXnWg"
    private val editor = FileEditorManager.getInstance(project).selectedTextEditor

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        textArea = JTextArea(20, 60).apply {
            isEditable = false
            font = Font("Monospaced", Font.PLAIN, 12)
            lineWrap = true
            wrapStyleWord = true
        }

        add(JScrollPane(textArea))
        val startButton = JButton("Start Code Analysis").apply {
            addActionListener { startCodeAnalysis() }
        }
        add(startButton)
    }

    fun startCodeAnalysis() {
        val sourceDir = File(project.basePath ?: "")
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            appendText("⚠️ Source directory is invalid or not found.")
            return
        }

        appendText("🔍 Scanning project for Java files...\n")
        sourceDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "java") {
                val code = Files.readString(Paths.get(file.toURI()))
                analyzeCodeWithGemini(code, file.name)
            }
        }
    }


    private fun analyzeCodeWithGemini(code: String, fileName: String) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val escapedCode = code.replace("\"", "\\\"")

        val requestBody = """
        {
          "contents": [ {
            "parts": [ { "text": "Analyze the following Java code for security vulnerabilities:\n\n```java\n$escapedCode\n```\nFilename: $fileName" } ]
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
                    appendText("Raw response from API for $fileName:\n$responseBody\n")

                    if (response.isSuccessful) {
                        val vulnerabilities = parseGeminiResponse(responseBody)
                        saveAndOpenHtmlReport(fileName, vulnerabilities)
                    } else {
                        appendText("Failed to analyze $fileName: ${response.message}")
                    }
                }
            }
        })
    }

    /**
     * Saves the report as an HTML file and opens it in the browser.
     */

    /**/
    private fun saveAndOpenHtmlReport(fileName: String, vulnerabilities: String) {
        val htmlContent = generateHtmlReport(fileName, vulnerabilities)
        val reportFile = File("code_analysis_report.html")

        try {
            reportFile.writeText(htmlContent)
            appendText("Report saved: ${reportFile.absolutePath}")

            // Open the file in the default browser
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(reportFile.toURI())
            }
        } catch (e: IOException) {
            appendText("Error saving report: ${e.message}")
        }
    }

    private fun generateHtmlReport(fileName: String, vulnerabilities: String): String {
        val formattedVulnerabilities = formatVulnerabilityReport(vulnerabilities)

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Code Security Analysis - $fileName</title>
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap/5.3.0/css/bootstrap.min.css">
            <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.5.0/highlight.min.js"></script>
            <script>hljs.highlightAll();</script>
            <style>
                body { padding: 20px; font-family: Arial, sans-serif; }
                .container { max-width: 800px; margin: auto; }
                .header { text-align: center; margin-bottom: 20px; }
                .card { margin-top: 20px; }
                pre { background: #f4f4f4; padding: 10px; border-radius: 5px; }
                h3 { color: #d9534f; } /* Highlight vulnerabilities */
            </style>
        </head>
        <body>
            <div class="container">
                <h2 class="header">🔍 Code Security Analysis Report</h2>
                <div class="card">
                    <div class="card-header bg-danger text-white">⚠️ Detected Vulnerabilities</div>
                    <div class="card-body">
                        $formattedVulnerabilities
                    </div>
                </div>
                <footer class="text-center mt-4">
                    <small>Generated by Securacode</small>
                </footer>
            </div>
        </body>
        </html>
    """.trimIndent()
    }


    private fun formatVulnerabilityReport(rawText: String): String {
        val formattedReport = StringBuilder()

        // Convert **bold** text to <h3>
        val boldPattern = Regex("\\*\\*(.*?)\\*\\*")
        var htmlText = rawText.replace(boldPattern, "<h4>$1</h4>")

        // Convert * list items to <ul><li>
        val listPattern = Regex("\\* (.*?)\\n")
        htmlText = htmlText.replace(listPattern, "<ul><li>$1</li></ul>\n")

        // Convert 1. 2. 3. list items to <ol><li> (ordered list)
        val orderedListPattern = Regex("(\\d+)\\. (.*?)\\n")
        htmlText = htmlText.replace(orderedListPattern, "<ol><li>$2</li></ol>\n")
        // Convert ```java ... ``` to <pre><code>
        val codeBlockPattern = Regex("```java\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        htmlText = htmlText.replace(codeBlockPattern, "<pre><code class=\"language-java\">$1</code></pre>")

        // Convert double newlines into paragraph <p>
        htmlText = htmlText.replace("\n\n", "<p>")

        // Convert single newlines into line breaks <br>
      //  htmlText = htmlText.replace("\n", "<br>")

        formattedReport.append(htmlText)
        return formattedReport.toString()
    }


    // Parse the response from Gemini API to extract vulnerabilities
    private fun parseGeminiResponse(responseBody: String): String {
        val jsonResponse = JSONObject(responseBody)

        // Extract candidates array
        val candidatesArray = jsonResponse.optJSONArray("candidates") ?: return "No vulnerabilities found."

        if (candidatesArray.length() > 0) {
            val firstCandidate = candidatesArray.getJSONObject(0)

            // Extract content object
            val content = firstCandidate.optJSONObject("content") ?: return "No content in response."

            // Extract parts array
            val partsArray = content.optJSONArray("parts") ?: return "No parts in response."

            if (partsArray.length() > 0) {
                return partsArray.getJSONObject(0).optString("text", "No vulnerabilities found.")
            }
        }

        return "No vulnerabilities found."
    }


    // Helper function to append text to the text area on the UI thread
    private fun appendText(message: String) {
        SwingUtilities.invokeLater {
            textArea.append(message + "\n")
            textArea.caretPosition = textArea.document.length
        }
    }




}



