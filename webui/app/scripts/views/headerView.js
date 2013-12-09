define(['backbone', 'hbs!tmpl/header'], function(Backbone, HeaderTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: HeaderTmpl,
        className: 'transp',
        onShow:function () {
			this.$('ul.nav').tab();
			this.$('.collapse').collapse();
        }
    });
});