LOGS = {
    logsForRequest: function (title, url, body, callback) {
        let lastResponseLength = 0;
        let ansiIp = new AnsiUp;

        let $document = $(document);
        let area = $('#log_textarea').text(title + '\n\n');
        return $.ajax(
            {
                url: url,
                type: 'POST',
                data: JSON.stringify(body),
                contentType: 'application/json',
                success: callback,
                xhrFields: {
                    onprogress: function (e) {
                        let response = e.currentTarget.response.replace(/^\s+/, "");
                        let newPart = response.substring(lastResponseLength);
                        lastResponseLength = response.length;

                        area.append(ansiIp.ansi_to_html(newPart));
                        $document.scrollTop(area[0].scrollHeight);
                    }
                }
            });
    }
};