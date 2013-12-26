define([
	'backbone',
	'communicator',
	'hbs!tmpl/gatewayPreView_tmpl'
],
function(Backbone, Communicator, GatewayTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: GatewayTmpl,
        tagName: 'tr',
        initialize: function() {
            
        }
    });
});