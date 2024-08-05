DbTools 說明文件

## 使用環境 (必要)

- 1. grails：5.3.2
- 2. jdk: OpenJDK 17 [下載連結](https://adoptium.net/temurin/releases/?version=17)

## 進入網址 (必要)

    {project}/query/index

## 設定資料庫資訊 (必要)

    1. 設定 DB jdbc plugin

    // build.gradle
    MsSQL:
        // https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
        implementation group: 'com.microsoft.sqlserver', name: 'mssql-jdbc', version: '9.5.0.jre17-preview'

    PostgreDB:
        // https://mvnrepository.com/artifact/org.postgresql/postgresql
        implementation group: "org.postgresql", name: "postgresql", version: "42.6.0"
        implementation "org.grails.plugins:postgresql-extensions:6.1.0"
        implementation "org.hibernate:hibernate-spatial:5.6.15.Final"

    Oracle:
        // https://mvnrepository.com/artifact/com.oracle.database.jdbc/ojdbc11
        implementation group: 'com.oracle.database.jdbc', name: 'ojdbc11', version: '23.2.0.0'

    2. 資料庫設定, 參照 application.yml


## 設定參數資訊 (必要)

    1. 進 /grails-app/config/application.yml
    
    query:
        authorized: wzt:Aa13185141..
