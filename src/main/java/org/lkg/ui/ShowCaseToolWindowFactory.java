package org.lkg.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.lkg.core.RequestService;
import org.lkg.pattern.UseCaseSubject;
import org.lkg.util.PromptHelper;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * @description: 展示用例的工厂
 * @author: 李开广
 * @date: 2023/5/5 10:21 AM
 */
public class ShowCaseToolWindowFactory implements ToolWindowFactory {

    private static final Logger log =  Logger.getLogger(ShowCaseToolWindowFactory.class.getSimpleName());

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CaseWindow showCaseToolWindow = new CaseWindow();

        UseCaseSubject.getInstance().addObserver(showCaseToolWindow);
        new Thread(() -> RequestService.submitWithPublicV2(PromptHelper.getSystemConversationForPreHeat(), true)).start();
        ContentFactory instance = ContentFactory.SERVICE.getInstance();
        Content content = instance.createContent(showCaseToolWindow.getGlobalPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.getContentManager().setSelectedContent(content);
        log.info("<<<<-----初始化工具窗口完毕------->>>");
    }
}
