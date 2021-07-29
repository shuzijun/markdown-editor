package com.shuzijun.markdown.controller;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtilRt;
import com.shuzijun.markdown.model.PluginConstant;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.FileResponses;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author shuzijun
 */
public class AssetsController extends BaseController {

    private static final Logger LOG = Logger.getInstance(AssetsController.class);

    private final String controllerPath = "assets";

    @Override
    public String getControllerPath() {
        return controllerPath;
    }

    @Override
    public FullHttpResponse get(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context) {
        String resourceName = getResourceName(urlDecoder).substring(1);
        byte[] data;
        try (InputStream inputStream = new FileInputStream(PluginConstant.TEMPLATE_PATH + resourceName)) {
            if (inputStream == null) {
                return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, Unpooled.EMPTY_BUFFER);
            }
            data = FileUtilRt.loadBytes(inputStream);
        } catch (IOException e) {
            LOG.warn(e);
            return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, Unpooled.EMPTY_BUFFER);
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(data));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileResponses.INSTANCE.getContentType(resourceName) + "; charset=utf-8");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=0, private, must-revalidate");
        response.headers().set(HttpHeaderNames.ETAG, Long.toString(LAST_MODIFIED));
        return response;
    }
}
