define(['backbone', 'communicator', 'hbs!tmpl/logoutView_tmpl'], function(Backbone, Communicator, LogoutTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: LogoutTmpl,
        className: 'container',
        initialize: function() {
            
        }
    });
});