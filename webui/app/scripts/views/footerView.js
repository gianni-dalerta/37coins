define(['backbone', 'hbs!tmpl/footer'], function(Backbone, FooterTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: FooterTmpl,
        className: 'transp'
    });
});