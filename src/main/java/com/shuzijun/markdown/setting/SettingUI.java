package com.shuzijun.markdown.setting;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.io.HttpRequests;
import com.shuzijun.markdown.model.PluginConstant;
import com.shuzijun.markdown.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * @author shuzijun
 */
public class SettingUI {

    private JPanel mainPanel;
    private JButton syncButton;
    private TextFieldWithBrowseButton templatePathField;
    private JComboBox editorPolicyBox;
    private JTextField assetsPath;
    private JComboBox assetsName;

    public SettingUI() {
        templatePathField.setText(PluginConstant.TEMPLATE_PATH);
        syncButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    syncButton.setEnabled(false);
                    try {
                        sync(templatePathField.getText());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        syncButton.setForeground(Color.RED);
                        syncButton.setToolTipText("Synchronization failure");
                    }finally {
                        syncButton.setForeground(Color.BLUE);
                        syncButton.setToolTipText("Synchronous success");
                        syncButton.setEnabled(true);
                    }
                });
            }
        });
        for (FileEditorPolicy value : FileEditorPolicy.values()) {
            editorPolicyBox.addItem(value.name());
        }
        editorPolicyBox.setSelectedItem(PropertiesComponent.getInstance().getValue(PluginConstant.editorPolicyKey,FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR.name()));
        assetsPath.setText(PropertiesComponent.getInstance().getValue(PluginConstant.editorAssetsPathKey,"assets"));
        assetsName.setSelectedItem(PropertiesComponent.getInstance().getValue(PluginConstant.editorAssetsNameAutoKey,"Rename"));
    }

    public JPanel getContentPane() {
        return mainPanel;
    }

    public boolean isModified() {
        return !PropertiesComponent.getInstance().getValue(PluginConstant.editorPolicyKey,FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR.name()).equals(editorPolicyBox.getSelectedItem())
                || PropertiesComponent.getInstance().getValue(PluginConstant.editorAssetsPathKey,"assets").equals(assetsPath.getText())
                || PropertiesComponent.getInstance().getValue(PluginConstant.editorAssetsNameAutoKey,"Rename").equals(assetsName.getSelectedItem());
    }

    public void apply() {
        PropertiesComponent.getInstance().setValue(PluginConstant.editorPolicyKey,editorPolicyBox.getSelectedItem().toString());
        PropertiesComponent.getInstance().setValue(PluginConstant.editorAssetsPathKey,assetsPath.getText());
        PropertiesComponent.getInstance().setValue(PluginConstant.editorAssetsNameAutoKey,assetsName.getSelectedItem().toString());
    }

    public void reset() {

    }

    public void disposeUIResources() {

    }



    private static void sync(String filePath) throws IOException {
        String versionStr = HttpRequests.request(PluginConstant.JS_DELIVR_ENDPOINTS).readString();
        String version = JSONObject.parseObject(versionStr).getJSONArray("versions").toJavaList(String.class)
                .stream().filter(v -> v.startsWith("template@" + PluginConstant.TEMPLATE_VERSION))
                .sorted(Comparator.comparing(String::toString).reversed())
                .collect(Collectors.toList()).get(0);
        String filesStr = HttpRequests.request(PluginConstant.JS_DELIVR_ENDPOINTS + "@" + version).readString();
        saveFile(JSONObject.parseObject(filesStr).getJSONArray("files"), version, "", filePath);
    }

    private static void saveFile(JSONArray fileArray, String version, String path, String filePath) throws IOException {
        for (int i = 0; i < fileArray.size(); i++) {
            JSONObject fileObject = fileArray.getJSONObject(i);
            String name = fileObject.getString("name");
            if (fileObject.getString("type").equals("directory")) {
                saveFile(fileObject.getJSONArray("files"), version, path + name + "/", filePath);
            } else {
                String value = HttpRequests.request(PluginConstant.CDN + version + "/" + path + name).readString();
                FileUtils.saveFile(new File(filePath + path + name), value);
            }
        }
    }
}
