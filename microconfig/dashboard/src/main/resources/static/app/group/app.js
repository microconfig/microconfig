GROUP = {
    init: function () {
        LOGS_SECTION.init();

        let groupName = GROUP.currentGroup();
        $('title').text('Releases: ' + groupName);
        $('#main_name').text(groupName);

        GROUP.loadCurrentReleaseInfo();
        GROUP.loadNewReleasesInfo();
    },
    loadCurrentReleaseInfo: function () {
        return $.get('/group/current-release/' + GROUP.currentGroup()).then(function (currentReleaseToServices) {
            GROUP.drawStatus(currentReleaseToServices, $('#current_releases'), 'Current')
        });
    },
    loadNewReleasesInfo: function () {
        $.get('/group/new-releases/' + GROUP.currentGroup()).then(function (newReleaseToServices) {
            GROUP.drawStatus(newReleaseToServices, $('#new_releases'), 'New');
            $('.arrow').fadeIn();
            $('.action_step').fadeIn();

            $('#approve').click(function () {
                $(this).attr('disabled', 'true').addClass('disabled_step');
                GROUP.approve(newReleaseToServices).then(markExecuted(this));
            });
            $('#deploy').click(function () {
                $(this).attr('disabled', 'true').addClass('disabled_step');
                GROUP.deploy(newReleaseToServices).then(markExecuted(this));
            });
            $('#healthcheck').click(function () {
                GROUP.healthcheck().then(markExecuted(this));
            });
            $('#status').click((function () {
                GROUP.status().then(markExecuted(this));
            }));

            function markExecuted(button) {
                return function () {
                    $(button).addClass('executed_step');
                }
            }
        });
    },
    drawStatus: function (nameToValues, container, releaseTitle) {
        container.hide();
        container.append($('<header/>').addClass('release_title').text(releaseTitle));
        for (let r in nameToValues) {
            container.append(drawValues(r, nameToValues[r]));
        }
        container.fadeIn();

        function drawValues(release, services) {
            let topContainer = $('<li/>');

            let header = $('<header/>').addClass('release_name');
            if (release === '') {
                header.text('already updated').addClass('release_not_found');
                topContainer.addClass('release_not_found');
            } else {
                header.html(release.replace(/,/g, '<br/>'))

            }
            return topContainer
                .append(header)
                .append($('<div/>').html(services.toString().replace(/,/g, '<br/>')).addClass('services_group'))
                .addClass('release_group');
        }
    },
    approve: function (newReleaseToServices) {
        let url = '/group/approve/' + GROUP.currentGroup();
        return GROUP.executeAndShowLogs('Approving...', url, newReleaseToServices);
    },
    deploy: function (newReleaseToServices) {
        let url = '/group/deploy/' + GROUP.currentGroup();
        return GROUP.executeAndShowLogs('Deploying...', url, newReleaseToServices);
    },
    healthcheck: function () {
        let url = '/group/healthcheck/' + GROUP.currentGroup();
        return GROUP.executeAndShowLogs('Healthcheck...', url);
    },
    status: function () {
        let url = '/group/status/' + GROUP.currentGroup();
        return GROUP.executeAndShowLogs('Status...', url);
    },
    executeAndShowLogs: function (command, url, body) {
        LOGS_SECTION.open();
        return $("#logs_frame")[0].contentWindow.LOGS.logsForRequest(command, url, body);
    },
    currentGroup: function () {
        return $.urlParam('name');
    }
};

LOGS_SECTION = {
    init: function () {
        $('#toggle_logs').click(function () {
            let frame = $('#logs_frame');
            $(this).text(frame.is(':visible') ? 'Show logs' : 'Hide logs');
            frame.slideToggle();
        });
    },
    open: function () {
        if (!$('#logs_frame').is(':visible')) {
            $('#toggle_logs').click();
        }
    }
};