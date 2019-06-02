package registered

import io.vertx.core.Vertx
import io.vertx.kotlin.core.deployVerticleAwait

suspend fun main() {
  val vertx = Vertx.vertx()
  try {
    vertx.deployVerticleAwait("registered.App")
    println("Application started")
  } catch (exception: Throwable) {
    println("Could not start application")
    exception.printStackTrace()
  }
}

