DASHBOARD = {
    init: function () {
        $.get('/stat/memory').then(DASHBOARD.draw);
        $('#force_refresh').click(DASHBOARD.refresh);
    },
    refresh: function () {
        $.post('/stat/refresh').then(function () {
            document.location = document.location;
        });
    },
    draw: function (env) {
        $('#main_name').text(env.name);
        $('#main_memory').text(env.valueInMb + ' mb');

        let hosts = $('#hosts').hide();
        $.each(env.children, function (i, host) {
            hosts.append(DASHBOARD.drawHost(host));
        });
        hosts.fadeIn();
    },
    drawHost: function (host) {
        let groups = $('<ul/>').attr('name', 'groups');
        $.each(host.children, function (i, componentGroup) {
            groups.append(DASHBOARD.drawComponentGroup(componentGroup));
        });

        return $('<li/>')
            .attr('name', host.name)
            .addClass('host_group')
            .append($('<header/>').addClass('host_group_header').html(host.name + ",&nbsp&nbsp" + host.valueInMb + 'mb'))
            .append(groups);
    },
    drawComponentGroup: function (componentGroup) {
        let services = $('<ul/>');
        $.each(componentGroup.children, function (i, service) {
            services.append(DASHBOARD.drawService(service));
        });

        let header = $('<header/>').addClass('group_name').append(
            $('<a>').attr('href', '/group?name=' + componentGroup.name).append(componentGroup.name + ' ' + componentGroup.valueInMb + 'mb')
        );

        return $('<li/>')
            .append(header)
            .append(services)
            .attr('name', componentGroup.name)
            .addClass('group_container');
    },
    drawService: function (service) {
        return $('<li/>')
            .append($('<header/>').append(service.name))
            .append($('<span/>').text((service.valueInMb == 0 ? '' : service.valueInMb + 'mb')).addClass('mb_usage'))
            .attr('name', service.name)
            .attr('title', service.description)
            .addClass('service_container');
    }
};