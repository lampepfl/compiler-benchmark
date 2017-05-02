package base

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

import org.openjdk.jmh.annotations._

import scala.collection.JavaConverters._
import scala.tools.nsc._
import java.io._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import dotty.tools.dotc.core.Contexts.ContextBase
import scala.collection.JavaConverters._
import org.openjdk.jmh.annotations._

import scala.bench.SimpleFileVisitor1
import scala.bench.SimpleFileVisitor1._

@State(Scope.Benchmark)
class BaseBenchmark {

  @Param(value = Array[String](""))
  var classPath: String = _

  @Param(value = Array[String](""))
  var dottyVersion: String = _

  @Param(value = Array[String](""))
  var source: String = _

  @Param(value = Array(""))
  var extraArgs: String = _

  var compilerArgs: Array[String] = _

  protected def compileDotc(): Unit = {
    val cp = classPath

    implicit val ctx = (new ContextBase).initialCtx.fresh
    ctx.setSetting(ctx.settings.classpath, cp)
    ctx.setSetting(ctx.settings.usejavacp, true)
    ctx.setSetting(ctx.settings.d, tempOutDir.getAbsolutePath)
    if (source == "scalap")
      ctx.setSetting(ctx.settings.language, List("Scala2"))

    val reporter = dotty.tools.dotc.Bench.doCompile(new dotty.tools.dotc.Compiler, compilerArgs.toList)
    assert(!reporter.hasErrors)
  }

  @Setup(Level.Trial)
  def setup(): Unit = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempDir = tempFile

    compilerArgs =
      if (source.startsWith("@")) {
        Array(source)
      }
      else {
        val allFiles = Files
          .walk(findSourceDir)
          .collect(Collectors.toList[Path])
          .asScala
          .toList

        allFiles
          .filter(_.getFileName.toString.endsWith(".scala"))
          .map(_.toAbsolutePath.toString)
          .toArray
      }
  }

  @TearDown(Level.Trial)
  def clearTemp(): Unit = {
    val directory = tempDir.toPath
    Files.walkFileTree(directory, new SimpleFileVisitor1[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        Files.delete(dir)
        FileVisitResult.CONTINUE
      }
    })
  }

  private var tempDir: File = null

  private def tempOutDir: File = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempFile
  }

  private def findSourceDir: Path = {
    val path = Paths.get("../corpus/" + source)
    if (Files.exists(path)) path
    else Paths.get(source)
  }
}






