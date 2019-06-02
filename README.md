# 安裝說明
請先在本地端安裝 java8 及 docker ， 並至專案的 docker-compose 目錄下執行
```sh
$ docker-compose up -d
```
上面指令主要是利用 docker-compose.yaml 建置 Mysql 及 Redis 資料庫容器，其建置的通訊埠如下
- Mysql : 6999 port
- Redis : 7000 port

PS. 如有通訊埠衝突問題請自行更改 docker-compose.yaml 相關設定及 App.kt 中對應的通訊埠常數

# 專案目錄下包含
#### docker-compose
- docker-compose.yaml (建置容器用)
- mysql-init (初始化 script folder)

#### vertx-coroutines-registered
- main.kt (程式進入點)
- App.kt (主要業務邏輯)

# API

> curl -X POST http://localhost:8080/api/user
```json
{
    "username" : "Kevin"
}
```
