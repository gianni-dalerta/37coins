define([
	'backbone',
	'communicator',
	'views/headerView',
	'views/footerView'
],

function( Backbone, Communicator, HeaderView, FooterView ) {
    'use strict';

	var App = new Backbone.Marionette.Application();

    // these regions correspond to #ID's in the index.html 
    App.addRegions({
        header: '#header',
        content: '#content',
        footer: '#footer'
    });

    // marionette app events...
    App.on('initialize:after', function() {
        if (Backbone.history){
            Backbone.history.start();
        }
    });

	Communicator.mediator.on('app:show', function(appView) {
        App.content.show(appView);
    });

	/* Add initializers here */
	App.addInitializer( function (options) {


        App.header.show(new HeaderView());
        App.footer.show(new FooterView());
        new options.pageController.Router({
            controller: options.pageController // wire-up the start method
        });
	});

	return App;
});
