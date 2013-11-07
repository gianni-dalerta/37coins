define(['backbone', 'hbs!tmpl/login'], function(Backbone, LoginTmpl) {
    'use strict';
    return Backbone.Marionette.ItemView.extend({
        template: LoginTmpl,
        className: 'container',
        initialize: function(opt) {
            this.next = opt.next;
			this.model.on('change:roles', this.onRolesChange, this);
            this.model.on('error', this.onError, this);
        },
        events: {
            'click #loginBtn':'handleLogin',
            'click #regBtn':'handleRegister',
            'click #lostBtn':'handleLost'
        },
        handleLogin: function(e) {
            e.preventDefault();
            var user = $('input:text').val();
            var pw = $('input:password').val();
            if (user && pw){
                var cred = {
                    username: user,
                    password: pw
                };
                sessionStorage.setItem('credentials',cred);
                this.model.credentials = cred;
                this.model.fetch();
            }
        },
        handleRegister: function(e) {
            e.preventDefault();
            window.location.assign('/pwm/public/NewUser');
        },
        handleLost: function(e) {
            e.preventDefault();
            window.location.assign('/pwm/public/ForgottenPassword');
        },
        onRolesChange: function(){
            this.next();
        },
        onError: function(){
            $('div.alert').show();
        },
        onShow:function () {
            $('.alert').alert();
            $('div.alert').hide();
        }
    });
});