require.config({

    baseUrl: "/scripts",

    /* starting point for application */
    deps: ['backbone.marionette', 'bootstrap', 'main'],


    shim: {
        backbone: {
            deps: [
                'underscore',
                'jquery'
            ],
            exports: 'Backbone'
        },
        bootstrap: {
            deps: ['jquery'],
            exports: 'jquery'
        },
        recaptcha: { exports: 'Recaptcha' }
    },

    paths: {
        jquery: '../bower_components/jquery/jquery',
        backbone: '../bower_components/backbone-amd/backbone',
        underscore: '../bower_components/underscore-amd/underscore',

        /* alias all marionette libs */
        'backbone.marionette': '../bower_components/backbone.marionette/lib/core/amd/backbone.marionette',
        'backbone.wreqr': '../bower_components/backbone.wreqr/lib/amd/backbone.wreqr',
        'backbone.babysitter': '../bower_components/backbone.babysitter/lib/amd/backbone.babysitter',
        'backbone.eventbinder': '../bower_components/backbone.eventbinder/lib/amd/backbone.eventbinder', // amd version
        routeFilter: '../bower_components/backbone-async-route-filter/backbone-route-filter-amd',

        /* alias the bootstrap js lib */
        bootstrap: '../bower_components/sass-bootstrap/dist/js/bootstrap',

        /* Alias text.js for template loading and shortcut the templates dir to tmpl */
        text: '../bower_components/requirejs-text/text',
        tmpl: '../templates',

        /* handlebars from the require handlerbars plugin below */
        handlebars: '../bower_components/require-handlebars-plugin/Handlebars',

        /* require handlebars plugin - Alex Sexton */
        i18nprecompile: '../bower_components/require-handlebars-plugin/hbs/i18nprecompile',
        json2: '../bower_components/require-handlebars-plugin/hbs/json2',
        hbs: '../bower_components/require-handlebars-plugin/hbs',
        basicauth: '../bower_components/backbone.basicauth/backbone.basicauth',
        recaptcha: 'vendor/recaptcha_ajax'
    },

    hbs: {
        disableI18n: true
    }
});
