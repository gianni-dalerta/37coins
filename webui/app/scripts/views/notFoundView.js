define(['backbone', 'communicator', 'hbs!tmpl/notFoundView_tmpl'], function(Backbone, Communicator, NotFoundTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: NotFoundTmpl,
        className: 'container',
        initialize: function() {
            
        }
    });
});