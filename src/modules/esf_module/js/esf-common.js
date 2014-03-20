(function ($) {
    var Esf = Esf || {};

    Drupal.behaviors.esf_common = {
        attach: function (context) {
            $('article a').each(function (index, element) {
                var protocol = Drupal.settings.esf.protocol;
                var re = new RegExp("(" + protocol + ":\/\/).*", "g");
                if (element.href.match(re)) {
                    var url = Drupal.settings.basePath + '?q=aspi'; // /esfDrupal.settings.esf.url + ':' + Drupal.settings.esf.port;
                    this.href = url;
                }
                //console.log(element.href);
            });
        }
    };

})(jQuery);