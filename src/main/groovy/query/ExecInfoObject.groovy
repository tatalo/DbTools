package query

class ExecInfoObject implements Serializable {
    private static final long serialVersionUID = 1L

    String url
    String username
    String pd
    String dbType
    Integer maxRows = 10
    String queryTextArea
    String execTextArea
    String execType
    Integer maxFetchSize = 500

    Integer recordsTotal
    List recordsColumn = []
    List recordsData = []

    def decodeUrl() {
        return this.url ? new String((byte[]) this.url?.substring(10)?.decodeBase64()) : null
    }
    def decodeUsername() {
        return this.username ? new String((byte[]) this.username?.substring(10)?.decodeBase64()) : null
    }
    def decodePd() {
        return this.pd ? new String((byte[]) this.pd?.substring(10)?.decodeBase64()) : null
    }
    def decodeDbType() {
        return this.dbType ? new String((byte[]) this.dbType?.substring(10)?.decodeBase64()) : null
    }
    def decodeQueryTextArea() {
        return this.queryTextArea ? new String((byte[]) this.queryTextArea?.substring(10)?.decodeBase64()) : null
    }
    def decodeExecTextArea() {
        return this.execTextArea ? new String((byte[]) this.execTextArea?.substring(10)?.decodeBase64()) : null
    }

    /***
     * 設定參數
     * @param params
     */
    void setParams(params) {
        this.url       = params?.url
        this.username  = params?.username
        this.pd        = params?.pd
        this.dbType      = params?.dbType
        this.execType      = params?.execType
        this.queryTextArea  = params?.queryTextArea
        this.maxRows   = params?.int('maxRows')
        this.maxFetchSize   = params?.int('maxFetchSize')
        this.execTextArea   = params?.execTextArea
    }
}

