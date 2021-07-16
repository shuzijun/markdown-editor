package com.shuzijun.markdown.editor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.shuzijun.markdown.model.MarkdownResponse;
import com.shuzijun.markdown.model.UploadResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.BuiltInServerManager;
import org.jetbrains.ide.HttpRequestHandler;
import org.jetbrains.io.FileResponses;
import org.jetbrains.io.Responses;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author shuzijun
 */
public class PreviewStaticServer extends HttpRequestHandler {

    private static final Logger LOG = Logger.getInstance(PreviewStaticServer.class);

    private static final String PREFIX = "/d5762d7c-840b-4d0b-bd5d-639850d2b4e2/";

    // every time the plugin starts up, assume resources could have been modified
    private static final long LAST_MODIFIED = System.currentTimeMillis();

    public static PreviewStaticServer getInstance() {
        return HttpRequestHandler.Companion.getEP_NAME().findExtension(PreviewStaticServer.class);
    }


    @NotNull
    private static String getStaticUrl(@NotNull String staticPath) {
        Url url = Urls.parseEncoded("http://localhost:" + BuiltInServerManager.getInstance().getPort() + PREFIX + staticPath);
        return BuiltInServerManager.getInstance().addAuthToken(Objects.requireNonNull(url)).toExternalForm();
    }

    public static Url getFileUrl(Project project, VirtualFile file) {
        Url url;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("http://localhost:").append(BuiltInServerManager.getInstance().getPort()).append(PREFIX);
            if (file instanceof LightVirtualFile) {
                throw new IllegalStateException("unable to create a URL from a in-memory file");
            }
            sb.append("source?file=").append(file.getPath());
            if (project.getPresentableUrl() != null) {
                sb.append("&projectUrl=").append(URLEncoder.encode(project.getPresentableUrl(), StandardCharsets.UTF_8.toString()));
            } else {
                sb.append("&projectName=").append(URLEncoder.encode(project.getName(), StandardCharsets.UTF_8.toString()));
            }
            url = Urls.parseEncoded(sb.toString());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("can't encode");
        }
        return url;
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

        final String payLoad = path.substring(PREFIX.length());

        if (payLoad.startsWith("resources")) {
            sendResource(request,
                    context.channel(),
                    payLoad.substring("resources".length()));
        } else if (payLoad.startsWith("markdownFile")) {
            String fileParameter = getParameter(urlDecoder, "file");
            String projectNameParameter = getParameter(urlDecoder, "projectName");
            String projectUrlParameter = getParameter(urlDecoder, "projectUrl");
            VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(fileParameter);
            if (virtualFile == null) {
                sendJson(request, context.channel(), MarkdownResponse.error("unable to to find file " + fileParameter).toString());
                return true;
            }
            Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () -> FileDocumentManager.getInstance().getDocument(virtualFile));
            if (request.method() == HttpMethod.POST) {
                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
                InterfaceHttpData valueData = decoder.getBodyHttpData("value");
                if (valueData != null && valueData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    String value = ((Attribute) valueData).getValue();
                    ApplicationManager.getApplication().invokeLaterOnWriteThread((() -> {
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            document.setText(value);
                            FileDocumentManager.getInstance().saveDocument(document);
                            sendJson(request, context.channel(), MarkdownResponse.success("").toString());
                        });
                    }));
                } else {
                    sendJson(request, context.channel(), MarkdownResponse.error("The requested content is not supported").toString());
                }
            } else {
                sendHtml(request, context.channel(), document.getText());
            }
            return true;
        } else if (payLoad.startsWith("uploadFile")) {
            String fileParameter = getParameter(urlDecoder, "file");
            String projectNameParameter = getParameter(urlDecoder, "projectName");
            String projectUrlParameter = getParameter(urlDecoder, "projectUrl");
            File markdownFile = new File(fileParameter);
            if (!markdownFile.exists()) {
                sendJson(request, context.channel(), UploadResponse.error("unable to to find file " + fileParameter).toString());
                return true;
            }
            String assetsPath = markdownFile.getParent() + File.separator + "assets" + File.separator;
            File assetsFile = new File(assetsPath);
            if (!assetsFile.exists()) {
                assetsFile.mkdirs();
            }

            if (request.method() == HttpMethod.POST) {
                HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
                List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
                UploadResponse.Data uploadResponseData = new UploadResponse.Data();
                for (InterfaceHttpData data : datas) {
                    if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                        FileUpload fileUpload = (FileUpload) data;
                        String fileName = fileUpload.getFilename();
                        if (fileUpload.isCompleted()) {
                            String newFileName = fileName;
                            File file = new File(assetsPath + newFileName);
                            if (file.exists()) {
                                newFileName = System.currentTimeMillis() + "-" + newFileName;
                                file = new File(assetsPath + newFileName);
                            }
                            fileUpload.renameTo(file);
                            uploadResponseData.addSuccMap(fileName, "./assets/" + newFileName);
                        } else {
                            uploadResponseData.addErrFiles(fileName);
                        }
                    }
                }
                sendJson(request, context.channel(), UploadResponse.success(uploadResponseData).toString());
            } else {
                sendJson(request, context.channel(), UploadResponse.error("The requested content is not supported").toString());
            }
            return true;
        } else {
            return false;
        }

        return true;
    }

    @Nullable
    private String getParameter(@NotNull QueryStringDecoder urlDecoder, @NotNull String parameter) {
        List<String> parameters = urlDecoder.parameters().get(parameter);
        if (parameters == null || parameters.size() != 1) {
            return null;
        }
        return parameters.get(0);
    }


    private static void sendJson(@NotNull HttpRequest request,
                                 @NotNull Channel channel, String markdownResponse) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(markdownResponse.getBytes(StandardCharsets.UTF_8)));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=5, private, must-revalidate");
        response.headers().set("Referrer-Policy", "no-referrer");
        Responses.send(response, channel, request);
    }

    private static void sendHtml(@NotNull HttpRequest request,
                                 @NotNull Channel channel, String markdownResponse) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(markdownResponse.getBytes(StandardCharsets.UTF_8)));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=5, private, must-revalidate");
        response.headers().set("Referrer-Policy", "no-referrer");
        Responses.send(response, channel, request);
    }

    private static void sendResource(@NotNull HttpRequest request,
                                     @NotNull Channel channel,
                                     @NotNull String resourceName) {

        byte[] data;
        try (InputStream inputStream = PreviewStaticServer.class.getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                Responses.send(HttpResponseStatus.NOT_FOUND, channel, request);
                return;
            }

            data = FileUtilRt.loadBytes(inputStream);
        } catch (IOException e) {
            LOG.warn(e);
            Responses.send(HttpResponseStatus.INTERNAL_SERVER_ERROR, channel, request);
            return;
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(data));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileResponses.INSTANCE.getContentType(resourceName) + "; charset=utf-8");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=3600, private, must-revalidate");
        response.headers().set(HttpHeaderNames.ETAG, Long.toString(LAST_MODIFIED));
        Responses.send(response, channel, request);
    }

}