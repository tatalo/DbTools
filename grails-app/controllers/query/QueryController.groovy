package query

import javax.servlet.ServletOutputStream

class QueryController {
    def queryService

    /***
     * 未登入畫面
     * @return
     */
    def index() {
        session.query = null
        render view: "/query/index"
    }

    /***
     * 主畫面
     * @return
     */
    def verify() {
        String user = grailsApplication.config.query.authorized.tokenize(':')[0]
        String pd = grailsApplication.config.query.authorized.tokenize(':')[1]

        if (user != (new String((byte[]) params.uCode?.substring(10)?.decodeBase64())) || pd != (new String((byte[]) params.pCode?.substring(10)?.decodeBase64()))) {
            redirect controller: "query", action: "index"
            return
        }

        session.query = true
        redirect controller: "query", action: "qIndex"
    }

    /***
     * 查詢主畫面
     * @return
     */
    def qIndex() {
        if (session.query != true) {
            redirect controller: "query", action: "index"
            return
        }

        render view: "/query/qIndex", model: [execInfoObject: (ExecInfoObject) flash?.chainModel?.execInfoObject]
    }

    /***
     * 查詢
     * @re1turn
     */
    def query() {
        if (session.query != true) {
            redirect controller: "query", action: "index"
            return
        }

        ExecInfoObject execInfoObject = queryService.execInfoObjectInit(params)
        def result = queryService.query(execInfoObject)


        if (result.failed) {
            flash.failed = result.failed
            chain(controller: "query", action: "qIndex", model: [execInfoObject: execInfoObject])
            return
        }

        flash.success = result.success
        chain(controller: "query", action: "qIndex", model: [execInfoObject: execInfoObject])
    }

    /***
     * 查詢匯出
     * @return
     */
    def queryExport() {
        if (session.query != true) {
            redirect controller: "query", action: "index"
            return
        }

        ExecInfoObject execInfoObject = queryService.execInfoObjectInit(params)
        def result = queryService.queryExport(execInfoObject)

        if (result.failed) {
            flash.failed = result.failed
            chain(controller: "query", action: "qIndex", model: [execInfoObject: execInfoObject])
            return
        }

        flash.success = result.success
        response.setHeader("Content-Disposition", "attachment;")
        response.contentType = "application/octet-stream"
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + result.key)
        FileInputStream ofd = null
        ServletOutputStream outputStream = null
        try {
            ofd = new FileInputStream(file)
            outputStream = response.getOutputStream()

            byte[] buffer = new byte[1024]
            int bytesRead

            while ((bytesRead = ofd.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
        } catch (IOException e) {
            e.printStackTrace()
        } finally {
            if (ofd != null) {
                try {
                    ofd.close();
                } catch (IOException e) {
                    flash.failed = e.getMessage()
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    flash.failed = e.getMessage()
                }
            }
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /***
     * 執行
     * @return
     */
    def execute() {
        if (session.query != true) {
            redirect controller: "query", action: "index"
            return
        }

        ExecInfoObject execInfoObject = queryService.execInfoObjectInit(params)
        def result = queryService.execute(execInfoObject)

        if (result.failed) {
            flash.failed = result.failed
            chain(controller: "query", action: "qIndex", model: [execInfoObject: execInfoObject])
            return
        }

        flash.success = result.success
        chain(controller: "query", action: "qIndex", model: [execInfoObject: execInfoObject])
    }


    /***
     * 停止
     * @return
     */
    def stopExecute() {
        if (session.query != true) {
            redirect controller: "query", action: "index"
            return
        }

        ExecInfoObject execInfoObject = queryService.execInfoObjectInit(params)
        def result = queryService.stopExecute()

        if (result.failed) {
            flash.failed = result.failed
            chain(controller: "query", action: "qIndex", model: [execInfoObject: execInfoObject])
            return
        }

        flash.success = result.success
        chain(controller: "query", action: "qIndex", model: [execInfoObject: execInfoObject])
    }
}
