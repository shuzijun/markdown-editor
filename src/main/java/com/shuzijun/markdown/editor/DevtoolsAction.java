package com.shuzijun.markdown.editor;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author shuzijun
 */
public class DevtoolsAction extends AnAction {

    @Override
    public void update(AnActionEvent e) {
        FileEditor fileEditor = FileEditorManager.getInstance(e.getProject()).getSelectedEditor();
        if(fileEditor != null && fileEditor instanceof MarkdownPreviewFileEditor){
            e.getPresentation().setEnabledAndVisible(true);
        }else {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            MarkdownPreviewFileEditor fileEditor = (MarkdownPreviewFileEditor) FileEditorManager.getInstance(e.getProject()).getSelectedEditor();
            fileEditor.openDevtools();
        }catch (Exception ignore){

        }


    }
}
