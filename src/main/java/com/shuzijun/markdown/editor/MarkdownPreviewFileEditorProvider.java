package com.shuzijun.markdown.editor;


import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import com.shuzijun.markdown.model.PluginConstant;
import org.jetbrains.annotations.NotNull;

/**
 * @author shuzijun
 */
public class MarkdownPreviewFileEditorProvider  extends WeighedFileEditorProvider {
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        FileType fileType = file.getFileType();
        return (fileType.getDefaultExtension().equals("md") ||
                (fileType.getDefaultExtension().equals("") && file.getName().endsWith(".md")))
                && JBCefApp.isSupported();
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        return new MarkdownPreviewFileEditor(project, file);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return "Markdown Editor";
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
       String editorPolicy = PropertiesComponent.getInstance().getValue(PluginConstant.editorPolicyKey,FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR.name());
        return FileEditorPolicy.valueOf(editorPolicy);
    }
}
