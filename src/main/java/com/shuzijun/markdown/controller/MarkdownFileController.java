package com.shuzijun.markdown.controller;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuzijun.markdown.model.MarkdownResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author shuzijun
 */
public class MarkdownFileController extends BaseController {

    private static final Logger LOG = Logger.getInstance(MarkdownFileController.class);

    private final String controllerPath = "markdownFile";

    @Override
    public String getControllerPath() {
        return controllerPath;
    }

    @Override
    public FullHttpResponse get(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context) {
        String fileParameter = getParameter(urlDecoder, "file");
        String projectNameParameter = getParameter(urlDecoder, "projectName");
        String projectUrlParameter = getParameter(urlDecoder, "projectUrl");

        FileApplicationService fileApplicationService = ApplicationManager.getApplication().getService(FileApplicationService.class);
        VirtualFile virtualFile = fileApplicationService.getVirtualFile(fileParameter, StringUtils.isNotBlank(projectUrlParameter) ? projectUrlParameter : projectNameParameter);
        if (virtualFile == null) {
            return fillHtmlResponse("unable to to find file " + fileParameter);
        }
        Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () -> FileDocumentManager.getInstance().getDocument(virtualFile));
        return fillHtmlResponse(document.getText());
    }

    @Override
    public FullHttpResponse post(@NotNull QueryStringDecoder urlDecoder, @NotNull FullHttpRequest request, @NotNull ChannelHandlerContext context) throws IOException {
        String fileParameter = getParameter(urlDecoder, "file");
        String projectNameParameter = getParameter(urlDecoder, "projectName");
        String projectUrlParameter = getParameter(urlDecoder, "projectUrl");
        FileApplicationService fileApplicationService = ApplicationManager.getApplication().getService(FileApplicationService.class);
        VirtualFile virtualFile = fileApplicationService.getVirtualFile(fileParameter, StringUtils.isNotBlank(projectUrlParameter) ? projectUrlParameter : projectNameParameter);
        if (virtualFile == null) {
            return fillJsonResponse(MarkdownResponse.error("unable to to find file " + fileParameter).toString());
        }
        Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () -> FileDocumentManager.getInstance().getDocument(virtualFile));
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        InterfaceHttpData valueData = decoder.getBodyHttpData("value");
        if (valueData != null && valueData.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
            String value = ((Attribute) valueData).getValue();
            CompletableFuture<FullHttpResponse> httpResponseFuture = new CompletableFuture<>();
            ApplicationManager.getApplication().invokeLaterOnWriteThread(() -> {
                try {
                    httpResponseFuture.complete(ApplicationManager.getApplication().runWriteAction(((ThrowableComputable<FullHttpResponse, Throwable>) () -> {
                        document.setText(value);
                        FileDocumentManager.getInstance().saveDocument(document);
                        return fillJsonResponse(MarkdownResponse.success("").toString());
                    })));
                } catch (Throwable t) {
                    httpResponseFuture.completeExceptionally(t);
                }
            });
            try {
                return httpResponseFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e);
                return fillJsonResponse(MarkdownResponse.error(e.getMessage()).toString());
            }
        } else {
            return fillJsonResponse(MarkdownResponse.error("The requested content is not supported").toString());
        }
    }
}
