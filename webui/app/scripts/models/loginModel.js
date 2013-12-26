define(['backbone','communicator'], function(Backbone, Communicator) {
    'use strict';

    // private
    var LoginStatus = Backbone.Model.extend({
        url: window.opt.basePath+'/api/gateway',
        defaults: {
            id: (!sessionStorage.getItem('id')||sessionStorage.getItem('id')==='undefined')?undefined:sessionStorage.getItem('id'),
            locale: window.opt.lng,
            basePath: window.opt.basePath,
            srvcPath: window.opt.srvcPath,
            cn: (!sessionStorage.getItem('cn')||sessionStorage.getItem('cn')==='undefined')?undefined:sessionStorage.getItem('cn'),
            roles: (!sessionStorage.getItem('roles')||sessionStorage.getItem('roles')==='undefined')?undefined:[sessionStorage.getItem('roles')],
            mobile: (!sessionStorage.getItem('mobile')||sessionStorage.getItem('mobile')==='mobile')?undefined:sessionStorage.getItem('mobile'),
            fee: (!sessionStorage.getItem('fee')||sessionStorage.getItem('fee')==='undefined')?undefined:sessionStorage.getItem('fee'),
            envayaToken: (!sessionStorage.getItem('envayaToken')||sessionStorage.getItem('envayaToken')==='undefined')?undefined:sessionStorage.getItem('envayaToken')
        },

        initialize: function(){
            var cred = sessionStorage.getItem('credentials');
            this.credentials = $.parseJSON(cred);
            this.on('change', function (model){
                sessionStorage.setItem('fee',model.get('fee'));
                sessionStorage.setItem('credentials',JSON.stringify(model.credentials));
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