name := "sbt-dropbox-plugin"

organization := "org.scala-sbt"

version := "0.0.1"

scalacOptions += "-deprecation"

publishMavenStyle := false

publishTo <<= (version) { version: String =>
    val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
    val (name, url) = if (version.contains("-"))
                        ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                      else
                        ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
    Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers += "sbt-dropbox-plugin" at "http://jberkel.github.com/sbt-dropbox-plugin/releases"

libraryDependencies ++= Seq(
  "com.dropbox" % "dropbox-java-sdk" % "1.3"
)

sbtPlugin := true
