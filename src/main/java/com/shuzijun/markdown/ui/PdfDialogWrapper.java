package com.shuzijun.markdown.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.shuzijun.markdown.editor.MarkdownHtmlPanel;
import com.shuzijun.markdown.model.PluginConstant;
import com.shuzijun.markdown.util.FileUtils;
import org.cef.misc.CefPdfPrintSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author shuzijun
 */
public class PdfDialogWrapper extends DialogWrapper {

    private JPanel jpanel;
    private MarkdownHtmlPanel loginJCEFPanel;
    private String fileName;
    private String message = "";
    private Project project;

    public PdfDialogWrapper(@Nullable Project project, String sourceUrl, String html, String fileName) {
        super(project, true);
        this.project = project;
        this.fileName = fileName;
        jpanel = new JBPanel();
        jpanel.setLayout(new BorderLayout());
        loginJCEFPanel = new MarkdownHtmlPanel(sourceUrl, project, false);
        loginJCEFPanel.getComponent().setMinimumSize(new Dimension(1000, 500));
        loginJCEFPanel.getComponent().setPreferredSize(new Dimension(1000, 500));
        loginJCEFPanel.loadMyHTML(html, sourceUrl);
        jpanel.add(new JBScrollPane(loginJCEFPanel.getComponent(), JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        jpanel.repaint();
        setModal(false);
        init();
        setTitle("Preview  PDF");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jpanel;
    }

    @Override
    protected @NotNull Action getOKAction() {
        Action action = new OkAction() {
            @Override
            protected void doAction(ActionEvent e) {
                printPdf(fileName);
            }
        };
        action.putValue(Action.NAME, "Export");
        return action;
    }

    private void printPdf(String fileName) {
        loginJCEFPanel.getCefBrowser().printToPDF(fileName, new CefPdfPrintSettings(), (targetString, success) -> {
            if (success) {
                Notifications.Bus.notify(new Notification(PluginConstant.NOTIFICATION_GROUP, "Export PDF", "Export success:" + fileName, NotificationType.INFORMATION), project);
                FileUtils.refreshProjectDirectory(project, fileName);
            } else {
                Notifications.Bus.notify(new Notification(PluginConstant.NOTIFICATION_GROUP, "Export PDF", "Export failure", NotificationType.INFORMATION), project);
            }
            ApplicationManager.getApplication().invokeLater(() -> getCancelAction().actionPerformed(null));
        });
    }

    @Override
    protected void dispose() {
        loginJCEFPanel.dispose();
        super.dispose();
    }

    public String getMessage() {
        return message;
    }
}

