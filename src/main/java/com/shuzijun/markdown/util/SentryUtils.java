package com.shuzijun.markdown.util;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.shuzijun.markdown.model.PluginConstant;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import io.sentry.context.Context;
import io.sentry.event.EventBuilder;
import io.sentry.event.helper.EventBuilderHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shuzijun
 */
public class SentryUtils {

    public static void submitErrorReport(Throwable error, String description) {

        final SentryClient sentry = SentryClientFactory.sentryClient("https://d1fa540b8bd047beb4e0ac3f66ad50f9@o291873.ingest.sentry.io/5875324");

        final ApplicationInfoImpl applicationInfo = (ApplicationInfoImpl) ApplicationInfo.getInstance();

        EventBuilderHelper eventBuilder = new EventBuilderHelper() {
            @Override
            public void helpBuildingEvent(EventBuilder eventBuilder) {
                final Map<String, Object> os = new HashMap<>();
                os.put("name", SystemInfo.OS_NAME);
                os.put("version", SystemInfo.OS_VERSION);
                os.put("kernel_version", SystemInfo.OS_ARCH);

                final Map<String, Object> runtime = new HashMap<>();
                final String ideName = applicationInfo.getBuild().getProductCode();
                runtime.put("name", ideName);
                runtime.put("version", applicationInfo.getFullVersion());

                final Map<String, Map<String, Object>> contexts = new HashMap<>();
                contexts.put("os", os);
                contexts.put("runtime", runtime);

                eventBuilder.withContexts(contexts);

                if (!StringUtil.isEmptyOrSpaces(description)) {
                    eventBuilder.withMessage(description);
                    eventBuilder.withTag("with-description", "true");
                }
            }
        };

        sentry.addBuilderHelper(eventBuilder);

        final Context context = sentry.getContext();


        context.addTag("javaVersion", SystemInfo.JAVA_RUNTIME_VERSION);
        context.addTag("pluginVersion",  PluginManagerCore.getPlugin(PluginId.getId(PluginConstant.PLUGIN_ID)).getVersion());
        if(error == null){
            sentry.sendMessage(description);
        }else {
            sentry.sendException(error);
        }

    }

}
