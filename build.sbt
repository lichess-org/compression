val commonSettings = Seq(
  scalacOptions := Seq(
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-Wunused:all",
    "-feature",
    "-language:postfixOps",
    "-indent",
    "-rewrite",
    "-source:future-migration",
    "-release:21"
  )
)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    scalaVersion := "3.7.0",
    name         := "compression",
    organization := "org.lichess",
    version      := "3.1.1",
    resolvers += "lila-maven".at("https://raw.githubusercontent.com/ornicar/lila-maven/master"),
    libraryDependencies += "org.specs2" %% "specs2-core" % "4.17.0" % Test
  )

lazy val benchmarks = (project in file("benchmarks"))
  .settings(
    commonSettings,
    name           := "compression-benchmarks",
    scalaVersion   := "3.7.0",
    publish / skip := true,
    libraryDependencies ++= Seq(
      "org.openjdk.jmh" % "jmh-core"                 % "1.37" % "compile",
      "org.openjdk.jmh" % "jmh-generator-annprocess" % "1.37" % "compile"
    )
  )
  .dependsOn(root)
  .enablePlugins(JmhPlugin)

publishTo := Some(Resolver.file("file", new File(sys.props.getOrElse("publishTo", ""))))
