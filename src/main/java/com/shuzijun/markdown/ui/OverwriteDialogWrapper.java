package com.shuzijun.markdown.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.shuzijun.markdown.util.PropertiesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author shuzijun
 */
public class OverwriteDialogWrapper extends DialogWrapper {

    private String filePath;

    private String tip;

    protected OverwriteDialogWrapper(@Nullable Project project, String filePath, String tip) {
        super(project);
        this.filePath = filePath;
        this.tip = tip;
        init();
        setTitle("Overwrite File");
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        return new JBLabel("<html><body>" + filePath + "<br> "+ PropertiesUtils.getInfo("overwrite.file")+"<br> " + tip + "<body></html>");
    }

    @Override
    protected @NotNull Action getOKAction() {
        Action action = super.getOKAction();
        action.putValue(Action.NAME, "Overwrite");
        return action;
    }
}
