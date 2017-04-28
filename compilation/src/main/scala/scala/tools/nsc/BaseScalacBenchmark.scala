package scala.tools.nsc

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import scala.collection.JavaConverters._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.annotations.Mode._

@State(Scope.Benchmark)
class BaseScalacBenchmark {
  @Param(value = Array[String]())
  var source: String = _

  @Param(value = Array(""))
  var extraArgs: String = _

  var driver: Driver = _

  var compilerArgs = _

  // MainClass is copy-pasted from compiler for source compatibility with 2.10.x - 2.13.x
  class MainClass extends Driver with EvalLoop {
    def resident(compiler: Global): Unit = loop { line =>
      val command = new CompilerCommand(line split "\\s+" toList, new Settings(scalacError))
      compiler.reporter.reset()
      new compiler.Run() compile command.files
    }

    override def newCompiler(): Global = Global(settings, reporter)

    override protected def processSettingsHook(): Boolean = {
      settings.usejavacp.value = true
      settings.outdir.value = tempDir.getAbsolutePath
      settings.nowarn.value = true
      if (extraArgs != null && extraArgs != "")
        settings.processArgumentString(extraArgs)
      true
    }

    compilerArgs =
      if (source.startsWith("@")) {
        Array(source)
      }
      else {
        val allFiles = Files.walk(findSourceDir).collect(Collectors.toList[Path]).asScala.toList
        allFiles.filter(_.getFileName.toString.endsWith(".scala")).map(_.toAbsolutePath.toString).toArray
      }

    driver = new MainClass
  }

  protected def compile(): Unit = {
    driver.process(compilerArgs)
    assert(!driver.reporter.hasErrors) // TODO: Remove
  }

  private var tempDir: File = null

  @Setup(Level.Trial)
  def initTemp(): Unit = {
    val tempFile = java.io.File.createTempFile("output", "")
    tempFile.delete()
    tempFile.mkdir()
    tempDir = tempFile
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

  private def findSourceDir: Path = {
    val path = Paths.get("../corpus/" + source)
    if (Files.exists(path)) path
    else Paths.get(source)
  }
}





