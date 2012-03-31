# Usage

Add the following to your `project/plugins.sbt`

```
resolvers ++= Seq(
  Resolver.url("scalasbt", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
  Resolver.url("dropbox sdk", new URL("http://jberkel.github.com/sbt-dropbox-plugin/releases"))
)

addSbtPlugin("org.scala-sbt" % "sbt-dropbox-plugin" % "0.0.1")
```

Then in your project build config:

```
import sbtdropbox.Dropbox
import sbtdropbox.Dropbox.DropboxKeys._

val myproject = Project("myproject", file("."), settings = Dropbox.settings ++ Seq(
  // your settings
  dropboxFiles  := Seq(new File(....)), // you can also reference tasks producing files here
  dropboxFolder := "someFolder",        // will get created if it does not exist, can be empty
  dropboxAppKey := ("key", "secret")    // obtain one from https://www.dropbox.com/developers
)
```

Then, from within sbt:

    $ sbt
    > dropbox-upload
    > dropbox-list

The first time you use the plugin it will open a browser window where you have to allow the plugin to access your dropbox. 
The token is cached in ~/.sbt/dropbox-plugin.

At the moment you'll have to generate an app key for the plugin yourself.
