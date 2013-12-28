define([
	'backbone',
	'hbs!tmpl/resetConfErrorView_tmpl',
	'hbs!tmpl/resetConfSuccessView_tmpl',
	'hbs!tmpl/resetConfView_tmpl'
],
function(Backbone, ResetErrorTmpl, ResetSuccessTmpl, ResetConfTmpl) {
    'use strict';

	/* Return a ItemView class definition */
	return Backbone.Marionette.ItemView.extend({

		initialize: function() {
			console.log('initialize a Reset Conf ItemView');
			this.model.on('error', this.onError, this);
			this.model.on('sync', this.onSuccess, this);
		},

		template: ResetConfTmpl,

		getTemplate: function(){
		    if (this.fetched){
		        return ResetSuccessTmpl;
		    } else if (this.inactive){
		        return ResetErrorTmpl;
		    }else{
				return ResetConfTmpl;
		    }
		},

		onSuccess: function(){
			this.fetched = true;
			this.render();
		},
        
        onError: function(){
            this.inactive = true;
			this.render();
        },
        handleClose: function(e){
            var alert = $(e.target).parent();
            alert.one(window.transEvent(), function(){
                alert.css('display', 'none');
            });
            alert.removeClass('in');
        },

		/* ui selector cache */
		ui: {},

		/* Ui events hash */
		events: {
            'click button.btn-primary':'handleReset',
            'click .close': 'handleClose',
            'blur input[name="password1"]':'checkPassword1'
        },

        checkPassword1: function(){
			this.$('button.btn-primary').button('reset');
        },

        handleReset: function(e){
			e.preventDefault();
			var pw1 = this.$('input[name="password1"]').val();
			var pw2 = this.$('input[name="password2"]').val();
			if (pw1 === pw2){
				$(e.target).button('loading');
				this.model.set('password',pw1);
				this.model.save();
	        }else{
				this.$('.alert').css('display','');
				this.$('.alert').addClass('in');
	        }
        },

		onShow: function() {
			if (!this.fetched && !this.inactive){
				this.$('.alert').css('display', 'none');
				this.$('button.btn-primary').prop('disabled',true);
				var self = this;
	            $.get( window.opt.basePath + '/ticket',
					{ticket: this.model.get('token')},
					function( data ) {
						if (data.value === 'inactive'){
							self.inactive = true;
							self.render();
						}
					}
				);
			}
		}
	});

});
