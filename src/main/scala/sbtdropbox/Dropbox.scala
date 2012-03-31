package sbtdropbox

import sbt._
import sbt.Keys._

object Dropbox {
  import DropboxKeys._

  object DropboxKeys {
      import com.dropbox.client2.session.{AccessTokenPair, AppKeyPair}

      val dropboxUpload = TaskKey[Unit]("dropbox-upload")
      val dropboxDelete = TaskKey[Unit]("dropbox-delete")
      val dropboxList   = TaskKey[Unit]("dropbox-list")

      val uploadFile   = TaskKey[File]("upload-file")
      val uploadFolder = SettingKey[String]("upload-folder")
      val deletePath   = TaskKey[String]("delete-path")
      val dropboxApi   = TaskKey[DropboxAPI]("dropbox-api")
      val dropboxToken = TaskKey[AccessTokenPair]("dropbox-access-token")
      val dropboxAppKey = SettingKey[AppKeyPair]("dropbox-appkey")
      val dropboxConfig = SettingKey[File]("dropbox-config")
  }

  lazy val settings = Seq(
    dropboxUpload <<= (streams, dropboxApi, uploadFile, uploadFolder) map { (s, api, file, folder) =>
      if (file == null || !file.exists) sys.error("file "+file+" does not exist")
      if (folder.isEmpty) sys.error("upload folder not set")

      val level = Level.Debug
      s.log.info("uploading "+file+" to folder "+folder)
      api.createFolder(folder)
      api.upload(folder+"/"+file.getName, file) { (b, t) =>
        if (level == Level.Debug)
          print(".")
      }
      if (level == Level.Debug) println()
      s.log.success("uploaded file "+file)
      ()
    },

    dropboxDelete <<= (streams, dropboxApi, deletePath) map { (s, api, path) =>
       if (path.isEmpty) sys.error("deletePath not set")
       api.delete(path)
    },

    dropboxList <<= (streams, dropboxApi, uploadFolder) map { (s, api, folder) =>
      for (e <- api.list(folder)) {
        s.log.info(String.format("%s%s (%s) %s", e.root, e.path, e.size, e.modified))
      }
    },

    dropboxToken <<= (dropboxAppKey, dropboxConfig) map (DropboxAPI.obtainToken(_)(_)),
    dropboxApi <<= (dropboxAppKey, dropboxToken) map (new DropboxAPI(_, _)),

    uploadFile := null,
    deletePath := "",
    uploadFolder := "",
    dropboxConfig := new File(System.getProperty("user.home"), ".sbt-dropbox-plugin"),
    dropboxAppKey := new com.dropbox.client2.session.AppKeyPair("3b02x88oi3bpa2x", "mwnxdopvusqxdyy")
  )
}
