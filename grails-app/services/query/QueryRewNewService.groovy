package query

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONWriter
import groovy.sql.Sql
import net.sf.jsqlparser.expression.Alias
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.select.SetOperationList

import java.sql.DriverManager

class QueryRewNewService {

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

        try (Sql sql = initSql(execInfoObject.decodeUrl(), execInfoObject.decodeUsername(), execInfoObject.decodePd())) {
            cancelSql = sql
            sql.connection.autoCommit = false
            def executeCnts = sql.executeUpdate(execScript)
            sql.commit()
            result.success = "成功執行 ${executeCnts} 數 "
        } catch (Exception ex) {
            result.failed = ex
        }
        return result
    }

    def stopExecute() {
        def result = [:]
        try {
            cancelSql.rollback()
        } catch (Exception ex) {
            result.failed = ex.toString()
        } finally {
            closeSql(cancelSql, result)
        }
        return result
    }

    def query(ExecInfoObject execInfoObject) {
        def result = [:]
        def queryScript = execInfoObject.decodeQueryTextArea()
        if (!queryScript) {
            result.failed = "未輸入查詢script"
            return result
        }

        try (Sql sql = initSql(execInfoObject.decodeUrl(), execInfoObject.decodeUsername(), execInfoObject.decodePd())) {
            cancelSql = sql
            execInfoObject.recordsTotal = sql.firstRow(buildCountScript(queryScript))[0]
            fetchQueryResults(sql, queryScript, execInfoObject)
            result.success = true
        } catch (Exception ex) {
            result.failed = ex
        }
        return result
    }

    def queryExport(ExecInfoObject execInfoObject) {
        def result = [:]
        def queryScript = execInfoObject.decodeQueryTextArea()
        if (!queryScript) {
            result.failed = "未輸入查詢script"
            return result
        }

        try (Sql sql = initSql(execInfoObject.decodeUrl(), execInfoObject.decodeUsername(), execInfoObject.decodePd())) {
            cancelSql = sql
            execInfoObject.recordsTotal = sql.firstRow(buildCountScript(queryScript))[0]
            exportQueryResults(sql, queryScript, execInfoObject, result)
            result.success = true
        } catch (Exception ex) {
            result.failed = ex
        }
        return result
    }

    private Sql initSql(String url, String username, String pwd) throws Exception {
        def connection = DriverManager.getConnection(url, username, pwd)
        return new Sql(connection)
    }

    private void closeSql(Sql sql, def result) {
        try {
            sql.close()
        } catch (Exception ex) {
            result.failed = ex.toString()
        }
    }

    private void fetchQueryResults(Sql sql, String queryScript, ExecInfoObject execInfoObject) {
        sql.withStatement { stmt ->
            stmt.fetchSize = execInfoObject.getMaxFetchSize()
            stmt.maxRows = execInfoObject.getMaxRows()
        }
        sql.eachRow(queryScript, { metadata ->
            int columnCount = metadata.getColumnCount()
            for (int j = 1; j <= columnCount; j++) {
                execInfoObject.recordsColumn.add(metadata.getColumnName(j))
            }
        }) { resultSet ->
            def row = [:]
            execInfoObject.recordsColumn.each { name ->
                row[name] = resultSet.getString(name)
            }
            execInfoObject.recordsData.add(row)
        }
    }

    private void exportQueryResults(Sql sql, String queryScript, ExecInfoObject execInfoObject, def result) {
        def tempPath = System.getProperty("java.io.tmpdir")
        def tempKey = result.key = UUID.randomUUID().toString()
        def outFile = new File(tempPath, tempKey)

        outFile.withOutputStream { out ->
            out.write([0xEF, 0xBB, 0xBF] as byte[])
        }

        outFile.withWriterAppend('UTF-8') { out ->
            sql.withStatement { stmt ->
                stmt.fetchSize = execInfoObject.getMaxFetchSize()
            }
            out.write("[")
            sql.eachRow(queryScript, { metadata ->
                int columnCount = metadata.getColumnCount()
                for (int j = 1; j <= columnCount; j++) {
                    execInfoObject.recordsColumn.add(metadata.getColumnName(j))
                }
            }) { resultSet ->
                def data = [:]
                execInfoObject.recordsColumn.each { key ->
                    data[key] = resultSet.getObject(key)
                }
                out.write(JSON.toJSONString(data, JSONWriter.Feature.WriteMapNullValue) + ",")
            }
            out.write("]")
        }
    }

    private String buildCountScript(String queryScript) {
        def countScript = ""
        Select select = CCJSqlParserUtil.parse(queryScript) as Select
        if (select.selectBody instanceof SetOperationList) {
            def queryInnerScript = select.selectBody.selects.collect { it.toString() }.join(" ${select.selectBody.operations.join(' ')} ")
            countScript = "select count(1) as TOTAL from (${queryInnerScript}) t"
        } else {
            PlainSelect plainSelect = select.selectBody as PlainSelect
            plainSelect.selectItems.eachWithIndex { selectItem, no ->
                if (!(selectItem instanceof net.sf.jsqlparser.statement.select.AllTableColumns) &&
                        !(selectItem instanceof net.sf.jsqlparser.statement.select.AllColumns)) {
                    selectItem.alias = new Alias("A${no}", true)
                }
            }
            countScript = "select count(1) as TOTAL from (${plainSelect}) t"
        }
        return countScript
    }
}