import java.io.File

import spray.revolver.RevolverPlugin._

name := """zipkin_tracing_macro"""

fork in run := true

resolvers ++= Seq("Twitter Repo" at "http://maven.twttr.com/")

resolvers ++= Seq("Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

resolvers += "Mesosphere Public Repository" at "http://downloads.mesosphere.io/maven"

scalaVersion := "2.11.5"

val finagleVersion = "6.25.0"

organization := "com.rigon"

libraryDependencies ++= Seq(
  "com.twitter" %% "twitter-server" % "1.10.0",
  "com.twitter" % "finagle-core_2.11" % "6.2.0",
  "com.twitter" %% "finagle-http" % finagleVersion,
  "com.twitter" %% "finagle-mysql" % finagleVersion,
  ("com.twitter" %% "finagle-stats" % finagleVersion).exclude("asm", "asm"),
  "com.twitter" %% "finagle-zipkin" % finagleVersion,
  "org.slf4j" % "slf4j-log4j12" % "1.7.10"
)

// test

libraryDependencies ++= Seq(
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
  "org.codehaus.groovy" % "groovy-all" % "2.4.3" % "test"
)

assemblyJarName in assembly := s"${name.value}.jar"

parallelExecution in Test := true

testOptions in Test += Tests.Cleanup(() => {
  val files = new File("/tmp").listFiles().filter(_.getName.startsWith("search_api_"))
  def delete(file: File): Array[(String, Boolean)] = Option(file.listFiles).map(_.flatMap(delete)).getOrElse(Array()) :+ (file.getPath -> file.delete)
  files map delete
})

ScoverageSbtPlugin.ScoverageKeys.coverageMinimum := 65

ScoverageSbtPlugin.ScoverageKeys.coverageFailOnMinimum := true

addCommandAlias("test", "testQuick")

addCommandAlias("devrun", "~re-start")

addCommandAlias("cov", "; clean; coverage; test")

Revolver.settings


lazy val generatePropertiesTask = Def.task {
  val file = (resourceManaged in Compile).value / "build.properties"
  var contents = s"name=${name.value}"
  contents += s"\nversion=${version.value}"
  contents += s"\nbuild=${version.value}"
  contents += "\nscm_repository=https://github.com/andrerigon/zipkin_tracing_macro.git"
  contents += s"\nbuild_branch_name=" + Process("git rev-parse --abbrev-ref HEAD").lines.head.split("/").last
  contents += "\nbuild_revision=" + Process("git rev-parse HEAD").lines.head
  contents += "\nbuild_last_few_commits=" + s"${Process("git log --oneline -n 5").lines.map(_.split(" ").tail.mkString(" ")).mkString("\\n")}"
  IO.write(file, contents)
  Seq(file)
}

resourceGenerators in Compile += generatePropertiesTask.taskValue

mappings in(Compile, packageSrc) ++= {
  val allGeneratedFiles = ((resourceManaged in Compile).value ** "*") filter {
    _.isFile
  }
  allGeneratedFiles.get pair relativeTo((resourceManaged in Compile).value)
}

test in assembly := {}

assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
{
  case "META-INF/spring.tooling" => MergeStrategy.first
  case x => old(x)
}
}

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)

releaseSettings

publishTo := Some(Resolver.file("Local repo", Path.userHome / ".m2" / "repository" asFile ))
