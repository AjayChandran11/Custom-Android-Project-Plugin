import com.android.tools.idea.gradle.project.build.GradleBuildListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions

class CustomSDKProject: AnAction(){
    override fun actionPerformed(event: AnActionEvent) {
        ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_PROJECT).actionPerformed(event)
    }
}