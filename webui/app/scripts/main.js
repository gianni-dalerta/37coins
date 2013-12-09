require([
	'backbone',
	'application',
	'routers/pageController',
	'basicauth',
	'regionManager'
	
],
function ( Backbone, App, PageController ) {
    'use strict';

    //workaround to get dynamic path working
    $('#bg').css('background-image',  'url('+window.opt.resPath+'/images/autumn-bg.jpg)');

    var options = {
        pageController: PageController
    };
	App.start(options);
});
