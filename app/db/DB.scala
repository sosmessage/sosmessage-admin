package db

import com.mongodb.casbah.{MongoURI, MongoCollection, MongoConnection}
import conf.SosMessageConfig
import org.streum.configrity.Configuration

object DB {

  val SosMessageMongoUriParam = "SOS_MESSAGE_MONGO_URI"

  val DefaultMongoUri = "mongodb://localhost/sosmessage"

  lazy val db = {
    val env = Configuration.environment
    val uri = MongoURI(env(SosMessageMongoUriParam, DefaultMongoUri))
    val mongo = MongoConnection(uri)
    val db = mongo(uri.database.getOrElse("sosmessage"))
    uri.username.map(name =>
      db.authenticate(name, uri.password.getOrElse(Array("")).foldLeft("")(_ + _.toString))
    )
    db
  }

  def collection[T](name: String)(f: MongoCollection => T): T = f(db(name))

  def drop(name: String) {
    db(name).drop()
  }

}
