package query

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import net.sf.jsqlparser.expression.Alias
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.select.SetOperationList

import java.sql.DriverManager

class QueryService {

    private final String MSSQL_DRIVER = 'com.microsoft.sqlserver.jdbc.SQLServerDriver'
    private final String ORACLE_DRIVER = 'oracle.jdbc.OracleDriver'
    private final String POSTGRESQL_DRIVER = 'org.postgresql.Driver'
    private final String MARIADB_DRIVER = 'org.mariadb.jdbc.Driver'
    private final String MYSQL_DRIVER = 'com.mysql.jdbc.Driver'

    Sql cancelSql

    /***
     * 參數初始化
     * @param params
     * @return
     */
    ExecInfoObject execInfoObjectInit(params) {
        ExecInfoObject execInfoObject = new ExecInfoObject()
        execInfoObject.setParams(params)
        return execInfoObject
    }

    /***
     * 執行
     * @param params
     * @return
     */
    def execute(ExecInfoObject execInfoObject) {
        def result = [:]

        def execScript = execInfoObject.decodeExecTextArea()
        if (!execScript) {
            result.failed = "未輸入查詢script"
            return result
        }

        Sql sql = null
        try {
            sql = initSql(execInfoObject.decodeUrl(), execInfoObject.decodeUsername(), execInfoObject.decodePd())
            cancelSql = sql
        } catch (ex) {
            result.failed = ex
            return result
        }

        def isCommit = false
        try {
            if (execInfoObject.decodeDbType() != ORACLE_DRIVER) {
                sql?.connection?.autoCommit = false
            }
            isCommit = true
        } catch (ex) {
        }

        def executeCnts = -1
        try {
            executeCnts = sql.executeUpdate(execScript)
        } catch (ex) {
            result.failed = ex
            sql.rollback()
            return result
        } finally {
            if (isCommit) {
                try {
                    sql.commit()
                } catch (ex) {
                }
            }
            sql.close()
        }

        result.success = "成功執行 ${executeCnts} 數 "
        return result
    }

    /***
     * 停止SQL
     * @param params
     * @return
     */
    def stopExecute() {
        LinkedHashMap result = [:]

        try {
            cancelSql.rollback()
        } catch (Exception ex) {
            result.failed = ex.toString()
        } finally {
            try {
                cancelSql.close()
            } catch (Exception ex) {
                result.failed = ex.toString()
            }
        }

        return result
    }


    /***
     * 查詢
     * @param params
     * @return
     */
    def query(ExecInfoObject execInfoObject) {
        def result = [:]

        def queryScript = execInfoObject.decodeQueryTextArea()
        if (!queryScript) {
            result.failed = "未輸入查詢script"
            return result
        }
        def countScript
        try {
            countScript = buildCountScript(queryScript)
        } catch (ex) {
            result.failed = ex
            return result
        }
        // Oracle分號有問題, 讓 CCJSqlParserUtil 處理掉
        if (execInfoObject.decodeDbType() == ORACLE_DRIVER) {
            queryScript = CCJSqlParserUtil.parse(queryScript,
                    { parser ->
                        parser.withSquareBracketQuotation(true).withAllowComplexParsing(true)
                    }).toString()
        }

        Sql sql = null
        try {
            sql = initSql(execInfoObject.decodeUrl(), execInfoObject.decodeUsername(), execInfoObject.decodePd())
            cancelSql = sql
        } catch (ex) {
            result.failed = ex
            return result
        }

        try {
            //取得總筆數
            sql.withStatement { stmt ->
                stmt.fetchSize = 1
                stmt.maxRows = 1
            }
            execInfoObject.recordsTotal = sql.firstRow(countScript)[0]

            //取得欄位名稱
            def metaClosure = { metadata ->
                int columnCount = metadata.getColumnCount()
                ArrayList<String> columns = new ArrayList<>()
                for (int j = 1; j <= columnCount; j++) {
                    String columnName = metadata.getColumnName(j)
                    columns.add(columnName)
                    execInfoObject.recordsColumn.add(columnName)
                }
            }

            //取得資料結果
            sql.withStatement { stmt ->
                stmt.fetchSize = execInfoObject.getMaxFetchSize()
                stmt.maxRows = execInfoObject.getMaxRows()
            }

            sql.eachRow(queryScript, metaClosure) { resultSet ->
                Map<String, Object> row = new HashMap<String, Object>()
                execInfoObject.recordsColumn.each { name ->
                    def val = resultSet.getString((String) name)
                    if (val.getClass().getSimpleName() == 'CLOB') {
                        val = val?.getSubString(1, (int) val.length())
                    } else if (val.getClass().getSimpleName() == 'BLOB') {
                        val = val.toString()
                    } else if (val.getClass().getSimpleName() == 'TIMESTAMP') {
                        val = val.toString()
                    }
                    row.put(name, val)
                    resultSet.getString(name).getClass().getSimpleName()
                }
                execInfoObject.recordsData.add(row)
            }
        } catch (ex) {
            result.failed = ex
            return result
        } finally {
            sql.close()
        }

        result.success = true
        return result
    }

