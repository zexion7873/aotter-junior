package registered

import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.UpdateResult
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.kotlin.ext.sql.*
import io.vertx.kotlin.redis.*
import io.vertx.redis.RedisClient
import io.vertx.redis.RedisOptions
import kotlinx.coroutines.launch as launch1


class App : CoroutineVerticle() {

  private lateinit var client: JDBCClient
  private lateinit var redisClient: RedisClient
  private val host = "127.0.0.1"
  private val redisPort = 7000
  private val mysqlPort = 6999
  private val dbName = "test"
  private val mysqlUser = "root"
  private val mysqlPwd = "1234"
  private val userNames = "userNames"


  override suspend fun start() {

    client = JDBCClient.createShared(vertx, json {
      obj(
        "url" to "jdbc:mysql://$host:$mysqlPort/$dbName",
        "user" to mysqlUser,
        "password" to mysqlPwd,
        "driver_class" to "com.mysql.cj.jdbc.Driver",
        "max_pool_size-loop" to 30
      )
    })

    val options = RedisOptions().setHost(host).setPort(redisPort)
    redisClient = RedisClient.create(vertx, options)

    // 將mysql 中的 user_name 撈出至 Redis 不重複集合中
    val resultSet: ResultSet = client.queryAwait("select user_name from account")
    val userNameList= resultSet.rows.map{jsonObject -> jsonObject.getString("user_name")}.toList()
    if(userNameList.isNotEmpty()) {
      redisClient.saddManyAwait(userNames , userNameList)
    }

    // Build Vert.x Web router
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.post("/api/user").coroutineHandler { ctx -> register(ctx) }

    // Start the server
    vertx.createHttpServer()
      .requestHandler(router)
      .listenAwait(config.getInteger("http.port", 8080))


  }

  /**
   * 凱哥註冊用
   */
  private suspend fun register(ctx: RoutingContext) {

    var userName = ctx.bodyAsJson.getString("username").trim()

    // 判斷輸入username是否為空
    if ("".equals(userName)) {
      ctx.response().setStatusCode(400).end(json {
        obj("message" to "username不可為空").encode()
      })
    }

    // 嘗試將username加入userNames不重複集合中，若已重複則使用incr增加計數值，再次重新加入直至成功
    var updateCount = redisClient.saddAwait(userNames, userName)

    // 若username已重複，重複此迴圈直至成功
    while(updateCount == 0L) {

      //  使用incr計數器增加計數值，再加入至username後綴字
      var count : Long = redisClient.incrAwait(userName)
      userName = "$userName$count"
      updateCount = redisClient.saddAwait(userNames, userName)

    }

    // 將userName寫入account table中
    client.getConnectionAwait().use { connection ->
      val updateResult: UpdateResult =
        connection.updateWithParamsAwait("insert into account (user_name) values(?)", json { array(userName) })

      // 若insert成功，回傳states code 200 ，並顯示新增之userId, username
      val id: Int = updateResult.keys.getInteger(0)
      ctx.response().setStatusCode(200).end(json {
        obj("userId" to id, "username" to userName).encode()
      })

    }

  }

  /**
   * An extension method for simplifying coroutines usage with Vert.x Web routers
   */
  fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
    handler { ctx ->
      launch1(ctx.vertx().dispatcher()) {
        try {
          fn(ctx)
        } catch (e: Exception) {
          ctx.fail(e)
        }
      }
    }
  }


}
