package com.shuzijun.markdown.editor;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.google.common.net.UrlEscapers;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.intellij.util.io.URLUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.shuzijun.markdown.controller.FileApplicationService;
import com.shuzijun.markdown.controller.PreviewStaticServer;
import com.shuzijun.markdown.model.PluginConstant;
import com.shuzijun.markdown.util.FileUtils;
import com.shuzijun.markdown.util.PropertiesUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * @author shuzijun
 */
public class MarkdownPreviewFileEditor extends UserDataHolderBase implements FileEditor {

    private static final Logger LOG = Logger.getInstance(MarkdownPreviewFileEditor.class);

    static final String URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS =
            "-._~" // Unreserved characters.
                    + "!$'()*,;&=" // The subdelim characters (excluding '+').
                    + "@:" // The gendelim characters permitted in paths.
                    + "/?"; // PATH
    private static final Escaper URL_FRAGMENT_ESCAPER = new PercentEscaper(URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS, true);

    private final Project myProject;
    private final VirtualFile myFile;
    private final Document myDocument;

    private final JPanel myHtmlPanelWrapper;
    private final MarkdownHtmlPanel myPanel;

    private final JBPanel toolbarPanel = new JBPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
    private final JBTextField searchField = new JBTextField();

    private final Url servicePath = BuiltInServerManager.getInstance().addAuthToken(Urls.parseEncoded("http://localhost:" + BuiltInServerManager.getInstance().getPort() + PreviewStaticServer.PREFIX));
    private final String templateHtmlFile = "template/default.html";
    private final boolean isPresentableUrl;

