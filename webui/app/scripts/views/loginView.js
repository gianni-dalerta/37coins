define(['underscoreM', 'marionette', 'templates'], function(_, Marionette, templates) {
    'use strict';
    return Marionette.ItemView.extend({
        template: _.template(templates.login),
        className: 'container',
        initialize: function(opt) {
            this.next = opt.next;
			this.model.bind('change:roles', this.onRolesChange, this);
            this.model.bind('error', this.onError, this);
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
            console.log('register');
        },
        handleLost: function(e) {
            e.preventDefault();
            console.log('lost');
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