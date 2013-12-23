define(['backbone', 'communicator', 'hbs!tmpl/login'], function(Backbone, Communicator, LoginTmpl) {
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
            'change input': 'changeInput'
        },
        changeInput: function(e) {
            this.$('#loginBtn').button('reset');
        },
        handleLogin: function(e) {
            e.preventDefault();
            $(e.target).button('loading');
            var user = $('input:text').val();
            var pw = $('input:password').val();
            if (user && pw){
                var cred = {
                    username: user,
                    password: pw
                };
                this.model.clear({silent:true});
                this.model.set({
                    locale: window.opt.lng,
                    basePath: window.opt.basePath,
                    srvcPath: window.opt.srvcPath
                });
                sessionStorage.setItem('credentials',cred);
                this.model.credentials = cred;
                this.model.fetch();
            }
        },
        onRolesChange: function(){
            Communicator.mediator.trigger('app:verify', this.next);
            //this.next();
        },
        onError: function(){
            this.$('div.alert').show();
            this.$('#loginBtn').button('reset');
        },
        onShow:function () {
            this.$('.alert').alert();
            this.$('div.alert').hide();
            this.$('#loginBtn').prop('disabled',true);
        }
    });
});