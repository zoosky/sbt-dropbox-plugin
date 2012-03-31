import scala.sys.process.Process
import com.dropbox.client2._
import com.dropbox.client2.session._
import com.dropbox.client2.exception._
import java.util.Properties
import java.io.{File, FileOutputStream, FileInputStream}

class DropboxAPI(appKey: AppKeyPair, token: AccessTokenPair) {
  lazy val session = new WebAuthSession(appKey, Session.AccessType.APP_FOLDER, token)
  lazy val api:com.dropbox.client2.DropboxAPI[WebAuthSession] = new com.dropbox.client2.DropboxAPI(session)

  def createFolder(path: String, ignoreExisting: Boolean = true) = {
    try {
      api.createFolder(path)
    } catch {
      case e: DropboxServerException if e.error == 403 => if (!ignoreExisting) throw e
      case e: DropboxException => System.err.println(e)
    }
  }

  def list(path: String) = {
    val entries = api.metadata(path, 0, null, true, null)
    entries
  }

  def upload(path: String, file: File)(listener: (Long, Long) => Unit) = {
    api.putFile(path, new FileInputStream(file), file.length, null, new ProgressListener {
      override def onProgress(bytes: Long, total: Long) { listener(bytes, total) }
    })
  }

  def dumpFields(o: Object) = o.getClass.getDeclaredFields.map(f=>(f.getName, f.get(o))).foreach(println(_))

  def accountInfo = api.accountInfo()
}

object DropboxAPI {
  val appKey = new AppKeyPair("7d20vctta697nbi", "a2i52ej60tq1j9y")
  val config = new File(System.getProperty("user.home"), ".sbt-dropbox-plugin")
  val accessTokenKey = "accessTokenPair.key"
  val accessTokenSecret = "accessTokenPair.secret"

  def loadProps:Properties = {
    val props = new Properties
    if (config.exists()) props.load(new FileInputStream(config))
    props
  }

  def storeToken(token: AccessTokenPair) = {
    val props = loadProps
    props.setProperty(accessTokenKey, token.key)
    props.setProperty(accessTokenSecret, token.secret)
    props.store(new FileOutputStream(config), null)
    token
  }

  def loadToken:Option[AccessTokenPair] = {
    val props = loadProps
    val (key, secret) = (props.getProperty(accessTokenKey), props.getProperty(accessTokenSecret))
    if (key != null && secret != null)
      Some(new AccessTokenPair(key, secret))
    else
      None
  }

  def obtainToken:AccessTokenPair = loadToken.getOrElse(storeToken(linkAccount))
  def linkAccount:AccessTokenPair = {
      val was = new WebAuthSession(appKey, Session.AccessType.APP_FOLDER);
      val info = was.getAuthInfo
      println("1. Go to: " + info.url)
      println("2. Allow access to this app.")
      println("3. Press ENTER.")

      Process("open "+info.url) !

      while (System.in.read() != '\n') {}

      // This will fail if the user didn't visit the above URL and hit 'Allow'.
      val uid = was.retrieveWebAccessToken(info.requestTokenPair)
      val accessToken = was.getAccessTokenPair
      accessToken
  }

  def main(args: Array[String]) = {
    val api = new DropboxAPI(appKey, obtainToken)

    println(api.accountInfo.displayName)
    println(api.accountInfo.uid)

    val path = "/api-test"
    api.createFolder(path)
    val entries = api.list(path)
    api.dumpFields(entries)

    if (args.length > 0) {
      val file = new File(args(0))
      if (file.exists) {
        println("uploading "+file)
        api.upload(path+"/"+file.getName, file) { (bytes, total) =>
        }
      }
    }
    System.exit(0)
  }
}
