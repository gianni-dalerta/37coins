define(['backbone','communicator'], function(Backbone, Communicator) {
    'use strict';

    // private
    var LoginStatus = Backbone.Model.extend({
        url: window.opt.basePath+'/api/gateway',
        defaults: {
            id: sessionStorage.getItem('id'),
            locale: window.opt.lng,
            basePath: window.opt.basePath,
            srvcPath: window.opt.srvcPath,
            cn: (sessionStorage.getItem('cn'))?sessionStorage.getItem('cn'):undefined,
            roles: (sessionStorage.getItem('roles'))?[sessionStorage.getItem('roles')]:undefined,
            mobile: (sessionStorage.getItem('mobile'))?sessionStorage.getItem('mobile'):undefined,
            fee: (sessionStorage.getItem('fee'))?sessionStorage.getItem('fee'):undefined,
            envayaToken: (sessionStorage.getItem('envayaToken'))?sessionStorage.getItem('envayaToken'):undefined
        },

        initialize: function(){
            this.credentials = sessionStorage.getItem('credentials');
            this.on('change', function (model){
                sessionStorage.setItem('fee',model.get('fee'));
            });
            var vent = Communicator.mediator;
            var self = this;
            vent.on('app:logout', function(){
                sessionStorage.clear();
                self.credentials = null;
                self.clear();
            });
        },

        parse: function(response) {
            if (response){
                if (response.id){
                    response.cn = new RegExp('cn=([^,]+),').exec(response.id)[1];
                }
                sessionStorage.setItem('id',response.id);
                sessionStorage.setItem('roles',response.roles);
                sessionStorage.setItem('mobile',response.mobile);
                sessionStorage.setItem('fee',response.fee);
                sessionStorage.setItem('envayaToken',response.envayaToken);
                sessionStorage.setItem('cn',response.cn);
            }
            return response;
        }
    });
    return LoginStatus;

});