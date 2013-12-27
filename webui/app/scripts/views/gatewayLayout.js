define([
	'backbone',
	'hbs!tmpl/gatewayLayout_tmpl'
],
function( Backbone, GatewayLayout) {
    'use strict';
	return Backbone.Marionette.Layout.extend({
	    template: GatewayLayout,

	    regions: {
	        bal: '#balView',
	        fee: '#feeView',
	        conf: '#confView'
	    }
	});

});