    public MarkdownPreviewFileEditor(@NotNull Project project, @NotNull VirtualFile file) {
        myProject = project;
        myFile = file;
        myDocument = FileDocumentManager.getInstance().getDocument(myFile);
        myHtmlPanelWrapper = new JPanel(new BorderLayout());
        isPresentableUrl = project.getPresentableUrl() != null;
        String url = UrlEscapers.urlFragmentEscaper().escape(URLUtil.FILE_PROTOCOL + URLUtil.SCHEME_SEPARATOR + FileUtils.separator() + myFile.getPath());
        MarkdownHtmlPanel tempPanel = null;
        try {
            tempPanel = new MarkdownHtmlPanel(url, project, true);
            tempPanel.loadMyHTML(createHtml(isPresentableUrl, tempPanel), url);
            myHtmlPanelWrapper.add(tempPanel.getComponent(), BorderLayout.CENTER);

            MarkdownHtmlPanel finalTempPanel = tempPanel;
            toolbarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()));
            searchField.setPreferredSize(new Dimension(260, 28));
            searchField.putClientProperty("JTextField.placeholderText", "Search in Markdown");
            searchField.addActionListener(event -> finalTempPanel.browserFind(searchField.getText(), true));
            searchField.getDocument().addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    finalTempPanel.browserFind(searchField.getText(), true);
                }
            });
            JButton previousButton = createSearchButton("Previous", "Previous match", () -> finalTempPanel.browserFind(searchField.getText(), false));
            JButton nextButton = createSearchButton("Next", "Next match", () -> finalTempPanel.browserFind(searchField.getText(), true));
            JButton closeButton = createSearchButton("Close", "Close search", () -> {
                searchField.setText("");
                toolbarPanel.setVisible(false);
            });
            toolbarPanel.add(searchField);
            toolbarPanel.add(previousButton);
            toolbarPanel.add(nextButton);
            toolbarPanel.add(closeButton);
            AnAction searchAction = ActionManager.getInstance().getAction("markdown.search");
            AnAction searchVisibleAction = ActionManager.getInstance().getAction("markdown.searchVisible");
            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(PluginConstant.EDITOR_TOOLBAR, new DefaultActionGroup(searchAction, searchVisibleAction), true);
            actionToolbar.setTargetComponent(myHtmlPanelWrapper);
            JComponent actionToolbarComponent = actionToolbar.getComponent();
            actionToolbarComponent.setVisible(false);
            toolbarPanel.add(actionToolbarComponent);
            toolbarPanel.setVisible(false);
            myHtmlPanelWrapper.add(toolbarPanel, BorderLayout.NORTH);
        } catch (IllegalStateException e) {
            myHtmlPanelWrapper.add(new JBLabel(e.getMessage()), BorderLayout.CENTER);
        }
        myPanel = tempPanel;
        myHtmlPanelWrapper.repaint();

        FileApplicationService fileApplicationService = ApplicationManager.getApplication().getService(FileApplicationService.class);
        fileApplicationService.putVirtualFile(myFile.getPath(), isPresentableUrl ? project.getPresentableUrl() : project.getName(), myFile);

        MessageBusConnection settingsConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
        settingsConnection.subscribe(EditorColorsManager.TOPIC, new EditorColorsListener() {
            @Override
            public void globalSchemeChange(@Nullable EditorColorsScheme scheme) {
                EditorColorsScheme currentScheme = scheme == null ? EditorColorsManager.getInstance().getGlobalScheme() : scheme;
                myPanel.updateStyle(getStyle(false), isDarkTheme(currentScheme.getDefaultBackground()));
            }
        });

    }


    @Override
    public @NotNull JComponent getComponent() {
        return myHtmlPanelWrapper;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return myPanel != null ? myPanel.getComponent() : null;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "Markdown Editor";
    }

    @Override
    public void setState(@NotNull FileEditorState state) {

    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {

    }

    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    @Override
    public StructureViewBuilder getStructureViewBuilder() {
        VirtualFile file = FileDocumentManager.getInstance().getFile(myDocument);
        if (file == null || !file.isValid()) return null;
        PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
        return psiFile == null ? null : LanguageStructureViewBuilder.getInstance().getStructureViewBuilder(psiFile);
    }

    @Override
    public void dispose() {
        ApplicationManager.getApplication().getService(FileApplicationService.class)
                .removeVirtualFile(myFile.getPath(), isPresentableUrl ? myProject.getPresentableUrl() : myProject.getName());
        if (myPanel != null) {
            Disposer.dispose(myPanel);
        }
    }

    public void openDevtools() {
        if (myPanel != null) {
            myPanel.openDevtools();
        }
    }

    private String createHtml(boolean isPresentableUrl, MarkdownHtmlPanel tempPanel) {
        InputStream inputStream = null;

        try {
            File templateFile = new File(PluginConstant.TEMPLATE_PATH + templateHtmlFile);
            if (templateFile.exists()) {
                inputStream = new FileInputStream(templateFile);
            } else {
                inputStream = PreviewStaticServer.class.getResourceAsStream("/" + templateHtmlFile);
            }
            String template = new String(FileUtilRt.loadBytes(inputStream));
            return template.replace("{{service}}", servicePath.getScheme() + URLUtil.SCHEME_SEPARATOR + servicePath.getAuthority() + servicePath.getPath())
                    .replace("{{serverToken}}", StringUtils.isNotBlank(servicePath.getParameters()) ? servicePath.getParameters().substring(1) : "")
                    .replace("{{filePath}}", URL_FRAGMENT_ESCAPER.escape(myFile.getPath()))
                    .replace("{{Lang}}", PropertiesUtils.getInfo("Lang"))
                    .replace("{{darcula}}", isDarkTheme(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground()) + "")
                    .replace("{{userTemplate}}", templateFile.exists() + "")
                    .replace("{{projectUrl}}", isPresentableUrl ? URL_FRAGMENT_ESCAPER.escape(myProject.getPresentableUrl()) : "")
                    .replace("{{projectName}}", isPresentableUrl ? "" : URL_FRAGMENT_ESCAPER.escape(myProject.getName()))
                    .replace("{{ideStyle}}", getStyle(true))
                    .replace("{{injectScript}}", tempPanel.getInjectScript())
                    ;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return myFile;
    }

    private String getStyle(boolean isTag) {
        try {
            EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
            Color defaultBackground = editorColorsScheme.getDefaultBackground();

            Color text = editorColorsScheme.getDefaultForeground();
            Color scrollbarThumbColor = UIManager.getColor("ScrollBar.thumb");
            if (scrollbarThumbColor == null) {
                scrollbarThumbColor = text == null ? defaultBackground : text;
            }
            String fontFamily = "font-family:\"" + editorColorsScheme.getEditorFontName() + "\",\"Helvetica Neue\",\"Luxi Sans\",\"DejaVu Sans\"," +
                    "\"Hiragino Sans GB\",\"Microsoft Yahei\",sans-serif,\"Apple Color Emoji\",\"Segoe UI Emoji\",\"Noto Color Emoji\",\"Segoe UI Symbol\"," +
                    "\"Android Emoji\",\"EmojiSymbols\";";
            StringBuilder sb = new StringBuilder(isTag ? "<style id=\"ideaStyle\">" : "");
            boolean darkTheme = isDarkTheme(defaultBackground);
            sb.append(darkTheme ? ".vditor--dark" : ".vditor").append("{--panel-background-color:").append(toHexColor(defaultBackground))
                    .append(";--textarea-background-color:").append(toHexColor(defaultBackground)).append(";");
            sb.append("--toolbar-background-color:").append(toHexColor(JBColor.background())).append(";");
            sb.append("--border-color:").append(toHexColor(JBColor.border())).append(";");
            sb.append("}");
            sb.append("::-webkit-scrollbar-track {background-color:").append(toHexColor(defaultBackground)).append(";}");
            sb.append("::-webkit-scrollbar-thumb {background-color:").append(toHexColor(scrollbarThumbColor)).append(";}");
            sb.append(".vditor-reset {font-size:").append(editorColorsScheme.getEditorFontSize()).append("px;");
            sb.append(fontFamily);
            if (text != null) {
                sb.append("color:").append(toHexColor(text)).append(";");
            }
            sb.append("}");
            if (text != null) {
                sb.append(".vditor-reset table {color:").append(toHexColor(text)).append(";}");
            }
            Color linkColor = UIManager.getColor("Link.activeForeground");
            if (linkColor == null) {
                linkColor = JBColor.BLUE;
            }
            Color borderColor = JBColor.border();
            Color codeBackground = darkTheme ? new Color(255, 255, 255, 18) : new Color(0, 0, 0, 8);
            Color quoteBackground = darkTheme ? new Color(255, 255, 255, 12) : new Color(0, 0, 0, 6);
            sb.append(".vditor-reset a{color:").append(toHexColor(linkColor)).append(";}");
            sb.append(".vditor-reset h1,.vditor-reset h2,.vditor-reset h3,.vditor-reset h4,.vditor-reset h5,.vditor-reset h6{");
            if (text != null) {
                sb.append("color:").append(toHexColor(text)).append(";");
            }
            sb.append("border-bottom-color:").append(toHexColor(borderColor)).append(";}");
            sb.append(".vditor-reset blockquote{background-color:").append(toHexColor(quoteBackground))
                    .append(";border-left-color:").append(toHexColor(linkColor)).append(";}");
            sb.append(".vditor-reset code:not(.hljs){background-color:").append(toHexColor(codeBackground))
                    .append(";border-color:").append(toHexColor(borderColor)).append(";}");
            sb.append(".vditor-reset pre{background-color:").append(toHexColor(codeBackground))
                    .append(";border-color:").append(toHexColor(borderColor)).append(";}");
            sb.append(".vditor-reset table th,.vditor-reset table td{border-color:").append(toHexColor(borderColor)).append(";}");
            sb.append(isTag ? "</style>" : "");
            LOG.info("markdown style: " + sb + " ; Dark theme: " + darkTheme);
            return sb.toString();
        } catch (Exception e) {
            LOG.info("Failed to create style", e);
            return "";
        }

    }

    private boolean isDarkTheme(Color background) {
        double luminance = (0.2126 * background.getRed() + 0.7152 * background.getGreen() + 0.0722 * background.getBlue()) / 255;
        return luminance < 0.5;
    }

    private String toHexColor(Color color) {
        DecimalFormat df = new DecimalFormat("0.00");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);
        return String.format("rgba(%s,%s,%s,%s)", color.getRed(), color.getGreen(), color.getBlue(), df.format(color.getAlpha() / (float) 255));
    }

    public void visibleToolbarPanel(boolean visible) {
        toolbarPanel.setVisible(visible);
        if (visible) {
            searchField.requestFocus();
        } else {
            searchField.setText("");
        }
    }

    private JButton createSearchButton(String text, String tooltip, Runnable action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setMargin(new Insets(2, 8, 2, 8));
        button.addActionListener(event -> action.run());
        return button;
    }
}
