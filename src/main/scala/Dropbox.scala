import sbt._
import Keys._

object Dropbox {
  val uploadDropbox = TaskKey[Unit]("upload-dropbox")
  val uploadFile = TaskKey[File]("upload-file")
  val uploadFolder = SettingKey[String]("upload-folder")
  val dropboxApi  = TaskKey[DropboxAPI]("dropbox-api")

  lazy val settings = Seq(
    uploadDropbox <<= (streams, dropboxApi, uploadFile, uploadFolder) map { (s, api, file, folder) =>
      s.log.debug("uploading "+file+" to folder "+folder)
      api.createFolder(folder)
      api.upload(folder+"/"+file.getName, file) { (b, t) => }
      ()
    },

    dropboxApi <<= (streams) map { (s) =>
      new DropboxAPI(DropboxAPI.appKey, DropboxAPI.obtainToken)
    }
  )
}