    /***
     * 查詢匯出
     * @param params
     * @return
     */
    def queryExport(ExecInfoObject execInfoObject) {
        def result = [:]

        def queryScript = execInfoObject.decodeQueryTextArea()
        if (!queryScript) {
            result.failed = "未輸入查詢script"
            return result
        }
        def countScript
        try {
            countScript = buildCountScript(queryScript)
        } catch (ex) {
            result.failed = ex
            return result
        }
        // Oracle分號有問題, 讓 CCJSqlParserUtil 處理掉
        if (execInfoObject.decodeDbType() == ORACLE_DRIVER) {
            queryScript = CCJSqlParserUtil.parse(queryScript,
                    { parser ->
                        parser.withSquareBracketQuotation(true).withAllowComplexParsing(true)
                    }).toString()
        }

        Sql sql = null
        try {
            sql = initSql(execInfoObject.decodeUrl(), execInfoObject.decodeUsername(), execInfoObject.decodePd())
            cancelSql = sql
        } catch (ex) {
            result.failed = ex
            return result
        }

        try {
            //取得總筆數
            sql.withStatement { stmt ->
                stmt.fetchSize = 1
                stmt.maxRows = 1
            }
            execInfoObject.recordsTotal = sql.firstRow(countScript)[0]

            //取得欄位名稱
            def metaClosure = { metadata ->
                int columnCount = metadata.getColumnCount()
                ArrayList<String> columns = new ArrayList<>()
                for (int j = 1; j <= columnCount; j++) {
                    String columnName = metadata.getColumnName(j)
                    columns.add(columnName)
                    execInfoObject.recordsColumn.add(columnName)
                }
            }

            // 匯出 json 格式, 避免出現數字變0問題
            // 檔案暫存的路徑
            def tempPath = System.getProperty("java.io.tmpdir");
            def tempKey = result.key = UUID.randomUUID().toString()
            def outFile = new File(tempPath, tempKey)

            outFile.withOutputStream { out ->
                // 寫入BOM
                byte[] uft8bom = [0xEF, 0xBB, 0xBF]
                out.write(uft8bom)
            }

            outFile.withWriterAppend('UTF-8') { out ->
                sql.withStatement { stmt ->
                    stmt.fetchSize = execInfoObject.getMaxFetchSize()
                }

                // 寫入筆數
                def writeRows = 0
                out.write("[")
                sql.eachRow(queryScript, metaClosure) { GroovyResultSet resultSet ->
                    def data = [:]
                    execInfoObject.recordsColumn.each { key ->
                        if (resultSet?.getAt(key)?.getClass() in [java.sql.Timestamp, java.sql.Date]) {
                            data[key] = resultSet?.getAt(key)?.toString()
                        } else {
                            data[key] = resultSet.getAt(key)
                        }
                    }

                    // 最後一筆不需要,
                    if (resultSet.getRow() == execInfoObject.getRecordsTotal()) {
                        out.write(JSON.toJSONString(data, JSONWriter.Feature.WriteMapNullValue))
                    } else {
                        out.write(JSON.toJSONString(data, JSONWriter.Feature.WriteMapNullValue) + ",")
                    }

                    writeRows++
                }
                // 沒寫入時輸出標題+無資料訊息
                if (writeRows == 0) {
                    def data = [:]
                    execInfoObject.recordsColumn.each { key ->
                        data["${key}"] = '查無資料'
                    }
                    out.write(JSON.toJSONString(data))
                }
                out.write("]")
            }
        } catch (ex) {
            result.failed = ex
            return result
        } finally {
            sql.close()
        }

        result.success = true
        return result
    }


    /***
     * 連線設定
     * @param url
     * @param username
     * @param pwd
     * @return
     * @throws Exception
     */
    Sql initSql(url, username, pwd) throws Exception {
        def connection = DriverManager.getConnection(url, username, pwd)    //SQLException
        Sql sql = new Sql(connection)    //SQLException
        return sql
    }

    /***
     * 解析 query script, 重組 count scirpt
     * @param script
     * @return
     */
    def buildCountScript(queryScript) {
        def countScript = ""

        Select select = ((Select) CCJSqlParserUtil.parse(
                queryScript,
                { parser ->
                    parser.withSquareBracketQuotation(true).withAllowComplexParsing(true)
                }))
        if (select.getSelectBody() instanceof SetOperationList) {
            def queryInnterScript = ""

            select?.getSelectBody()?.selects?.eachWithIndex { it, i ->
                PlainSelect plainSelect = (PlainSelect) it
                //重新處理欄位 alias
                plainSelect?.selectItems?.eachWithIndex { selectItem, no ->
                    if (selectItem.getClass() != net.sf.jsqlparser.statement.select.AllTableColumns &&
                            selectItem.getClass() != net.sf.jsqlparser.statement.select.AllColumns) {
                        selectItem?.alias = new Alias("A${no}", true)
                    }
                }
                plainSelect.setOrderByElements(null)

                queryInnterScript += plainSelect.toString()
                if (select?.getSelectBody()?.operations[i]) {
                    queryInnterScript += " " + select?.getSelectBody()?.operations[i] + " "
                }
            }

            if (select?.getWithItemsList()?.size() > 0) {
                countScript = "with " + select?.getWithItemsList().join(',')
                countScript = countScript + " select count(1) as TOTAL from ( ${queryInnterScript} ) t"
            } else {
                countScript = countScript + " select count(1) as TOTAL from ( ${queryInnterScript} ) t"
            }
        } else {
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody()

            //重新處理欄位 alias
            plainSelect?.selectItems?.eachWithIndex { selectItem, no ->
                if (selectItem.getClass() != net.sf.jsqlparser.statement.select.AllTableColumns &&
                        selectItem.getClass() != net.sf.jsqlparser.statement.select.AllColumns) {
                    selectItem?.alias = new Alias("A${no}", true)
                }
            }
            plainSelect.setOrderByElements(null)

            if (select?.getWithItemsList()?.size() > 0) {
                countScript = "with " + select?.getWithItemsList().join(',')
                countScript = countScript + " select count(1) as TOTAL from ( ${plainSelect.toString()} ) t"
            } else {
                countScript = countScript + " select count(1) as TOTAL from ( ${plainSelect.toString()} ) t"
            }
        }
        return countScript
    }
}