# Usage

Add the following to your `project/plugins.sbt`

```scala
resolvers ++= Seq(
  Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
  "dropbox sdk" at "http://jberkel.github.com/sbt-dropbox-plugin/releases"
)

addSbtPlugin("org.scala-sbt" % "sbt-dropbox-plugin" % "0.1.0")
```

Then in your project build config:

```scala
import sbtdropbox.Dropbox
import sbtdropbox.Dropbox.DropboxKeys._

val myproject = Project("myproject", file("."), settings = Dropbox.settings ++ Seq(
  // your settings
  dropboxFiles  := Seq(new File(....)), // you can also reference tasks producing files here
  dropboxFolder := "someFolder",        // will get created if it does not exist, can be empty
  dropboxAppKey := ("key", "secret")    // obtain one from https://www.dropbox.com/developers
))
```

Then, from within sbt:

    $ sbt
    > dropbox-upload
    > dropbox-list

The first time you use the plugin it will open a browser window where you have to give the plugin permission to access
your dropbox.  The token is then cached in `~/.sbt-dropbox-plugin`.

At the moment you'll have to generate an app key for the plugin yourself - this can be done quite easily at
[dropbox.com/developers](https://www.dropbox.com/developers) (Go to "My Apps" - then "Create an App").
