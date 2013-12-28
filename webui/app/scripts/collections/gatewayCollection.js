define([
	'backbone',
	'models/gatewayModel'
],
function( Backbone, GatewayModel ) {
    'use strict';

	/* Return a collection class definition */
	return Backbone.Collection.extend({
		url: window.opt.basePath+'/data/gateways',
		initialize: function() {
			console.log('initialize a Gateway collection');
		},

		model: GatewayModel
		
	});
});
