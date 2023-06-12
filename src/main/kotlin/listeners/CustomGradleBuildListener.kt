package listeners

import com.android.tools.idea.gradle.project.build.BuildContext
import com.android.tools.idea.gradle.project.build.BuildStatus
import com.android.tools.idea.gradle.project.build.GradleBuildListener
import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType

class CustomGradleBuildListener: GradleBuildListener {
    override fun buildExecutorCreated(request: GradleBuildInvoker.Request) {}

    override fun buildStarted(context: BuildContext) {}

    override fun buildFinished(status: BuildStatus, context: BuildContext?) {
        if(status == BuildStatus.FAILED){
            if (context != null) {
                if(PropertiesComponent.getInstance(context.project).getBoolean("isBuildStatusNotified")) return

                NotificationGroup("buildFailed", NotificationDisplayType.BALLOON).createNotification(
                    "Build Failed :",
                    "Check if you have missed something in your build.",
                    NotificationType.ERROR,
                    null
                ).notify(context.project)
                PropertiesComponent.getInstance(context.project).setValue("isBuildStatusNotified", true)
            }
        }
    }
}