define(['jquery', 'underscoreM', 'backbone'], function( $, _, Backbone) {
    'use strict';

    // private
    var LoginStatus = Backbone.Model.extend({
        url: window.opt.basePath+'/api/gateway',
        defaults: {
            id: sessionStorage.getItem('id'),
            locale: window.opt.lng,
            roles: (sessionStorage.getItem('roles'))?[sessionStorage.getItem('roles')]:null
        },

        initialize: function(){
            this.credentials = sessionStorage.getItem('credentials');
        },

        parse: function(response) {
            if (response){
                sessionStorage.setItem('id',response.id);
                sessionStorage.setItem('roles',response.roles);
            }
            return response;
        }
    });
    return LoginStatus;

});