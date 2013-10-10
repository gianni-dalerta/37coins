require.config({
    paths: {
        jquery: '../components/jquery/jquery',
        underscore: '../components/underscore-amd/underscore', // amd version
        underscoreM: 'libs/underscore/underscore-mustache',  // templating supporting mustache style {{ ... }}
        backbone: '../components/backbone-amd/backbone', // amd version
        'backbone.wreqr': '../components/backbone.wreqr/lib/amd/backbone.wreqr', // amd version
        'backbone.eventbinder': '../components/backbone.eventbinder/lib/amd/backbone.eventbinder', // amd version
        'backbone.babysitter': '../components/backbone.babysitter/lib/amd/backbone.babysitter', // amd version
        marionette: '../components/marionette/lib/core/amd/backbone.marionette',  // amd version
        basicauth: '../components/backbone.basicauth/backbone.basicauth',
        routeFilter: '../components/backbone-async-route-filter/backbone-route-filter-amd',
        text: '../components/requirejs-text/text',
        handlebars: '../components/handlebars/handlebars',
        i18next: '../components/i18next/release/i18next.amd-1.6.3.min',
        bootstrap: '../components/sass-bootstrap/dist/js/bootstrap',
    },
    shim: {
        bootstrap: {
            deps: ['jquery']
        }
    }
});

require(['backbone', 'app', 'controllers/pageController','i18next','basicauth','bootstrap'], function(Backbone, App, PageController, I18next) {
    'use strict';
    var options = {
        pageController: PageController
    };
    I18next.init({
        lng: window.opt.lng,
        resGetPath: window.opt.i18nResPath
    },function(t) {
        options.t = t;
        App.start(options);
    });
});
