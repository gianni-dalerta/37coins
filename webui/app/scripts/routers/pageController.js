define(['backbone',
    'communicator',
    'models/loginModel',
    'views/indexView',
    'views/loginView',
    'views/gatewayView',
    'views/faqView',
    'views/contactView',
    'views/verifyView',
    'views/validateView',
    'routeFilter'
    ], function(Backbone, Communicator, LoginModel, IndexView, LoginView, GatewayView, FaqView, ContactView, VerifyView, ValidateView) {
    'use strict';

    var Controller = {};

    // private module/app router  capture route and call start method of our controller
    Controller.Router = Backbone.Marionette.AppRouter.extend({
        appRoutes: {
            '': 'showIndex',
            'gateways': 'showGateway',
            'faq': 'showFaq',
            'contact': 'showContact'
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
            var view;
            var model = this.options.controller.loginStatus;
            if (model.get('roles')){
                Communicator.mediator.trigger('app:verify', next);
            }else{
                view = new LoginView({model:model,next:next});
                Communicator.mediator.trigger('app:show', view);
            }
        }
    });

    Communicator.mediator.on('app:verify', function(next) {
        console.dir(Controller.loginStatus);
        var view;
        if (!Controller.loginStatus.get('mobile')){
            view = new VerifyView({model:Controller.loginStatus,next:next});
            Communicator.mediator.trigger('app:show', view);
        }else if (!Controller.loginStatus.get('fee')){
            view = new ValidateView({model:Controller.loginStatus,next:next});
            Communicator.mediator.trigger('app:show', view);
        }else {
            next();
        }
    });

    Controller.showIndex = function() {
        var view = new IndexView({model:new Backbone.Model({resPath:window.opt.resPath})});
        Communicator.mediator.trigger('app:show', view);
    };

    Controller.showGateway = function() {
        var view = new GatewayView({model:this.loginStatus});
        Communicator.mediator.trigger('app:show', view);
    };

    Controller.showFaq = function() {
        var view = new FaqView();
        Communicator.mediator.trigger('app:show', view);
    };

    Controller.showContact = function() {
        var view = new ContactView();
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