require([
	'backbone',
	'application',
	'routers/pageController',
	'regionManager'
	
],
function ( Backbone, App, PageController ) {
    'use strict';
    var options = {
        pageController: PageController
    };
	App.start(options);
});
