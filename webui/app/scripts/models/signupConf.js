define([
	'backbone'
],
function( Backbone ) {
    'use strict';

	/* Return a model class definition */
	return Backbone.Model.extend({
		url: window.opt.basePath+'/account/create',
		initialize: function() {
			console.log('initialize a Account Request');
		},


    });
});
