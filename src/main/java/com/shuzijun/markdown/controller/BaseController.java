package com.shuzijun.markdown.controller;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.Responses;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author shuzijun
 */
public abstract class BaseController {

    // every time the plugin starts up, assume resources could have been modified
    protected static final long LAST_MODIFIED = System.currentTimeMillis();

    public final void process(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
        FullHttpResponse response;
        if (request.method() == HttpMethod.POST) {
            response = post(urlDecoder, request, context);
        } else if (request.method() == HttpMethod.GET) {
            response = get(urlDecoder, request, context);
        } else {
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        }
        Responses.send(response, context.channel(), request);
        if (response.content() != Unpooled.EMPTY_BUFFER) {
            try {
                response.release();
            } catch (Exception ignore) {
            }
        }
    }

    public FullHttpResponse get(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("Default response".getBytes(StandardCharsets.UTF_8)));
    }

    public FullHttpResponse post(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer("Default response".getBytes(StandardCharsets.UTF_8)));
    }

    public abstract String getControllerPath();

    protected String getResourceName(QueryStringDecoder urlDecoder) {
        return urlDecoder.path().substring(PreviewStaticServer.PREFIX.length() + getControllerPath().length());
    }

    protected String getParameter(@NotNull QueryStringDecoder urlDecoder, @NotNull String parameter) {
        List<String> parameters = urlDecoder.parameters().get(parameter);
        if (parameters == null || parameters.size() != 1) {
            return null;
        }
        return URLDecoder.decode(parameters.get(0), StandardCharsets.UTF_8);
    }

    protected FullHttpResponse fillHtmlResponse(String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=5, private, must-revalidate");
        response.headers().set("Referrer-Policy", "no-referrer");
        return response;
    }

    protected FullHttpResponse fillJsonResponse(String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=5, private, must-revalidate");
        response.headers().set("Referrer-Policy", "no-referrer");
        return response;
    }

    public void addRoute(Map<String, BaseController> route) {
        route.put(PreviewStaticServer.PREFIX + getControllerPath(), this);
    }


}
