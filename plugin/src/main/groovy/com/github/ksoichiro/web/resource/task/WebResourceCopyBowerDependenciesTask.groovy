package com.github.ksoichiro.web.resource.task

import com.github.ksoichiro.web.resource.util.PathResolver
import com.github.ksoichiro.web.resource.extension.WebResourceExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class WebResourceCopyBowerDependenciesTask extends DefaultTask {
    static String NAME = "webResourceCopyBowerDependencies"
    WebResourceExtension extension
    PathResolver pathResolver

    WebResourceCopyBowerDependenciesTask() {
        dependsOn([WebResourceInstallBowerDependenciesTask.NAME])
        project.afterEvaluate {
            extension = project.extensions.webResource
            pathResolver = new PathResolver(project, extension)
            getInputs()
                .dir(new File(extension.workDir, WebResourceInstallBowerDependenciesTask.BOWER_COMPONENTS_DIR))
                .property('bower', extension.bower)
                .property('version', WebResourceExtension.VERSION)
            getOutputs().files(pathResolver.retrieveValidPaths(pathResolver.getDestLib()))
        }
    }

    @TaskAction
    void exec() {
        // Remove old files first
        project.delete(project.file("${extension.base.dest}/${extension.lib.dest}").absolutePath)

        project.copy {
            from project.fileTree("${extension.workDir}/bower_components").matching {
                if (!extension.bower.dependencies.isEmpty()) {
                    extension.bower.dependencies.each { dependency ->
                        String[] expr = dependency.filter
                        if (expr) {
                            expr.each { e -> it.include("${dependency.name}/${e}") }
                        } else {
                            it.include("${dependency.name}/**/*")
                        }
                    }
                } else {
                    it.include("**/*")
                }
            }
            into "${extension.base.dest}/${extension.lib.dest}"
        }
    }
}