package sbtdropbox

import sbt._
import sbt.Keys._

import com.dropbox.client2.DropboxAPI.Entry

object Dropbox {
  import DropboxKeys._

  object DropboxKeys {
      import com.dropbox.client2.session.{AccessTokenPair, AppKeyPair}

      val dropboxUpload = TaskKey[Seq[Entry]]("dropbox-upload")
      val dropboxDelete = TaskKey[Unit]("dropbox-delete")
      val dropboxList   = TaskKey[Seq[Entry]]("dropbox-list")

      val uploadFiles  = TaskKey[Seq[File]]("upload-files")
      val uploadFolder = SettingKey[String]("upload-folder")
      val deletePath   = TaskKey[String]("delete-path")
      val dropboxApi   = TaskKey[DropboxAPI]("dropbox-api")
      val dropboxToken = TaskKey[AccessTokenPair]("dropbox-access-token")
      val dropboxAppKey = SettingKey[AppKeyPair]("dropbox-appkey")
      val dropboxConfig = SettingKey[File]("dropbox-config")
  }

  def uploadFile(api: DropboxAPI, folder: String, file: File, log: Logger, progress: Boolean = false) = {
      log.info("uploading "+file+" to folder "+folder)
      val start = System.currentTimeMillis
      val entry = api.upload(folder+"/"+file.getName, file) { (b, t) => if (progress) print(".") }
      if (progress) println()
      log.success("uploaded file %s in %.2f secs".format(file, (System.currentTimeMillis-start)/1000.0))
      entry
  }

  lazy val settings = Seq(
    dropboxUpload <<= (streams, dropboxApi, uploadFiles, uploadFolder) map { (s, api, files, folder) =>
      if (!files.isEmpty) {
        if (!folder.isEmpty) {
          api.createFolder(folder)
        }
        files.map(uploadFile(api, folder, _, s.log))
      } else {
        Seq.empty
      }
    },

    dropboxDelete <<= (streams, dropboxApi, deletePath) map { (s, api, path) =>
       if (path.isEmpty) sys.error("deletePath not set")
       api.delete(path)
    },

    dropboxList <<= (streams, dropboxApi, uploadFolder) map { (s, api, folder) =>
      val entries = api.list(folder)
      entries.foreach(e => s.log.info(String.format("%s%s (%s) %s", e.root, e.path, e.size, e.modified)))
      entries
    },

    dropboxToken <<= (dropboxAppKey, dropboxConfig) map (DropboxAPI.obtainToken(_)(_)),
    dropboxApi <<= (dropboxAppKey, dropboxToken) map (new DropboxAPI(_, _)),

    uploadFiles := Seq.empty,
    deletePath   := "",
    uploadFolder := "",
    dropboxConfig := new File(System.getProperty("user.home"), ".sbt-dropbox-plugin"),
    dropboxAppKey := new com.dropbox.client2.session.AppKeyPair("3b02x88oi3bpa2x", "mwnxdopvusqxdyy")
  )
}
