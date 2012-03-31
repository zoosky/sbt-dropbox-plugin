package sbtdropbox

import com.dropbox.client2._
import com.dropbox.client2.session._
import com.dropbox.client2.exception._
import com.dropbox.client2.DropboxAPI.Entry
import java.util.Properties
import java.io.{File, FileOutputStream, FileInputStream}
import java.awt.Desktop
import collection.JavaConverters._

class DropboxAPI(appKey: AppKeyPair, token: AccessTokenPair) {
  type DApi = com.dropbox.client2.DropboxAPI[WebAuthSession]

  lazy val session = new WebAuthSession(appKey, Session.AccessType.APP_FOLDER, token)
  lazy val api:DApi = new com.dropbox.client2.DropboxAPI(session)

  def createFolder(path: String, ignoreExisting: Boolean = true):Option[Entry] = {
    val codes = if (ignoreExisting) Seq(403) else Seq.empty

    tryOperation(codes:_*)(api.createFolder(path))
  }

  def tryOperation[T](ignoreCodes: Int*)(operation:  => T):Option[T] = {
    try {
      Some(operation)
    } catch {
      case e: DropboxServerException if ignoreCodes.contains(e.error) => None
    }
  }

  def list(path: String):List[Entry] = {
    api.metadata(if (path.startsWith("/")) path else "/"+path, 0, null, true, null).contents.asScala.toList
  }

  def upload(path: String, file: File)(listener: ((Long, Long) => Unit) = null) = {
    api.putFile(path, new FileInputStream(file), file.length, null, new ProgressListener {
      override def onProgress(bytes: Long, total: Long) { if (listener != null) listener(bytes, total) }
    })
  }

  def delete(path: String) = {
    tryOperation(404)(api.delete(path))
  }

  def accountInfo = api.accountInfo()
}

object DropboxAPI {
  val accessTokenKey = "accessTokenPair.key"
  val accessTokenSecret = "accessTokenPair.secret"

  def loadProps(implicit config: File):Properties = {
    val props = new Properties
    if (config.exists()) props.load(new FileInputStream(config))
    props
  }

  def storeToken(token: AccessTokenPair)(implicit config: File) = {
    val props = loadProps(config)
    props.setProperty(accessTokenKey, token.key)
    props.setProperty(accessTokenSecret, token.secret)
    props.store(new FileOutputStream(config), null)
    token
  }

  def loadToken(implicit config: File):Option[AccessTokenPair] = {
    val props = loadProps(config)
    val (key, secret) = (props.getProperty(accessTokenKey), props.getProperty(accessTokenSecret))
    if (key != null && secret != null)
      Some(new AccessTokenPair(key, secret))
    else
      None
  }

  def obtainToken(appKey: AppKeyPair)(implicit config: File):AccessTokenPair = loadToken.getOrElse(storeToken(linkAccount(appKey)))

  def linkAccount(appKey: AppKeyPair):AccessTokenPair = {
      val was = new WebAuthSession(appKey, Session.AccessType.APP_FOLDER);
      val info = was.getAuthInfo
      if (!openUrl(info.url))
        println("Go to: " + info.url)

      println("Allow access to this app and press ENTER")
      while (System.in.read() != '\n') {}

      // This will fail if the user did not allow the app
      val uid = was.retrieveWebAccessToken(info.requestTokenPair)
      val accessToken = was.getAccessTokenPair
      accessToken
  }

  def openUrl(url: String):Boolean = {
    if (!Desktop.isDesktopSupported()) {
      false
    } else {
      val desktop = Desktop.getDesktop()
      if (!desktop.isSupported(Desktop.Action.BROWSE)) {
        false
      } else {
         desktop.browse(new java.net.URI(url))
         true
      }
    }
  }
}
