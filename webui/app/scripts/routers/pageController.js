define(['backbone', 
    'communicator',
    'models/loginModel',
    'views/indexView',
    'views/loginView',
    'views/gatewayView',
    'routeFilter'
    ], function(Backbone, Communicator, LoginModel, IndexView, LoginView, GatewayView) {
    'use strict';

    var Controller = {};

    // private module/app router  capture route and call start method of our controller
    Controller.Router = Backbone.Marionette.AppRouter.extend({
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
                Communicator.mediator.trigger('app:show', view);
            }
        }
    });

    Controller.showIndex = function() {
        var view = new IndexView();
        Communicator.mediator.trigger('app:show', view);
    };

    Controller.showGateway = function() {
        var view = new GatewayView({model:this.loginStatus});
        Communicator.mediator.trigger('app:show', view);
    };

    Controller.showLogin = function(fragment, args, next) {
        if (!this.loginStatus){
            this.loginStatus = new LoginModel();
        }
        var view = new LoginView({model:this.loginStatus,next:next});
        Communicator.mediator.trigger('app:show', view);
    };

    return Controller;
});