package com.shuzijun.markdown.model;

import com.intellij.openapi.application.PathManager;

import java.io.File;

/**
 * @author shuzijun
 */
public class PluginConstant {

    public static final String PLUGIN_ID = "com.shuzijun.markdown-editor";

    public static final String NOTIFICATION_GROUP = "Markdown editor";
    public static final String APPLICATION_CONFIGURABLE_DISPLAY_NAME = "Markdown editor";

    public static final String TEMPLATE_VERSION = "1";
    public static final String TEMPLATE_PATH = PathManager.getPluginsPath() + File.separator + "markdown-editor" + File.separator + "assets" + File.separator;
}
