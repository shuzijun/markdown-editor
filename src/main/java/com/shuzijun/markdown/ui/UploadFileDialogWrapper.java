package com.shuzijun.markdown.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.shuzijun.markdown.util.PropertiesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * @author shuzijun
 */
public class UploadFileDialogWrapper extends DialogWrapper {
    private JPanel jpanel;
    private JTextField fileNameField;
    private JCheckBox relativePathCheckBox;
    private TextFieldWithBrowseButton directoryButton;
    private boolean overwrite = false;

    private Project project;

    public UploadFileDialogWrapper(@Nullable Project project, String path, String fileName) {
        super(project, true);
        this.project = project;
        directoryButton.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()) {
        });
        directoryButton.setText(path);
        fileNameField.setText(fileName);
        init();
        setTitle("Upload");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return jpanel;
    }

    public FileSetting getSetting() {

        return new FileSetting(directoryButton.getText(), fileNameField.getText(), relativePathCheckBox.isSelected(), overwrite);
    }

    @Override
    protected @NotNull Action getOKAction() {
        return new OkAction() {

            @Override
            protected void doAction(ActionEvent e) {
                File file = new File(getSetting().filePath());
                if (file.exists()) {
                    if (!new OverwriteDialogWrapper(project, file.getPath(),
                            PropertiesUtils.getInfo("overwrite.img")).showAndGet()) {
                        return;
                    } else {
                        overwrite = true;
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

        private boolean relativePath;

        private boolean overwrite = false;

        public FileSetting() {
        }

        public FileSetting(String directory, String name, boolean relativePath, boolean overwrite) {
            this.ok = true;
            this.directory = directory;
            this.name = name;
            this.relativePath = relativePath;
            this.overwrite = overwrite;
        }

        public boolean isOk() {
            return ok;
        }

        public boolean isRelativePath() {
            return relativePath;
        }

        public boolean isOverwrite() {
            return overwrite;
        }

        public String filePath() {
            return directory + File.separator + name;
        }

    }
}
