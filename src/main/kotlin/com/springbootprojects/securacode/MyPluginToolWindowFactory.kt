package com.springbootprojects.securacode

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel

class MyPluginToolWindowFactory : ToolWindowFactory {

    private val toolWindowLogger = Logger.getInstance(MyPluginToolWindowFactory::class.java)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Create a terminal-like panel to display the results
        val terminalPanel = TerminalPanel(project)

        // Adding the terminal panel as a content to the tool window
        val content = ContentFactory.getInstance().createContent(terminalPanel, "", false)
        toolWindow.contentManager.addContent(content)

        // Trigger the analysis when the tool window is created
        terminalPanel.startCodeAnalysis(project)
    }
}
