define(['backbone', 'hbs!tmpl/contact'], function(Backbone, ContactTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: ContactTmpl,
        className: 'static'
    });
});