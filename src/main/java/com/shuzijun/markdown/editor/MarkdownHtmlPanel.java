package com.shuzijun.markdown.editor;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.URLUtil;
import com.intellij.util.ui.UIUtil;
import com.shuzijun.markdown.util.FileUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.lang.StringUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author shuzijun
 */
public class MarkdownHtmlPanel extends JCEFHtmlPanel {

    private static final Logger LOG = Logger.getInstance(MarkdownHtmlPanel.class);

    private final CefRequestHandler requestHandler;
    private final CefLifeSpanHandler lifeSpanHandler;
    //private final JBCefJSQuery findJSQuery;

    private final boolean isFileEditor;
    private final String url;
    private final Project project;
    private final List<String> iframe = new ArrayList<>();
    private static final List<String> headers = Arrays.asList(HttpHeaderNames.CONTENT_SECURITY_POLICY.toString(), HttpHeaderNames.CONTENT_ENCODING.toString(), HttpHeaderNames.CONTENT_LENGTH.toString());

    public MarkdownHtmlPanel(@Nullable String url, Project project, boolean isFileEditor) {
        super(offScreenRendering(isFileEditor), null, null);
        this.url = url;
        this.project = project;
        this.isFileEditor = isFileEditor;
        getJBCefClient().addRequestHandler(requestHandler = new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean user_gesture, boolean is_redirect) {
                String requestUrl = request.getURL();
                if (requestUrl.startsWith(url)) {
                    return false;
                } else if (!user_gesture) {
                    if (requestUrl.contains("csp=false")) {
                        iframe.add(requestUrl);
                    }
                    return false;
                } else {
                    openUrl(URLDecoder.decode(frame.getURL(), StandardCharsets.UTF_8), URLDecoder.decode(requestUrl, StandardCharsets.UTF_8));
                    return true;
                }
            }

            @Override
            public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame, CefRequest request, boolean isNavigation, boolean isDownload, String requestInitiator, BoolRef disableDefaultHandling) {
                String requestUrl = request.getURL();
                if (!iframe.contains(requestUrl)) {
                    return null;
                }

                return new CefResourceRequestHandlerAdapter() {

                    @Override
                    public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
                        try {
                            return HttpRequests.request(request.getURL()).throwStatusCodeException(false).connect(new HttpRequests.RequestProcessor<CefResourceHandler>() {
                                @Override
                                public CefResourceHandler process(HttpRequests.@NotNull Request request) throws IOException {
                                    HttpURLConnection urlConnection = (HttpURLConnection) request.getConnection();
                                    Map<String, String> header = new HashMap<>();
                                    urlConnection.getHeaderFields().forEach((key, values) -> {
                                        if (key != null && values != null && !headers.contains(key.toLowerCase())) {
                                            header.put(key, StringUtils.join(values.toArray(), ";"));
                                        }
                                    });
                                    return new ProxyLoadHtmlResourceHandler(request.readString(), header, urlConnection.getResponseCode());
                                }
                            });
                        } catch (IOException io) {

                            return null;
                        }
                    }
                };
            }
        }, getCefBrowser());
        getJBCefClient().addLifeSpanHandler(lifeSpanHandler = new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String target_url, String target_frame_name) {
                if (!target_url.startsWith(url)) {
                    openUrl(URLDecoder.decode(frame.getURL(), StandardCharsets.UTF_8), URLDecoder.decode(target_url, StandardCharsets.UTF_8));
                }
                return true;
            }
        }, getCefBrowser());
        /*findJSQuery = JBCefJSQuery.create(this);
        findJSQuery.addHandler(new Function<String, JBCefJSQuery.Response>() {
            @Override
            public JBCefJSQuery.Response apply(String find) {
                if (StringUtils.isEmpty(find)) {
                    getCefBrowser().stopFinding(true);
                    return null;
                }
                JSONObject findObject = JSONObject.parseObject(find);
                if (StringUtils.isEmpty(findObject.getString("searchText"))) {
                    getCefBrowser().stopFinding(true);
                    return null;
                }
                getCefBrowser().find(1, findObject.getString("searchText"), findObject.getBoolean("forward"), false, true);
                return null;
            }
        });*/
    }

    @Override
    protected DefaultCefContextMenuHandler createDefaultContextMenuHandler() {

        return new DefaultCefContextMenuHandler(true) {
            @Override
            public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
                if (isFileEditor) {
                    model.clear();
                    ActionGroup anAction = (ActionGroup) ActionManager.getInstance().getAction("EditorPopupMenu");
                    ActionPopupMenu actionPopupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_POPUP, anAction);
                    actionPopupMenu.setTargetComponent(getComponent());
                    final int x = params.getXCoord();
                    final int y = params.getYCoord();
                    ApplicationManager.getApplication().invokeLater(() -> actionPopupMenu.getComponent().show(getComponent(), x, y));
                } else {
                    super.onBeforeContextMenu(browser, frame, params, model);
                }
            }
        };

    }

    @Override
    public void dispose() {
        getJBCefClient().removeRequestHandler(requestHandler, getCefBrowser());
        getJBCefClient().removeLifeSpanHandler(lifeSpanHandler, getCefBrowser());
        //Disposer.dispose(findJSQuery);
        super.dispose();
    }

    private void openUrl(String frameUrl, String url) {
        if (url.startsWith(URLUtil.FILE_PROTOCOL)) {
            File file = new File(url.substring((URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + FileUtils.separator()).length()));
            if (!file.exists() || file.isDirectory()) {
                openSearchEverywhere(searchText(frameUrl, url));
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                    FileEditor[] editors = FileEditorManager.getInstance(project).openFile(vf, false);
                    if (editors == null || editors.length == 0) {
                        openSearchEverywhere(vf.getPath());
                    }
                });
            }
        } else {
            BrowserUtil.browse(url);
        }
    }

    private void openSearchEverywhere(String searchText) {
        SearchEverywhereManager manager = SearchEverywhereManager.getInstance(project);
        if (!manager.isShown()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                AnActionEvent anActionEvent = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, DataManager.getInstance().getDataContext(getComponent()));
                manager.show(SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID, searchText, anActionEvent);
            });
        }
    }

    private String searchText(String frameUrl, String url) {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(frameUrl)) {
            return url;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < url.length(); i++) {
            if (i >= frameUrl.length()) {
                sb.append(url.substring(i));
                break;
            } else if (url.charAt(i) != frameUrl.charAt(i)) {
                sb.append(url.substring(i));
                break;
            }
        }
        if (sb.length() == 0) {
            sb.append(url.substring((URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + FileUtils.separator()).length()));
        }
        return sb.toString();
    }

    public void updateStyle(String style) {
        getCefBrowser().executeJavaScript("updateStyle('" + style + "'," + UIUtil.isUnderDarcula() + ");", getCefBrowser().getURL(), 0);
    }

    public String getInjectScript() {
        //String script = "function find(searchText,forward){\n" + "        let findJson = '{\"searchText\":\"'+searchText+'\",\"forward\":'+forward+'}';\n" + "        " + findJSQuery.inject("findJson") + "    }";
        //return script;
        return "";
    }

    public void browserFind(String txt, boolean forward) {
        if (StringUtils.isEmpty(txt)) {
            getCefBrowser().stopFinding(true);
            return;
        }
        getCefBrowser().find(1, txt, forward, false, true);
    }

    private static boolean offScreenRendering(boolean isFileEditor) {
        /*if (!isFileEditor) {
            return false;
        }
        return Registry.is("ide.browser.jcef.markdownView.osr.enabled", true);*/
        return false;
    }
}
