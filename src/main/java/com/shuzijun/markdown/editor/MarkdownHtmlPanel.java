package com.shuzijun.markdown.editor;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.ex.FileTypeChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.util.io.URLUtil;
import com.shuzijun.markdown.model.PluginConstant;
import com.shuzijun.markdown.util.FileUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandler;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefRequestHandler;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @author shuzijun
 */
public class MarkdownHtmlPanel extends JCEFHtmlPanel {
    private final CefRequestHandler requestHandler;
    private final CefLifeSpanHandler lifeSpanHandler;

    private final String url;
    private final Project project;

    public MarkdownHtmlPanel(@Nullable String url, Project project) {
        super("about:blank");
        this.url = url;
        this.project = project;
        getJBCefClient().addRequestHandler(requestHandler = new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                String requestUrl =request.getURL();
                if (requestUrl.startsWith(url)) {
                    return false;
                } else {
                    openUrl(URLDecoder.decode(requestUrl, StandardCharsets.UTF_8));
                    return true;
                }
            }
        }, getCefBrowser());
        getJBCefClient().addLifeSpanHandler(lifeSpanHandler = new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
                if (!target_url.startsWith(url)) {
                    openUrl(URLDecoder.decode(target_url, StandardCharsets.UTF_8));
                }
                return true;
            }
        }, getCefBrowser());
    }

    @Override
    public void dispose() {
        getJBCefClient().removeRequestHandler(requestHandler, getCefBrowser());
        getJBCefClient().removeLifeSpanHandler(lifeSpanHandler, getCefBrowser());
        super.dispose();
    }

    private void openUrl(String url) {
        if (url.startsWith(URLUtil.FILE_PROTOCOL)) {
            File file = new File(url.substring((URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + FileUtils.separator()).length()));
            if(!file.exists()){
                Notifications.Bus.notify(new Notification(PluginConstant.NOTIFICATION_GROUP, "Cannot Open File", file.getPath() + " not exist", NotificationType.INFORMATION), project);
            } else if (file.isDirectory()) {
                Notifications.Bus.notify(new Notification(PluginConstant.NOTIFICATION_GROUP, "Cannot Open Directory", file.getPath() + " is a directory", NotificationType.INFORMATION), project);
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                    FileEditor[] editors = FileEditorManager.getInstance(project).openFile(vf, false);
                    if (editors == null || editors.length == 0) {
                        FileType fileType = FileTypeChooser.getKnownFileTypeOrAssociate(vf, project);
                        if (fileType == null || fileType == FileTypes.UNKNOWN) {
                            return;
                        } else {
                            FileEditorManager.getInstance(project).openFile(vf, false);
                        }
                    }
                });
            }
        } else {
            BrowserUtil.browse(url);
        }
    }
}
