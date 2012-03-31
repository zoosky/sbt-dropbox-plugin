import sbt._
import Keys._
import com.dropbox.client2.session.{AccessTokenPair, AppKeyPair}

object Dropbox {

  val dropboxUpload = TaskKey[Unit]("dropbox-upload")
  val dropboxDelete = TaskKey[Unit]("dropbox-delete")
  val dropboxList   = TaskKey[Unit]("dropbox-list")

  val uploadFile   = TaskKey[File]("upload-file")
  val deletePath   = TaskKey[String]("delete-path")
  val uploadFolder = SettingKey[String]("upload-folder")
  val dropboxApi   = TaskKey[DropboxAPI]("dropbox-api")
  val dropboxToken = TaskKey[AccessTokenPair]("dropbox-access-token")
  val dropboxConfig = SettingKey[File]("dropbox-config")
  val dropboxAppKey = SettingKey[AppKeyPair]("dropbox-appkey")

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
    dropboxAppKey := new AppKeyPair("7d20vctta697nbi", "a2i52ej60tq1j9y")
  )
}
