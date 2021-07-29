package com.shuzijun.markdown.controller;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuzijun.markdown.model.MarkdownResponse;
import com.shuzijun.markdown.model.UploadResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author shuzijun
 */
public class UploadFileController extends BaseController {

    private static final Logger LOG = Logger.getInstance(UploadFileController.class);

    private final String controllerPath = "uploadFile";

    @Override
    public String getControllerPath() {
        return controllerPath;
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
        File markdownFile = new File(fileParameter);
        if (!markdownFile.exists()) {
            return fillJsonResponse(MarkdownResponse.error("unable to to find file " + fileParameter).toString());
        }
        String assetsPath = markdownFile.getParent() + File.separator + "assets" + File.separator;
        File assetsFile = new File(assetsPath);
        if (!assetsFile.exists()) {
            assetsFile.mkdirs();
        }

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
        return fillJsonResponse(UploadResponse.success(uploadResponseData).toString());
    }
}
