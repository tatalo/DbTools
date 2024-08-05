<g:set var="form_id1" value="${g.uuid()}"/>
<g:set var="form_id2" value="${g.uuid()}"/>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="robots" content="noindex , nofollow">

    <g:javascript>
        window.appname = '${request.contextPath}';
    </g:javascript>

    <title>exuSql</title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>

    <style>
    table.conditionTable, tbody.conditionTable > * > th, tbody.conditionTable > * > td  {
        border-collapse: collapse;
        border: 1px solid black;
    }

    td.resultTable, th.resultTable {
        border: 1px solid gray;
    }

    th.resultTable {
        background-color:lightblue;
    }

    table.resultTable {
        table-layout: fixed;
        display: block;
        overflow-x: auto;
    }

    thead tr th {
        position: sticky;
        top: 0;
        background: #F4F4F5;
        height: 30px;
    }

    /* 首列固定/最后一列固定*/
    th:first-child.resultTable,
    td:first-child.resultTable {
        position: sticky;
        z-index: 2;
        left: 0px;
        background-color:lightpink;
        text-align: center;
        min-width: 20px;
    }

    td:nth-child(2).resultTable {
        left: 28px;
        background-color:lightpink;
        position: sticky;
    }

    th:nth-child(2).resultTable {
        z-index: 6;
        left: 28px;
        position: sticky;
    }

    thead.resultTable tr.resultTable th.resultTable {
        position: sticky;
        top:0; /* 列首永遠固定於上 */
    }

    th:first-child.resultTable{
        z-index:5;
        background-color:lightblue;
    }

    .multiline-ellipsis {
        max-height: 40px;
        max-width: 250px;
        word-wrap: break-word;
        overflow: auto;
    }

    textarea {
        width: 1300px;
        height: 100px;
        padding: 2px;
        border: 1px solid #ccc;
        box-sizing: border-box;
    }
    </style>

</head>
<body onload="execTypeSwitchDiv();">
<div style="display: none;" class="connection">
    使用說明：
    <br>
    1. 如需支援其他 DB 時，請 dependencies 對應的 jdbc 或在libs放入相關的jar檔。(以下僅參考)。
    <BR/>
    implementation group: 'org.postgresql', name: 'postgresql', version: '42.3.1'
    <font color="red">(postgres)</font>
    <BR/>
    implementation group: 'com.microsoft.sqlserver', name: 'mssql-jdbc', version: '9.4.0.jre8'
    <font color="red">(sqlservewr)</font>
    <BR/>
    implementation group: 'com.oracle', name: 'ojdbc6', version: '12.1.0.1-atlassian-hosted'
    <font color="red">(oracle)</font>
    <BR/>
    implementation group: 'org.mariadb.jdbc', name: 'mariadb-java-client', version: '2.7.4'
    <font color="red">(mariadb)</font>
    <BR/>
    implementation "mysql:mysql-connector-java:5.1.49"
    <font color="red">(mysql)</font>
    <hr>
</div>
<g:form name="${form_id2}" method="post">
    <g:hiddenField name="_csrf" value="${_csrf?.token}"/>
    <g:hiddenField name="url"/>
    <g:hiddenField name="username"/>
    <g:hiddenField name="pd"/>
    <g:hiddenField name="dbType"/>
    <g:hiddenField name="execType"/>
    <g:hiddenField name="maxRows"/>
    <g:hiddenField name="maxFetchSize"/>
    <g:hiddenField name="queryTextArea"/>
    <g:hiddenField name="execTextArea"/>
</g:form>

