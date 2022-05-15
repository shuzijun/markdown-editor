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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.URLUtil;
import com.shuzijun.markdown.model.MarkdownResponse;
import com.shuzijun.markdown.model.PluginConstant;
import com.shuzijun.markdown.ui.ExportFileDialogWrapper;
import com.shuzijun.markdown.ui.PdfDialogWrapper;
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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

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
                String type = (typeData != null && typeData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) ? ((Attribute) typeData).getValue() : "html";
                Project project = getProject(projectNameParameter, projectUrlParameter);
                ExportFileDialogWrapper.FileSetting fileSetting = getFileSetting(project, virtualFile, type);
                if (!fileSetting.isOk()) {
                    return fillJsonResponse(MarkdownResponse.error("cancel").toString());
                }
                if ("html".equals(type)) {
                    if (themeCdnData != null && themeCdnData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        value = value.replaceAll(((Attribute) themeCdnData).getValue(), PluginConstant.CDN + PluginManagerCore.getPlugin(PluginId.getId(PluginConstant.PLUGIN_ID)).getVersion() + "/src/main/resources/vditor/dist/css/content-theme");
                    }
                    if (cdnData != null && cdnData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        value = value.replaceAll(((Attribute) cdnData).getValue(), PluginConstant.CDN + PluginManagerCore.getPlugin(PluginId.getId(PluginConstant.PLUGIN_ID)).getVersion() + "/src/main/resources/vditor");
                    }
                    FileUtils.saveFile(new File(fileSetting.filePath()), value);
                    FileUtils.refreshProjectDirectory(project, fileSetting.filePath());
                    Notifications.Bus.notify(new Notification(PluginConstant.NOTIFICATION_GROUP, "Export PDF", "Export success:" + fileSetting.filePath(), NotificationType.INFORMATION), project);
                    return fillJsonResponse(MarkdownResponse.success("").toString());
                } else if ("pdf".equals(type)) {
                    String finalValue = value;
                    String url = UrlEscapers.urlFragmentEscaper().escape(URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + FileUtils.separator() + virtualFile.getPath() + System.currentTimeMillis());
                    ApplicationManager.getApplication().invokeLater(() -> new PdfDialogWrapper(project, url, finalValue, fileSetting.filePath()).show());
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

    public ExportFileDialogWrapper.FileSetting getFileSetting(Project project, VirtualFile markdownFile, String type) {
        AtomicReference<ExportFileDialogWrapper.FileSetting> atomicReference = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            String path = markdownFile.getParent().getPath();
            String filename = markdownFile.getName().replaceAll("\\.(md|markdown)$", "." + type);
            ExportFileDialogWrapper fileDialogWrapper = new ExportFileDialogWrapper(project, path, filename, type);
            if (fileDialogWrapper.showAndGet()) {
                atomicReference.set(fileDialogWrapper.getSetting());
            } else {
                atomicReference.set(new ExportFileDialogWrapper.FileSetting());
            }
        });
        return atomicReference.get();
    }

}
