define([
	'backbone',
	'hbs!tmpl/signupView_tmpl',
	'hbs!tmpl/signupCompletedView_tmpl',
	'recaptcha',
	'jqueryValidation'
],
function( Backbone, SignupTmpl, SignupCompleteTmpl, Recaptcha) {
    'use strict';

	/* Return a ItemView class definition */
	return Backbone.Marionette.ItemView.extend({

		initialize: function() {
			this.model.on('sync', this.onSuccess, this);
			this.model.on('error', this.onError, this);
		},

		onError: function(model, response){
			if (response.status===400){
				location.reload();
			}
			this.$('.alert').css('display','');
            this.$('.alert').addClass('in');
            this.$('button.btn-primary').button('reset');
		},

		onSuccess: function(){
			this.fetched = true;
			this.render();
		},

		getTemplate: function(){
		    if (this.fetched){
		        return SignupCompleteTmpl;
		    } else {
		        return SignupTmpl;
		    }
		},

        handleRegister: function() {
            this.$('button.btn-primary').button('loading');
            var email = this.$('input[name="email"]').val();
            var pw1 = this.$('input[name="password1"]').val();
            this.model.set('email',email);
			this.model.set('password',pw1);
			this.model.save();
        },

        onShow: function(){
			if (!this.fetched){
				this.$('.alert').css('display', 'none');
				var jForm = this.$('form');
				var self = this;
				jForm.validate({
			        rules: {
			            email: {
							required: true,
							email: true
			            },
			            password1: {
			                minlength: 6,
			                maxlength: 15,
			                required: true
			            },
			            password2: {
			                equalTo: 'input[name="password1"]'
			            }
			        },
			        highlight: function(element) {
			            $(element).closest('.form-group').addClass('has-error');
			        },
			        unhighlight: function(element) {
			            $(element).closest('.form-group').removeClass('has-error');
			        },
			        errorElement: 'span',
			        errorClass: 'help-block',
			        submitHandler: function() {
						self.handleRegister();
			        },
			        errorPlacement: function(error, element) {
			            error.insertAfter(element);
			        }
			    });
			}
        }

	});

});
