define([
	'backbone',
	'hbs!tmpl/signupView_tmpl',
	'recaptcha'
],
function( Backbone, SignupTmpl, Recaptcha) {
    'use strict';

	/* Return a ItemView class definition */
	return Backbone.Marionette.ItemView.extend({

		initialize: function() {
			this.model.on('sync', this.onSuccess, this);
			this.model.on('error', this.onError, this);
		},

		onError: function(){
			this.$('div.alert').show();
            this.$('button.btn-primary').button('reset');
		},

		onSuccess: function(){
			this.$('form.form-signin').hide();
			this.$('div.after').show();
			this.$('.alert').show();
		},
		
		template: SignupTmpl,


		/* ui selector cache */
		ui: {},

		/* Ui events hash */
		events: {
            'click button.btn-primary':'handleRegister',
            'blur input[name="email"]' : 'checkEmail',
            'blur input[name="password1"]'    : 'checkPassword1',
            'blur input[name="password2"]'    : 'checkPassword2'
        },

        checkEmail: function(e){
			var user = this.$('input[name="email"]').val();
			console.log(user);
        },

        checkPassword1: function(e){
			var pw1 = this.$('input[name="password1"]').val();
			console.log(pw1);
			this.$('button.btn-primary').button('reset');
        },

        checkPassword2: function(e){
			var pw2 = this.$('input[name="password2"]').val();
			console.log(pw2);
        },

        handleRegister: function(e) {
			e.preventDefault();
            $(e.target).button('loading');
            var email = this.$('input[name="email"]').val();
            var pw1 = this.$('input[name="password1"]').val();
            var pw2 = this.$('input[name="password2"]').val();
            if (pw1 === pw2){
	            this.model.set('email',email);
				this.model.set('password',pw1);
				this.model.save();
	        }else{
				this.$('div.alert').show();
	        }
        },

        onShow: function(){
			this.$('.alert').alert();
            this.$('div.alert').hide();
            this.$('div.after').hide();
            this.$('button.btn-primary').prop('disabled',true);
            var self = this;
        }

	});

});
