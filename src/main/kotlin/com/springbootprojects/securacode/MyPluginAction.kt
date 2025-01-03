package com.springbootprojects.securacode

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.content.ContentFactory
import java.awt.Font
import javax.swing.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class MyPluginAction : AnAction() {

    private val toolWindowLogger = Logger.getInstance(MyPluginAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Get the tool window instance
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("StaticCodeAnalysisToolWindow")
        if (toolWindow != null) {
            toolWindow.show()
        } else {
            Messages.showErrorDialog(project, "Tool Window not available", "Error")
        }
    }
}

class TerminalPanel(private val project: Project) : JPanel() {
    private val textArea: JTextArea

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        textArea = JTextArea(20, 60)
        textArea.isEditable = false
        textArea.font = Font("Monospaced", Font.PLAIN, 12)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        val scrollPane = JScrollPane(textArea)

        add(scrollPane)
    }

    // This method performs the static code analysis and appends results to the terminal-like panel
    fun startCodeAnalysis(project: Project) {
        val vulnerabilities = listOf(
            ".*System\\.out\\.print.*" to "Improper logging (System.out.print)",
            ".*executeQuery\\(.*\"SELECT \\* FROM\".*\\).*" to "Possible SQL Injection (SELECT * FROM)",
            ".*request\\.getParameter\\(.*\\).*" to "Potential User Input without Validation (request.getParameter)",
            ".*new File\\(.*\\).*" to "Unvalidated Path Manipulation (new File)"
        )

        // Get the source directory of the project
        val sourceDir = File(project.basePath ?: "")
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            appendText("Source directory is invalid or not found.\n")
            return
        }

        // Scan Java files for vulnerabilities
        val filesWithIssues = mutableListOf<String>()
        scanFiles(sourceDir, filesWithIssues, vulnerabilities)

        // Show the results of the scan in the terminal panel
        if (filesWithIssues.isNotEmpty()) {
            appendText("Found issues in the following files:\n${filesWithIssues.joinToString("\n")}")
        } else {
            appendText("No issues found in your code.")
        }
    }

    // Function to recursively scan files in the source directory
    private fun scanFiles(directory: File, filesWithIssues: MutableList<String>, vulnerabilities: List<Pair<String, String>>) {
        directory.walkTopDown().forEach { file ->
            // Only scan .java files
            if (file.isFile && file.extension == "java") {
                scanFile(file, filesWithIssues, vulnerabilities)
            }
        }
    }

    // Function to scan a single file for vulnerabilities
    private fun scanFile(file: File, filesWithIssues: MutableList<String>, vulnerabilities: List<Pair<String, String>>) {
        val lines = Files.readAllLines(Paths.get(file.toURI()))
        var foundIssue = false

        // Check each line of the file for known vulnerabilities
        for ((rule, message) in vulnerabilities) {
            for ((lineNumber, line) in lines.withIndex()) {
                if (line.matches(Regex(rule))) {
                    foundIssue = true
                    filesWithIssues.add("${file.name} - Line ${lineNumber + 1}: $message")
                }
            }
        }

        // If we found an issue in the file, add it to the list
        if (foundIssue) {
            filesWithIssues.add("Issues found in file: ${file.name}")
        }
    }

    // Method to append text to the JTextArea (the terminal window)
    private fun appendText(message: String) {
        SwingUtilities.invokeLater {
            textArea.append(message + "\n")
            textArea.setCaretPosition(textArea.document.length)  // Auto scroll to the bottom
        }
    }
}

