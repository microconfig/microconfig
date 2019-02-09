LOADING_ICON = {
    init: function () {
        $(document).on({
            ajaxStart: function () {
                LOADING_ICON.show();
            },
            ajaxStop: function () {
                LOADING_ICON.hide();
            }
        });
    },
    hide: function () {
        $("#loading_icon").hide();
    },
    show: function () {
        $("#loading_icon").show();
    }
};

$.urlParam = function (name) {
    var results = new RegExp('[\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] : '';
};

$(document).ready(LOADING_ICON.init);