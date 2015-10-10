package com.github.ksoichiro.web.resource

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.gpars.GParsPool
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class TriremeBowerTask extends DefaultTask {
    static final String NAME = "triremeBower"
    WebResourceExtension extension
    PathResolver pathResolver

    TriremeBowerTask() {
        project.afterEvaluate {
            extension = project.extensions.webResource
            pathResolver = new PathResolver(project, extension)
            getInputs()
                .files(pathResolver.retrieveValidPaths(pathResolver.getSrcLess()))
                .property('bower', extension.bower)
                .property('version', WebResourceExtension.VERSION)
            getOutputs().files(new File(extension.workDir, 'bower_components'), getBowerScript())
        }
    }

    @TaskAction
    void exec() {
        if (!extension.bower) {
            return
        }

        File bowerComponentsDir = new File(extension.workDir, "bower_components")
        if (!bowerComponentsDir.exists()) {
            bowerComponentsDir.mkdirs()
        }

        // Ensure bower.json does not exist since it affects bower's installation.
        def rootBowerJson = new File(extension.workDir, 'bower.json')
        if (rootBowerJson.exists()) {
            project.delete(rootBowerJson)
        }

        List bowerConfig = []
        extension.bower.dependencies.each {
            File bowerJson = new File(extension.workDir, "bower_components/${it.name}/bower.json")
            if (bowerJson.exists()) {
                // already installed
                def pkg = new JsonSlurper().parseText(bowerJson.text)
                if (pkg.version) {
                    if (pkg.version != it.version) {
                        // should be updated, so remove it before install
                        project.delete("${extension.workDir}/bower_components/${it.name}")
                    }
                }
            }
            Map dependency = [name: it.name, version: it.version]
            if (it.cacheName) {
                dependency['cacheName'] = it.cacheName
            }
            bowerConfig.add(dependency)
        }
        def dependencies = bowerConfig.isEmpty() ? '[]'
            : JsonOutput.prettyPrint(JsonOutput.toJson(bowerConfig))
        new File(extension.workDir, 'bowerPackages.json').text = dependencies
        new File(extension.workDir, 'bower.js').text = getClass().getResourceAsStream('/bower.js').text

        def triremeNodeRunner = new TriremeNodeRunner(
            scriptName: 'bower.js',
            workingDir: extension.workDir)
        triremeNodeRunner.exec()
    }

    File getBowerScript() {
        new File(extension.workDir, 'bower.js')
    }
}
