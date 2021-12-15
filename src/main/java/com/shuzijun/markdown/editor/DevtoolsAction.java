package com.shuzijun.markdown.editor;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author shuzijun
 */
public class DevtoolsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            MarkdownPreviewFileEditor fileEditor = (MarkdownPreviewFileEditor) FileEditorManager.getInstance(e.getProject()).getSelectedEditor();
            fileEditor.openDevtools();
        }catch (Exception ignore){

        }


    }
}
