define([
	'backbone'
],
function( Backbone ) {
    'use strict';

	/* Return a model class definition */
	return Backbone.Model.extend({
		initialize: function() {
			console.log('initialize a Gateway model');
		},
		parse: function(response) {
            if (response){
            	response.locale = response.locale.substring(1,3);
            }
            return response;
        }
    });
});
