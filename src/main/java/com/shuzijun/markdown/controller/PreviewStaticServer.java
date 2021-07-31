package com.shuzijun.markdown.controller;

import com.intellij.openapi.diagnostic.Logger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.ide.HttpRequestHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author shuzijun
 */
public class PreviewStaticServer extends HttpRequestHandler {

    private static final Logger LOG = Logger.getInstance(PreviewStaticServer.class);

    public static final String PREFIX = "/d5762d7c-840b-4d0b-bd5d-639850d2b4e2/";

    public static final Map<String, BaseController> route = new HashMap<>();
    static {
        new AssetsController().addRoute(route);
        new MarkdownFileController().addRoute(route);
        new ResourcesController().addRoute(route);
        new UploadFileController().addRoute(route);
        new ExportFileController().addRoute(route);
    }

    public static PreviewStaticServer getInstance() {
        return HttpRequestHandler.Companion.getEP_NAME().findExtension(PreviewStaticServer.class);
    }

    @Override
    public boolean isAccessible(@NotNull HttpRequest request) {
        return true;
    }

    @Override
    public boolean isSupported(@NotNull FullHttpRequest request) {
        return (request.method() == HttpMethod.GET || request.method() == HttpMethod.HEAD || request.method() == HttpMethod.POST) && request.uri().startsWith(PREFIX);
    }

    @Override
    public boolean process(@NotNull QueryStringDecoder urlDecoder,
                           @NotNull FullHttpRequest request,
                           @NotNull ChannelHandlerContext context) throws IOException {
        final String path = urlDecoder.path();
        if (!path.startsWith(PREFIX)) {
            throw new IllegalStateException("prefix should have been checked by #isSupported");
        }

        for (String controllerPath : route.keySet()) {
            if (path.startsWith(controllerPath)) {
                route.get(controllerPath).process(urlDecoder, request, context);
                return true;
            }
        }
        return false;
    }


}