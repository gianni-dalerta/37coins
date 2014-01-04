define(['backbone', 'communicator', 'hbs!tmpl/exampleView_tmpl'], function(Backbone, Communicator, ExampleTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: ExampleTmpl,
        className: 'container'
    });
});