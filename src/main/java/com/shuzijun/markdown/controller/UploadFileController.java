package com.shuzijun.markdown.controller;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.shuzijun.markdown.model.MarkdownResponse;
import com.shuzijun.markdown.model.PluginConstant;
import com.shuzijun.markdown.model.UploadResponse;
import com.shuzijun.markdown.ui.UploadFileDialogWrapper;
import com.shuzijun.markdown.util.FileUtils;
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
import java.util.concurrent.atomic.AtomicReference;

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
        Project project = getProject(projectNameParameter, projectUrlParameter);

        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
        try {
            UploadResponse.Data uploadResponseData = new UploadResponse.Data();
            for (InterfaceHttpData data : datas) {
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
                    FileUpload fileUpload = (FileUpload) data;
                    String fileName = fileUpload.getFilename();
                    if (PropertiesComponent.getInstance().getValue(PluginConstant.editorAssetsNameAutoKey,"Rename").equalsIgnoreCase("Timestamp")){
                        fileName = System.currentTimeMillis() + "_" + fileName;
                    }
                    if (fileUpload.isCompleted()) {
                        UploadFileDialogWrapper.FileSetting setting = getFileSetting(project, markdownFile, fileName);
                        if (setting.isOk()) {
                            File file = new File(setting.filePath());
                            fileUpload.renameTo(file);
                            FileUtils.refreshProjectDirectory(project, file.getPath());
                            VirtualFile toFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
                            uploadResponseData.addSuccMap(fileName,
                                    (setting.isRelativePath() ? FileUtil.getRelativePath(virtualFile.getParent().getPath(), toFile.getPath(), '/') : toFile.getPath())
                                            + (setting.isOverwrite() ? "?t=" + System.currentTimeMillis() : ""));
                        } else {
                            uploadResponseData.addErrFiles(fileName);
                        }
                    } else {
                        uploadResponseData.addErrFiles(fileName);
                    }
                }
            }
            return fillJsonResponse(UploadResponse.success(uploadResponseData).toString());
        } finally {
            decoder.destroy();
        }
    }

    public UploadFileDialogWrapper.FileSetting getFileSetting(Project project, File markdownFile, String filename) {
        AtomicReference<UploadFileDialogWrapper.FileSetting> atomicReference = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            String path = getFilePath(markdownFile);
            UploadFileDialogWrapper fileDialogWrapper = new UploadFileDialogWrapper(project, path, filename);
            if (fileDialogWrapper.showAndGet()) {
                atomicReference.set(fileDialogWrapper.getSetting());
            } else {
                atomicReference.set(new UploadFileDialogWrapper.FileSetting());
            }
        });
        return atomicReference.get();
    }

    private String getFilePath(File markdownFile){
        if (PropertiesComponent.getInstance().getBoolean(PluginConstant.editorAbsolutePathKey,false)){
            return PropertiesComponent.getInstance().getValue(PluginConstant.editorAssetsPathKey, "assets");
        } else {
            return markdownFile.getParent() + File.separator + PropertiesComponent.getInstance().getValue(PluginConstant.editorAssetsPathKey, "assets");
        }
    }
}
