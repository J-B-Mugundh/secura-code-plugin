package com.springbootprojects.securacode

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class MyPluginAction : AnAction() {
    // Define a set of common vulnerabilities or issues to check in the code
    private val vulnerabilities = listOf(
            ".*System\\.out\\.print.*" to "Improper logging (System.out.print)",
            ".*executeQuery\\(.*\"SELECT \\* FROM\".*\\).*" to "Possible SQL Injection (SELECT * FROM)",
            ".*request\\.getParameter\\(.*\\).*" to "Potential User Input without Validation (request.getParameter)",
            ".*new File\\(.*\\).*" to "Unvalidated Path Manipulation (new File)"
    )

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        if (project == null) {
            Messages.showMessageDialog(
                    "Project is not available.",
                    "Error",
                    Messages.getErrorIcon()
            )
            return
        }

        // Displaying the message to indicate the action was triggered
        Messages.showMessageDialog(
                project,
                "Static code analysis started...",
                "Static Code Analysis",
                Messages.getInformationIcon()
        )

        // Get the source directory of the project
        val sourceDir = File(project.basePath ?: "")
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            Messages.showMessageDialog(
                    project,
                    "Source directory is invalid or not found.",
                    "Error",
                    Messages.getErrorIcon()
            )
            return
        }

        // Scan Java files for vulnerabilities
        val filesWithIssues = mutableListOf<String>()
        scanFiles(sourceDir, filesWithIssues)

        // Show the results of the scan
        if (filesWithIssues.isNotEmpty()) {
            Messages.showMessageDialog(
                    project,
                    "Found issues in the following files:\n${filesWithIssues.joinToString("\n")}",
                    "Code Analysis Results",
                    Messages.getWarningIcon()
            )
        } else {
            Messages.showMessageDialog(
                    project,
                    "No issues found in your code.",
                    "Code Analysis Completed",
                    Messages.getInformationIcon()
            )
        }
    }

    // Function to recursively scan files in the source directory
    private fun scanFiles(directory: File, filesWithIssues: MutableList<String>) {
        directory.walkTopDown().forEach { file ->
            // Only scan .java files
            if (file.isFile && file.extension == "java") {
                scanFile(file, filesWithIssues)
            }
        }
    }

    // Function to scan a single file for vulnerabilities
    private fun scanFile(file: File, filesWithIssues: MutableList<String>) {
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
}
