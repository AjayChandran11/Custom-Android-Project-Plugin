package listeners

import utils.DirectoryImpl
import utils.File
import utils.FileType
import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker
import com.android.tools.idea.gradle.project.sync.GradleSyncListener
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyFileImpl

class CustomGradleSyncListener: GradleSyncListener {
    override fun syncSucceeded(project: Project) {
        super.syncSucceeded(project)
        if(PropertiesComponent.getInstance(project).getBoolean("isSdkLoaded")) return

        val projectSourceRoots: Array<VirtualFile> = ProjectRootManager.getInstance(project).contentSourceRoots

        val projectJavaPath = projectSourceRoots.filter {
            val pathTrimmed = it.path
            pathTrimmed.contains("src", true)
                    && !pathTrimmed.contains("test")
                    && !pathTrimmed.contains("build", true)
                    && !pathTrimmed.contains("res", true)
                    && !pathTrimmed.contains("androidTest", true)
        }[0]

        var packageName: String? = null
        var subDirectory: String? = null
        searchFiles(project, "MainActivity.kt").forEach { psiFile ->
            if(psiFile is KtFile){
                packageName = psiFile.packageFqName.toString()
                subDirectory = packageName?.replace(".", "/")
            }
        }

        val directory = PsiManager.getInstance(project).findDirectory(projectJavaPath)?.let {
            DirectoryImpl(project, it)
        }

        val file = File(
            "CustomSDKActivity",
            "package $packageName\n" +
                    "\n" +
                    "import androidx.appcompat.app.AppCompatActivity\n" +
                    "import android.os.Bundle\n" +
                    "\n" +
                    "class CustomSDKActivity : AppCompatActivity() {\n" +
                    "\n" +
                    "   override fun onCreate(savedInstanceState: Bundle?) {\n" +
                    "       super.onCreate(savedInstanceState)\n" +
                    "       //write your custom code here to be written while creating the project\n" +
                    "   }\n" +
                    "}\n", FileType.KOTLIN
        )

        WriteCommandAction.runWriteCommandAction(project) {
            var newSubDirectory = directory
            subDirectory?.split("/")?.forEach { segment ->
                newSubDirectory = newSubDirectory?.findSubdirectory(segment) ?: directory?.createSubdirectory(segment)
            }
            newSubDirectory?.addFile(file)
        }

        addMavenRepositoryToGradle(project)
        addDependencyToGradleFile(project)
        GradleBuildInvoker.getInstance(project).rebuild()

        PropertiesComponent.getInstance(project).setValue("isSdkLoaded", true)
    }

    private fun addDependencyToGradleFile(project: Project){
        searchFiles(project, "build.gradle").forEach { psiFile ->
            val appPath = psiFile.virtualFile.path.split("/").filter { it == "app" }
            if(psiFile is GroovyFileImpl && appPath.isNotEmpty()){
                psiFile.accept(object : GroovyRecursiveElementVisitor(){

                    override fun visitMethodCall(call: GrMethodCall) {
                        super.visitMethodCall(call)
                        call.firstChild.let {
                            if(it is GrReferenceExpression && it.qualifiedReferenceName == "dependencies"){
                                val elementFactory = GroovyPsiElementFactory.getInstance(project)
                                val dependency = elementFactory.createReferenceExpressionFromText("implementation", it.context)
                                dependency.add(elementFactory.createWhiteSpace())
                                dependency.add(elementFactory
                                    .createLiteralFromValue("//add your custom dependency"))

                                WriteCommandAction.runWriteCommandAction(project){
                                    call.lastChild.addBefore(dependency, call.lastChild.lastChild)
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    private fun addMavenRepositoryToGradle(project: Project){
        searchFiles(project, "settings.gradle").forEach { psiFile ->
            if(psiFile is GroovyFileImpl){
                psiFile.accept(object : GroovyRecursiveElementVisitor() {
                    override fun visitMethodCall(call: GrMethodCall) {
                        super.visitMethodCall(call)
                        call.firstChild.let {
                            if (it is GrReferenceExpression && it.qualifiedReferenceName == "repositories") {
                                val elementFactory = GroovyPsiElementFactory.getInstance(project)
                                val repository = elementFactory.createReferenceExpressionFromText("maven", it.context)
                                repository.add(elementFactory.createWhiteSpace())
                                repository.add(elementFactory.createBlockStatementFromText("{\n"
                                        +"url 'http://urltodownloaddependency.com'\n"
                                        +"allowInsecureProtocol = true\n"
                                        +"}", it.context))

                                WriteCommandAction.runWriteCommandAction(project){
                                    call.lastChild.addBefore(repository, call.lastChild.lastChild)
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    private fun searchFiles(project: Project, fileName: String): Array<PsiFile> =
        FilenameIndex.getFilesByName(project, fileName, GlobalSearchScope.allScope(project))
}