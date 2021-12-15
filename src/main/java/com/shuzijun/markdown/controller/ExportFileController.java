package com.shuzijun.markdown.controller;

import com.google.common.net.UrlEscapers;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.util.io.URLUtil;
import com.shuzijun.markdown.editor.MarkdownHtmlPanel;
import com.shuzijun.markdown.model.MarkdownResponse;
import com.shuzijun.markdown.model.PluginConstant;
import com.shuzijun.markdown.util.FileUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author shuzijun
 */
public class ExportFileController extends BaseController {

    private static final Logger LOG = Logger.getInstance(ExportFileController.class);

    private final String controllerPath = "exportFile";

    @Override
    public String getControllerPath() {
        return controllerPath;
    }

    @Override
    public FullHttpResponse post(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
        String fileParameter = getParameter(urlDecoder, "file");
        String projectNameParameter = getParameter(urlDecoder, "projectName");
        String projectUrlParameter = getParameter(urlDecoder, "projectUrl");
        FileApplicationService fileApplicationService = ApplicationManager.getApplication().getService(FileApplicationService.class);
        VirtualFile virtualFile = fileApplicationService.getVirtualFile(fileParameter, StringUtils.isNotBlank(projectUrlParameter) ? projectUrlParameter : projectNameParameter);
        if (virtualFile == null) {
            return fillJsonResponse(MarkdownResponse.error("unable to to find file " + fileParameter).toString());
        }

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        InterfaceHttpData valueData = decoder.getBodyHttpData("value");
        InterfaceHttpData typeData = decoder.getBodyHttpData("type");
        InterfaceHttpData themeCdnData = decoder.getBodyHttpData("themeCdn");
        InterfaceHttpData cdnData = decoder.getBodyHttpData("cdn");
        try {
            if (valueData != null && valueData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                String value = ((Attribute) valueData).getValue();
                String type = (typeData != null && typeData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) ?
                        ((Attribute) typeData).getValue() : "html";
                String fileName = virtualFile.getParent().getPath() + File.separator + virtualFile.getName().replaceAll("\\.(md|markdown)$", "." + type);
                File exportFile = new File(fileName);
                if (exportFile.exists()) {
                    fileName = virtualFile.getParent().getPath() + File.separator + System.currentTimeMillis() + "-" + virtualFile.getName().replaceAll("\\.(md|markdown)$", "." + type);
                    exportFile = new File(fileName);
                }
                Project project = getProject(projectNameParameter, projectUrlParameter);
                if ("html".equals(type)) {
                    if (themeCdnData != null && themeCdnData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        value = value.replaceAll(((Attribute) themeCdnData).getValue(), PluginConstant.CDN + PluginManagerCore.getPlugin(PluginId.getId(PluginConstant.PLUGIN_ID)).getVersion() + "/src/main/resources/vditor/dist/css/content-theme");
                    }
                    if (cdnData != null && cdnData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        value = value.replaceAll(((Attribute) cdnData).getValue(), PluginConstant.CDN + PluginManagerCore.getPlugin(PluginId.getId(PluginConstant.PLUGIN_ID)).getVersion() + "/src/main/resources/vditor");
                    }
                    FileUtils.saveFile(exportFile, value);
                    Notifications.Bus.notify(new Notification(PluginConstant.NOTIFICATION_GROUP, "Export PDF", "Export success:" + fileName, NotificationType.INFORMATION), project);
                    return fillJsonResponse(MarkdownResponse.success("").toString());
                } else if ("pdf".equals(type)) {
                    String finalValue = value;
                    String finalFileName = fileName;
                    String url = UrlEscapers.urlFragmentEscaper().escape(URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + FileUtils.separator() + virtualFile.getPath() + System.currentTimeMillis());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        PdfDialogWrapper pdfDialogWrapper = null;
                        try {
                            pdfDialogWrapper = new PdfDialogWrapper(project, url, finalValue, finalFileName);
                            if (pdfDialogWrapper.showAndGet()) {
                                Notifications.Bus.notify(new Notification(PluginConstant.NOTIFICATION_GROUP, "Export PDF", pdfDialogWrapper.getMessage(), NotificationType.INFORMATION), project);
                            }
                        } finally {
                            if (pdfDialogWrapper != null) {
                                pdfDialogWrapper.dispose();
                            }
                        }
                    });
                    return fillJsonResponse(MarkdownResponse.success("").toString());
                } else {
                    return fillJsonResponse(MarkdownResponse.error("Type " + type + " is not supported").toString());
                }
            } else {
                return fillJsonResponse(MarkdownResponse.error("The requested content is not supported").toString());
            }
        } finally {
            decoder.destroy();
        }
    }

    private class PdfDialogWrapper extends DialogWrapper {

        private JPanel jpanel;
        private JCEFHtmlPanel loginJCEFPanel;
        private String fileName;
        private String message = "";

        public PdfDialogWrapper(@Nullable Project project, String sourceUrl, String html, String fileName) {
            super(project, true);
            this.fileName = fileName;
            jpanel = new JBPanel();
            jpanel.setLayout(new BorderLayout());
            loginJCEFPanel = new MarkdownHtmlPanel(sourceUrl, project, false);
            loginJCEFPanel.getComponent().setMinimumSize(new Dimension(1000, 500));
            loginJCEFPanel.getComponent().setPreferredSize(new Dimension(1000, 500));
            loginJCEFPanel.loadHTML(html, sourceUrl);
            jpanel.add(new JBScrollPane(loginJCEFPanel.getComponent(), JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
            jpanel.repaint();
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
                public void actionPerformed(ActionEvent e) {
                    message = printPdf(fileName);
                    super.actionPerformed(e);
                }
            };
            action.putValue(Action.NAME, "Export");
            return action;
        }

        private String printPdf(String fileName) {
            CompletableFuture<String> successFuture = new CompletableFuture<>();
            loginJCEFPanel.getCefBrowser().printToPDF(fileName, null, (targetString, success) -> {
                if (success) {
                    successFuture.complete("Export success:" + fileName);
                } else {
                    successFuture.complete("Export failure");
                }
            });
            try {
                return successFuture.get(1, TimeUnit.MINUTES);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error(e);
                return "Export failure:" + e.getMessage();
            }
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
}
