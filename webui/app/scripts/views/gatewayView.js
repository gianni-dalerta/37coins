define([
    'backbone',
    'hbs!tmpl/configView_tmpl'
],
function(Backbone, GatewayTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: GatewayTmpl,
        className: 'gwLayout'
    });
});