<g:form name="${form_id1}">
    <table width="100%" class="conditionTable" id="conditionTable">
        <tbody class="conditionTable">
        <tr style="display: none;" class="connection">
            <th width="150px">url</th>
            <td width="">
                <g:textField name="url" value="${execInfoObject?.decodeUrl() ?: grailsApplication.config.dataSource.url}"
                             style="width:90%;"/>
            </td>
        </tr>
        <tr style="display: none;" class="connection">
            <th>user</th>
            <td>
                <g:textField name="username"
                             value="${execInfoObject?.decodeUsername() ?: grailsApplication.config.dataSource.username}"
                             style="width:90%;"/>
            </td>
        </tr>
        <tr style="display: none;" class="connection">
            <th>pd</th>
            <td>
                <g:field type="password" name="pd"
                         value="${execInfoObject?.decodePd() ?: grailsApplication.config.dataSource.password}"
                         style="width:90%;"/>
            </td>
        </tr>
        <tr style="display: none;" class="connection">
            <th>drive</th>
            <td>
                <g:select name="dbType"
                          from="['com.microsoft.sqlserver.jdbc.SQLServerDriver': 'SQLServer(com.microsoft.sqlserver.jdbc.SQLServerDriver)',
                                 'oracle.jdbc.OracleDriver'                    : 'Oracle(oracle.jdbc.OracleDriver)',
                                 'org.postgresql.Driver'                       : 'PostgreSQL(org.postgresql.Driver)',
                                 'org.mariadb.jdbc.Driver'                     : 'MariaDB(com.mariadb.jdbc.Driver)',
                                 'com.mysql.jdbc.Driver'                       : 'MySQL(com.mysql.jdbc.Driver)']"
                          style="width:90%;"
                          optionKey="key" optionValue="value"
                          value="${execInfoObject?.decodeDbType() ?: grailsApplication.config.dataSource.driverClassName}"
                          noSelection="['': '---']"/>
            </select>
            </td>
        </tr>
        <tr>
            <th width="150px">Type</th>
            <td>
                <input type="radio" id="execType.1" name="execType"
                       onchange="execTypeSwitchDiv();" ${(execInfoObject?.execType ?: 'query') == 'query' ? 'checked' : ''}
                       value="query">
                <label for="execType.1">查詢</label>
                <input type="radio" id="execType.2" name="execType"
                       onchange="execTypeSwitchDiv();" ${(execInfoObject?.execType ?: 'query') == 'execute' ? 'checked' : ''}
                       value="execute">
                <label for="execType.2">執行</label>
            </td>
        </tr>
        <tr class="queryDiv" style="display:none">
            <th width="150px">顯示筆數(最大100)</th>
            <td>
                <g:textField name="maxRows" value="${execInfoObject?.maxRows ?: 10}"/>
            </td>
        </tr>
        <tr class="queryDiv" style="display:none">
            <th width="150px">緩存筆數</th>
            <td>
                <g:textField name="maxFetchSize" value="${execInfoObject?.maxFetchSize ?: 1000}"/>
            </td>
        </tr>
        <tr class="queryDiv" style="display:none">
            <th>查詢輸入框</th>
            <td colspan="3">
                <g:textArea id="queryArea" name="queryTextArea" value="${execInfoObject?.decodeQueryTextArea()}" rows="6"/>
            </td>
        </tr>

        <tr class="queryDiv" style="display:none">
            <th></th>
            <td colspan="3">
                <button type="button" onclick="query();">查詢</button>
                <button type="button" onclick="queryExport();">查詢匯出</button>
                <button type="button" onclick="stopExecute();">停止</button>
                <button type="button" onclick="showConnection();"></button>
            </td>
        </tr>

        <tr class="executeDiv" style="display:none">
            <th>執行輸入框</th>
            <td colspan="3">
                <div>
                    <g:textArea id="executeArea" name="execTextArea" value="${execInfoObject?.decodeExecTextArea()}" rows="5"/>
                </div>
                <div>
                    ※ 如果多筆更新僅會回傳最後一筆更新數
                </div>
            </td>
        </tr>


        <tr class="executeDiv" style="display:none">
            <th></th>
            <td colspan="3">
                <button type="button" onclick="execute();">執行</button>
                <button type="button" onclick="stopExecute();">停止</button>
                <button type="button" onclick="showConnection();"></button>
            </td>
        </tr>

        </tbody>
    </table>
    <g:if test="${flash?.failed}">
        <font color="red">
            ${flash.remove('failed')}
        </font>
    </g:if>
    <g:if test="${flash?.success}">
        <font color="blue">
            ${flash.remove('success')}
        </font>
    </g:if>
