package com.springbootprojects.securacode

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.JFrame

class ShowStaticAnalysisAction : AnAction("Show Static Analysis") {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        // Create a JFrame to display the TerminalPanel
        val frame = JFrame("Static Code Analysis")
        val terminalPanel = TerminalPanel(project)

        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.add(terminalPanel)
        frame.setSize(800, 600)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        // Start code analysis when the panel is shown
        terminalPanel.startCodeAnalysis()
    }
}
