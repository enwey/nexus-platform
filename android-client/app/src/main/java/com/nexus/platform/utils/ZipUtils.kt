package com.nexus.platform.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipUtils {

    fun unzip(zipFile: File, destDir: File) {
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(8192)

            while (entry != null) {
                val destFile = File(destDir, entry.name)

                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    
                    FileOutputStream(destFile).use { fos ->
                        var len: Int
                        while (zis.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun zip(sourceDir: File, zipFile: File) {
        if (!sourceDir.exists()) {
            throw FileNotFoundException("源目录不存在: ${sourceDir.absolutePath}")
        }

        zipFile.parentFile?.mkdirs()

        FileOutputStream(zipFile).use { fos ->
            java.util.zip.ZipOutputStream(fos).use { zos ->
                sourceDir.walkTopDown().forEach { file ->
                    val entryPath = sourceDir.toPath().relativize(file.toPath()).toString()
                    
                    if (file.isDirectory) {
                        val entry = java.util.zip.ZipEntry("$entryPath/")
                        zos.putNextEntry(entry)
                        zos.closeEntry()
                    } else {
                        val entry = java.util.zip.ZipEntry(entryPath)
                        zos.putNextEntry(entry)
                        file.inputStream().use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }
}