</g:form>
<div>
    <hr>
    查詢結果(總數：${execInfoObject?.recordsTotal})
    <table class="resultTable">
        <thead class="resultTable">
        <tr class="resultTable">
            <th class="resultTable">
            </th>
            <g:each in="${execInfoObject?.recordsColumn}" var="colI">
                <th class="resultTable">
                    ${colI}
                </th>
            </g:each>
        </tr>
        </thead>
        <tbody class="resultTable">
        <g:each in="${execInfoObject?.recordsData}" var="dataI" status="i">
            <tr class="resultTable">
                <td class="resultTable">
                    <div class="multiline-ellipsis" title="${i+1}">
                        ${i+1}
                    </div>
                </td>
                <g:each in="${execInfoObject?.recordsColumn}" var="colI">
                    <td class="resultTable">
                        <div class="multiline-ellipsis" title="${dataI?."${colI}"}">
                            ${dataI?."${colI}"}
                        </div>
                    </td>
                </g:each>
            </tr>
        </g:each>
        <g:if test="${execInfoObject?.recordsData?.size() < execInfoObject?.recordsTotal}">
            <tr>
                <td class="resultTable">
                    ....
                </td>
            </tr>
        </g:if>
        </tbody>
    </table>
</div>

<script type="text/javascript">

    // 調整輸入框
    var queryArea = document.getElementById('queryArea');
    var executeArea = document.getElementById('executeArea');

    queryArea.addEventListener('mouseup', function() {
        var width = queryArea.offsetWidth;
        var height = queryArea.offsetHeight;

        sessionStorage.setItem('queryAreaWidth', width);
        sessionStorage.setItem('queryAreaHeight', height);
    });

    executeArea.addEventListener('mouseup', function() {
        var width = executeArea.offsetWidth;
        var height = executeArea.innerHeight;

        sessionStorage.setItem('executeAreaWidth', width);
        sessionStorage.setItem('executeAreaHeight', height);
    });

    document.addEventListener("DOMContentLoaded", function() {
        var queryAreaHeight = sessionStorage.getItem('queryAreaHeight');
        var queryAreaWidth = sessionStorage.getItem('queryAreaWidth');
        var executeAreaHeight = sessionStorage.getItem('executeAreaHeight');
        var executeAreaWidth = sessionStorage.getItem('executeAreaWidth');

        if (queryAreaHeight && queryAreaWidth) {
            queryArea.style.height = queryAreaHeight + 'px';
            queryArea.style.width = queryAreaWidth + 'px';
        }
        if (executeAreaHeight && executeAreaWidth) {
            executeArea.style.height = executeAreaHeight + 'px';
            executeArea.style.width = executeAreaWidth + 'px';
        }
    });

    // 調整下方Table
    function resizeTable() {
        var tableHeight = document.getElementById('conditionTable').offsetHeight;

        var windowHeight = window.innerHeight;
        var windowWidth = window.innerWidth;
        var newHeight = windowHeight - tableHeight;

        var resultTable = document.querySelector('table.resultTable');
        resultTable.style.width = (windowWidth-25) + 'px';

        if (document.body.scrollHeight > window.innerHeight) {
            resultTable.style.height = (newHeight-100) + 'px';
        }
    }
    window.addEventListener('resize', function() {
        resizeTable();
    });

    setTimeout(function() {
        window.dispatchEvent(new Event('resize'));
    }, 100);

    /***
     * 加密, 10碼隨機碼
     */
    function base64Text() {
        document.getElementById('${form_id2}').elements.url.value = randomWord(true, 10, 10) + btoa(unescape(encodeURIComponent(document.getElementById('${form_id1}').elements.url.value)));
        document.getElementById('${form_id2}').elements.username.value = randomWord(true, 10, 10) + btoa(unescape(encodeURIComponent(document.getElementById('${form_id1}').elements.username.value)));
        document.getElementById('${form_id2}').elements.pd.value = randomWord(true, 10, 10) + btoa(unescape(encodeURIComponent(document.getElementById('${form_id1}').elements.pd.value)));
        document.getElementById('${form_id2}').elements.dbType.value = randomWord(true, 10, 10) + btoa(unescape(encodeURIComponent(document.getElementById('${form_id1}').elements.dbType.value)));
        document.getElementById('${form_id2}').elements.execType.value = document.getElementById('${form_id1}').elements.execType.value;
        document.getElementById('${form_id2}').elements.maxRows.value = document.getElementById('${form_id1}').elements.maxRows.value;
        document.getElementById('${form_id2}').elements.maxFetchSize.value = document.getElementById('${form_id1}').elements.maxFetchSize.value;
        document.getElementById('${form_id2}').elements.queryTextArea.value = Math.random().toString(36).slice(-10) + btoa(unescape(encodeURIComponent(document.getElementById('${form_id1}').elements.queryTextArea.value)));
        document.getElementById('${form_id2}').elements.execTextArea.value = Math.random().toString(36).slice(-10) + btoa(unescape(encodeURIComponent(document.getElementById('${form_id1}').elements.execTextArea.value)));
    }

    /***
     * 產生隨機字母
     * @param randomFlag
     * @param min
     * @param max
     * @returns {string}
     */
    function randomWord(randomFlag, min, max) {
        var str = "", range = min,
            // 字母，數字
            arr = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'];
        // 隨機產生
        if (randomFlag) {
            range = Math.round(Math.random() * (max - min)) + min;
        }
        for (var i = 0; i < range; i++) {
            pos = Math.round(Math.random() * (arr.length - 1));
            str += arr[pos];
        }
        return str;
    }


    /***
     * 查詢
     */
    function query() {
        document.getElementById('${form_id2}').action = '${createLink(controller: 'query', action: 'query')}';
        base64Text();//加密
        document.getElementById('${form_id2}').submit();
    }

    /***
     * 執行
     */
    function queryExport() {
        document.getElementById('${form_id2}').action = '${createLink(controller: 'query', action: 'queryExport', id: 'export.json')}';
        base64Text();//加密
        document.getElementById('${form_id2}').submit();
    }

    /***
     * 執行
     */
    function execute() {
        var queryScript = document.getElementById('${form_id1}').elements.execTextArea.value;
        var result = confirm('請確認Sql正常\n\n' + queryScript);
        if (result) {
            document.getElementById('${form_id2}').action = '${createLink(controller: 'query', action: 'execute')}';
            base64Text();//加密
            document.getElementById('${form_id2}').submit();
        }
    }

    /***
     * 執行
     */
    function stopExecute() {
        document.getElementById('${form_id2}').action = '${createLink(controller: 'query', action: 'stopExecute')}';
        base64Text(); //加密
        document.getElementById('${form_id2}').submit();
    }

    /***
     * 顯示區塊檢查
     */
    function execTypeSwitchDiv() {
        var execType = document.querySelector('input[name="execType"]:checked').value;

        if (execType == 'query') {
            var queryDivs = document.getElementsByClassName("queryDiv");
            for (var i = 0; i < queryDivs.length; i++) {
                queryDivs[i].style.display = "";
            }
            var executeDivs = document.getElementsByClassName("executeDiv");
            for (var i = 0; i < executeDivs.length; i++) {
                executeDivs[i].style.display = "none";
            }
        } else if (execType == 'execute') {
            var queryDivs = document.getElementsByClassName("queryDiv");
            for (var i = 0; i < queryDivs.length; i++) {
                queryDivs[i].style.display = "none";
            }
            var executeDivs = document.getElementsByClassName("executeDiv");
            for (var i = 0; i < executeDivs.length; i++) {
                executeDivs[i].style.display = "";
            }
        }
    }

    // 連線區
    function showConnection() {
        var elements = document.querySelectorAll('.connection');
        elements.forEach(function(element) {
            var currentDisplay = window.getComputedStyle(element).getPropertyValue('display');

            console.log("currentDisplay = " + currentDisplay);
            if (currentDisplay === 'none') {
                element.style.display = ''; // 或者其他你想要的显示样式
            } else {
                element.style.display = 'none'; // 或者其他你想要的显示样式
            }
        });
    }
</script>
</body>
</html>