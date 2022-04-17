lazy val compression = Project("compression", file("."))
scalaVersion := "3.1.2"
name         := "compression"
organization := "org.lichess"
version      := "1.7"
resolvers += "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"
libraryDependencies += "org.specs2" %% "specs2-core" % "4.15.0" % Test
scalacOptions := Seq(
  "-encoding",
  "utf-8",
  "-rewrite",
  "-source:future-migration",
  "-indent",
  "-explaintypes",
  "-feature",
  "-language:postfixOps"
  // Warnings as errors!
  // "-Xfatal-warnings",
)
publishTo := Some(Resolver.file("file", new File(sys.props.getOrElse("publishTo", ""))))
