define(['backbone', 'hbs!tmpl/header'], function(Backbone, HeaderTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: HeaderTmpl,
        className: 'transp',
        onShow:function () {
			console.log($('ul.nav'));
			$('ul.nav').tab();
        }
    });
});