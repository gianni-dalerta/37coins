require([
	'backbone',
	'application',
	'routers/pageController',
	'basicauth',
	'regionManager'
	
],
function ( Backbone, App, PageController ) {
    'use strict';

    var options = {
        pageController: PageController
    };
	App.start(options);
});
