package com.codesteward.identity

import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import java.io.File

data class DetectedIdentity(
    val user: String,
    val project: String,
    val branch: String,
)

object IdentityDetector {

    fun detect(project: Project): DetectedIdentity {
        return DetectedIdentity(
            user = detectUser(project),
            project = detectProject(project),
            branch = detectBranch(project),
        )
    }

    fun detectUser(project: Project): String {
        val basePath = project.basePath ?: return System.getProperty("user.name", "")
        return runGit(basePath, "config", "user.email")
            ?: System.getProperty("user.name", "")
    }

    fun detectProject(project: Project): String {
        return project.basePath?.let { File(it).name } ?: ""
    }

    fun detectBranch(project: Project): String {
        val repos = GitRepositoryManager.getInstance(project).repositories
        val repo = repos.firstOrNull() ?: return "unknown"
        return repo.currentBranchName ?: "unknown"
    }

    private fun runGit(cwd: String, vararg args: String): String? {
        return try {
            val process = ProcessBuilder("git", *args)
                .directory(File(cwd))
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            if (exitCode == 0 && output.isNotEmpty()) output else null
        } catch (_: Exception) {
            null
        }
    }
}
