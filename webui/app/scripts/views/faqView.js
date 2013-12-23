define(['backbone', 'hbs!tmpl/faq'], function(Backbone, FaqTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: FaqTmpl,
        className: 'static',
        onShow:function () {
			this.$('.collapse').collapse({
			    parent: '#accordion',
			    toggle: true
			});
        }
    });
});