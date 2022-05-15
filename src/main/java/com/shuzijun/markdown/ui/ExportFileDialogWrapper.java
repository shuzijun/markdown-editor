package com.shuzijun.markdown.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * @author shuzijun
 */
public class ExportFileDialogWrapper extends DialogWrapper {
    private JPanel jpanel;
    private JTextField fileNameField;
    private TextFieldWithBrowseButton directoryButton;

    private Project project;

    public ExportFileDialogWrapper(@Nullable Project project, String path, String fileName, String type) {
        super(project, true);
        this.project = project;
        directoryButton.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
        });
        directoryButton.setText(path);
        fileNameField.setText(fileName);
        init();
        setTitle("Export" + type.toUpperCase());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return jpanel;
    }

    public FileSetting getSetting() {

        return new FileSetting(directoryButton.getText(), fileNameField.getText());
    }

    @Override
    protected @NotNull Action getOKAction() {
        return new OkAction() {

            @Override
            protected void doAction(ActionEvent e) {
                File file = new File(getSetting().filePath());
                if (file.exists()) {
                    if (!new OverwriteDialogWrapper(project, file.getPath(), "").showAndGet()) {
                        return;
                    }
                } else if (file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                super.doAction(e);
            }
        };
    }


    public static class FileSetting {

        private boolean ok;

        private String directory;

        private String name;


        public FileSetting() {
        }

        public FileSetting(String directory, String name) {
            this.ok = true;
            this.directory = directory;
            this.name = name;
        }

        public boolean isOk() {
            return ok;
        }


        public String filePath() {
            return directory + File.separator + name;
        }


    }
}
