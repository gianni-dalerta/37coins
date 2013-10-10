define(['marionette','models/loginModel','views/templateView','views/loginView','views/gatewayView','vent', 'templates', 'routeFilter'], function(Marionette, LoginModel, TemplateView, LoginView, GatewayView, vent, templates) {
    'use strict';

    var Controller = {};

    // private module/app router  capture route and call start method of our controller
    Controller.Router = Marionette.AppRouter.extend({
        appRoutes: {
            '': 'showIndex',
            'gateways': 'showGateway'
        },
        before:{
            'gateways': 'showLogin',
            '*any': function(fragment, args, next){
                console.log('before');
                next();
            }
        },
        showLogin: function(fragment, args, next) {
            if (!this.options.controller.loginStatus){
                this.options.controller.loginStatus = new LoginModel();
            }
            if (this.options.controller.loginStatus.get('roles')){
                next();
            }else{
                var view = new LoginView({model:this.options.controller.loginStatus,next:next});
                vent.trigger('app:show', view);
            }
        }
    });

    Controller.showIndex = function() {
        this.showTemplate('index');
    };

    Controller.showGateway = function() {
        var view = new GatewayView({model:this.loginStatus});
        vent.trigger('app:show', view);
    };

    Controller.showTemplate = function(tmplt) {
        if (!templates[tmplt]){
            tmplt = 'index';
        }
        var view = new TemplateView({tmplt:templates[tmplt]});
        vent.trigger('app:show', view);
    };

    Controller.showLogin = function(fragment, args, next) {
        if (!this.loginStatus){
            this.loginStatus = new LoginModel();
        }
        var view = new LoginView({model:this.loginStatus,next:next});
        vent.trigger('app:show', view);
    };

    return Controller;
});