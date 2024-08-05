<g:set var="form_id1" value="${g.uuid()}"/>
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
</head>

<body>
<script type="text/javascript">
    var uCode = encodeURI(prompt('請輸入Ucode'));
    var pCode = encodeURI(prompt('請輸入Pcode'));

    if (uCode == 'null' || pCode == 'null') {
        throw "取消";
    }
    window.location.href = window.appname + "/query/verify?uCode=" + randomWord(true, 10, 10) + btoa(unescape(encodeURIComponent(uCode))) + "&pCode=" + randomWord(true, 10, 10) + btoa(unescape(encodeURIComponent(pCode)));

    //產生隨機字母
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
</script>
</body>
</html>